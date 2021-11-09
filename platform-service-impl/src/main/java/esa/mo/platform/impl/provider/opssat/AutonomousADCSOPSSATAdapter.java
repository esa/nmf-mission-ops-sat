/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under European Space Agency Public License (ESA-PL) Weak Copyleft – v2.4
 * You may not use this file except in compliance with the License.
 *
 * Except as expressly set forth in this License, the Software is provided to
 * You on an "as is" basis and without warranties of any kind, including without
 * limitation merchantability, fitness for a particular purpose, absence of
 * defects or errors, accuracy or non-infringement of intellectual property rights.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * ----------------------------------------------------------------------------
 */
package esa.mo.platform.impl.provider.opssat;

import at.tugraz.ihf.opssat.iadcs.*;
import esa.mo.platform.impl.provider.gen.AutonomousADCSAdapterInterface;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import esa.mo.platform.impl.provider.gen.PowerControlAdapterInterface;
import org.ccsds.moims.mo.mal.structures.FloatList;
import org.ccsds.moims.mo.platform.autonomousadcs.structures.*;
import org.ccsds.moims.mo.platform.powercontrol.structures.DeviceType;
import org.ccsds.moims.mo.platform.structures.VectorF3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;

/**
 *
 * @author Cesar Coelho
 */
public class AutonomousADCSOPSSATAdapter implements AutonomousADCSAdapterInterface
{

  private static final String TLE_PATH = "/etc/tle";
  private static final Logger LOGGER = Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName());
  private static final float ANGLE_TOL_RAD = 0.0872665f;
  private static final float ANGLE_TOL_PERCENT = 20.0f;
  private static final float ANGLE_VEL_TOL_RADPS = 0.00872665f;
  private static final float TARGET_THRESHOLD_RAD = 0.261799f;

  private static final float MAX_REACTION_WHEEL_SPEED = 1047.197551f;
  private static final float MAX_REACTION_WHEEL_TORQUE = 0.0001f;
  private static final int IADCS_WATCH_PERIOD_MS = 30 * 1000;

  private AttitudeMode activeAttitudeMode;
  private SEPP_IADCS_API adcsApi;
  private final boolean apiLoaded;
  private boolean unitInitialized = false;

  // Additional parameters which need to be used for attitude mode changes.
  private SEPP_IADCS_API_VECTOR3_XYZ_FLOAT losVector; // Satellite body vector pointing at the target (e.g. nadir or fixed target)
  private SEPP_IADCS_API_VECTOR3_XYZ_FLOAT flightVector; // Satellite body vector pointing into the flight direction
  private SEPP_IADCS_API_VECTOR3_XYZ_FLOAT sunPointingVector; // Satellite body vector pointing into the sun
  private SEPP_IADCS_API_TARGET_POINTING_TOLERANCE_PARAMETERS tolerance;
  private PowerControlAdapterInterface pcAdapter;

  private PositionHolder holder;
  private Thread watcherThread;

  public AutonomousADCSOPSSATAdapter(PowerControlAdapterInterface pcAdapter)
  {
    this.pcAdapter = pcAdapter;
    LOGGER.log(Level.INFO, "Initialisation");
    try {
      System.loadLibrary("iadcs_api_jni");
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, "iADCS library could not be loaded!", ex);
      apiLoaded = false;
      return;
    }
    // Mark it as available even if it is offline - might come up later
    apiLoaded = true;
    watcherThread = new Thread(new IADCSWatcher(), "iADCS Watcher");
    watcherThread.start();
  }

  /**
   * Inits iADCS API and puts it into default mode
   */
  private boolean initIADCS()
  {
    synchronized(this) {
      try {
      adcsApi = new SEPP_IADCS_API();
      } catch (final Exception ex) {
        LOGGER.log(Level.SEVERE, "iADCS API could not get initialized!", ex);
        return false;
      }
      tolerance = new SEPP_IADCS_API_TARGET_POINTING_TOLERANCE_PARAMETERS();
      tolerance.setPREALIGNMENT_ANGLE_TOLERANCE_RAD(ANGLE_TOL_RAD);
      tolerance.setPREALIGNMENT_ANGLE_TOLERANCE_PERCENT(ANGLE_TOL_PERCENT);
      tolerance.setPREALIGNMENT_ANGULAR_VELOCITY_TOLERANCE_RADPS(ANGLE_VEL_TOL_RADPS);
      tolerance.setPREALIGNMENT_TARGET_THRESHOLD_RAD(TARGET_THRESHOLD_RAD); // See section 6.2.2.4 in ICD

      losVector = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
      losVector.setX(0);
      losVector.setY(0);
      losVector.setZ(-1);
  
      flightVector = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
      flightVector.setX(0);
      flightVector.setY(-1);
      flightVector.setZ(0);

      sunPointingVector = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
      sunPointingVector.setX(-1);
      sunPointingVector.setY(0);
      sunPointingVector.setZ(0);
      try {
        // Try running a short command as a ping
        adcsApi.Get_Epoch_Time();
      } catch (final Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to get Epoch Time", e);
        // Assume the device is offline / failed
        return false;
      }
      try {
        dumpPowerTelemetry();
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Failed to dump iADCS TM", e);
        return false;
      }

      try {
        unset(); // Transition to measurement mode
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Failed to switch to measurement mode upon init", e);
        return false;
      }
      return true;
    }
  }

  /**
   * Monitors the iADCS offline->online transitions and configures it into default mode
   */
  private class IADCSWatcher implements Runnable
  {
    public IADCSWatcher()
    {
    }

    @Override
    public void run()
    {
      try {
        while (true) {
          Thread.sleep(IADCS_WATCH_PERIOD_MS);
          boolean isAvailable = isUnitAvailableInternal();
          if (isAvailable && !unitInitialized) {
            LOGGER.log(Level.INFO, "iADCS came online - attempting initialisation");
            if (initIADCS()) {
              LOGGER.log(Level.INFO, "iADCS initialised - marking available");
              unitInitialized = true;
            } else {
              LOGGER.log(Level.WARNING, "iADCS init failed");
            }
          } else if (!isAvailable && unitInitialized) {
            LOGGER.log(Level.INFO, "iADCS gone offline - marking unavailable");
            adcsApi = null;
            unitInitialized = false;
          }
        }
      } catch (InterruptedException ex) {
        return;
      }
    }
  }
  /**
   * class for controlling the vector pointing mode
   */
  private class PositionHolder implements Runnable
  {

    private final Vector3D targetVec;

    private final float margin;
    private boolean isHoldingPosition;
    private boolean isX = true;
    private boolean isY;
    private boolean isZ;
    private boolean isFinished;

    public PositionHolder(final Vector3D targetVec, final float margin)
    {
      this.targetVec = targetVec;
      this.margin = margin;
      isHoldingPosition = true;
      LOGGER.log(Level.INFO, "OPSSAT vector pointing initiated");
    }

    /**
     * stops the vector pointing mode
     */
    public void stop()
    {
      isHoldingPosition = false;
      while (!isFinished) {
        try {
          Thread.sleep(1000);
        } catch (final InterruptedException ex) {
          Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }

    @Override
    public void run()
    {
      do {
        try {
          Thread.sleep(1000);
          synchronized(this) {
            // get current attitude telemetry
            final SEPP_IADCS_API_QUATERNION_FLOAT telemetry =
                adcsApi.Get_Attitude_Telemetry().getATTITUDE_QUATERNION_BF();
            final Rotation currentRotation = new Rotation(telemetry.getQ(), telemetry.getQ_I(),
                telemetry.getQ_J(), telemetry.getQ_K(), true);

            /* calculate rotation angles
              by creating a rotation from the camera vector (in spacecraft frame)
              to the target vector
              (which has to be transformed into spacecraft frame from ICRF, hence the applyInverse)*/
            final Vector3D diff = new Vector3D(
                new Rotation(new Vector3D(0, 0, -1), currentRotation.applyInverseTo(targetVec))
                    .getAngles(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR));

            // rotate around each axis in spacecraft frame. (first x than y than z)
            if (isX) {
              //rotate diff.getX() around x
              adcsApi.Start_SingleAxis_AngleStep_Controller(
                  SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X,
                  (float) diff.getX());
            } else if (isY) {
              //rotate diff.getY() around y
              adcsApi.Start_SingleAxis_AngleStep_Controller(
                  SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y,
                  (float) diff.getY());
            } else if (isZ) {
              //rotate diff.getZ() around z
              adcsApi.Start_SingleAxis_AngleStep_Controller(
                  SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Z,
                  (float) diff.getZ());
            } else {
              // in case of drift, restart alignment
              if (Math.abs(FastMath.toDegrees(diff.getX())) > this.margin) {

                adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
                    SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X);
                adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
                    SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y);
                adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
                    SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Z);

                isX = true;
              } else {
                // if attitude is good, hold attitude
                adcsApi.Start_SingleAxis_AngularVelocity_Controller(
                    SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X,
                    0);
                adcsApi.Start_SingleAxis_AngularVelocity_Controller(
                    SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y,
                    0);
                adcsApi.Start_SingleAxis_AngularVelocity_Controller(
                    SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Z,
                    0);
              }
            }

            // if x target is reached swap to y axis
            if (isX && Math.abs(FastMath.toDegrees(diff.getX())) <= this.margin) {
              isX = false;
              isY = true;
              adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
                  SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X);
            }

            // if y target is reached swap to z axis
            if (isY && Math.abs(FastMath.toDegrees(diff.getY())) <= this.margin + 0.01) {
              isY = false;
              isZ = true;
              adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
                  SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y);
            }

            // if z target is reached, stop rotation
            if (isZ && Math.abs(FastMath.toDegrees(diff.getZ())) <= this.margin + 0.01) {
              isZ = false;
              adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
                  SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Z);
            }
          }

        } catch (final InterruptedException ex) {
          Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.SEVERE, null,
              ex);
        }

      } while (isHoldingPosition); // do this while the vectorpointing mode is active

      synchronized(this) {
        // cleanup: stop every mode that is possibly running
        adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
            SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X);
        adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
            SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y);
        adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
            SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Z);

        adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
            SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X);
        adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
            SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y);
        adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
            SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Z);
      }

      isFinished = true;
    }
  }

  private SEPP_IADCS_API_ORBIT_TLE_DATA readTLEFile() throws IOException {
    final File f = new File(TLE_PATH);
    final BufferedReader br = new BufferedReader(new FileReader(f));
    String s;
    final List<String> lines = new ArrayList<>();
    while ((s = br.readLine()) != null) {
      lines.add(s);
    }
    if (lines.size() == 3) {
      lines.remove(0);
    }
    final SEPP_IADCS_API_ORBIT_TLE_DATA tle = new SEPP_IADCS_API_ORBIT_TLE_DATA();
    if (lines.get(0).length() != 69) {
      throw new IOException(
        MessageFormat.format("TLE Line 1 is not 69 characters long ({0}): {1}",
          lines.get(0).length(), lines.get(0)));
    }
    else if (lines.get(1).length() != 69) {
      throw new IOException(
        MessageFormat.format("TLE Line 2 is not 69 characters long ({0}): {1}",
          lines.get(1).length(), lines.get(1)));
    }
    // Convert Java bytes to null terminated strings
    final byte[] l1 = new byte[70];
    final byte[] l2 = new byte[70];
    l1[69] = 0;
    l2[69] = 0;
    System.arraycopy(lines.get(0).getBytes(StandardCharsets.ISO_8859_1), 0, l1, 0, 69);
    LOGGER.log(Level.INFO, "Successfully loaded line 1 into {0} byte string.", l1.length);
    System.arraycopy(lines.get(1).getBytes(StandardCharsets.ISO_8859_1), 0, l2, 0, 69);
    LOGGER.log(Level.INFO, "Successfully loaded line 2 into {0} byte string.", l2.length);

    tle.setTLE_1(l1);
    tle.setTLE_2(l2);
    return tle;
  }

  /**
   * Sets the line of sight vector for the different target pointing modes.
   *
   * @param los Line of sight unit vector in body frame of the satellite.
   * @throws IllegalArgumentException Thrown when the vector does not have length 1.
   */
  public void setLOSVector(final VectorF3D los) throws IllegalArgumentException
  {
    final double x2 = (double) los.getX() * (double) los.getX();
    final double y2 = (double) los.getY() * (double) los.getY();
    final double z2 = (double) los.getZ() * (double) los.getZ();
    if (!isUnity(los)) {
      throw new IllegalArgumentException("The provided line of sight vector needs to have length 1.");
    }
    losVector = convertToAdcsApiVector(los);
  }

  /**
   * Sets the flight vector for different target pointing modes.
   *
   * @param flight Flight vector as unit vector in body frame of the satellite.
   * @throws IllegalArgumentException Thrown when the vector does not have length 1.
   */
  public void setFlightVector(final VectorF3D flight) throws IllegalArgumentException
  {
    if (!isUnity(flight)) {
      throw new IllegalArgumentException("The provided flight vector needs to have length 1.");
    }
    flightVector = convertToAdcsApiVector(flight);
  }

  /**
   * Sets the target vector for active sun pointing mode.
   *
   * @param target Target vector as unit vector in body frame of the satellite.
   * @throws IllegalArgumentException Thrown when the vector is not a unit vector.
   */
  public void setSunPointingVector(final VectorF3D target) throws IllegalArgumentException
  {
    if (!isUnity(target)) {
      throw new IllegalArgumentException("The provided target vector needs to have length 1.");
    }
    sunPointingVector = convertToAdcsApiVector(target);
  }

  /**
   * Checks if a given VectorF3D has the euclidian length 1 (is a unit vector).
   *
   * @param vec The vector to be checked.
   * @return True iff length is 1.
   */
  private boolean isUnity(final VectorF3D vec)
  {
    final double x2 = (double) vec.getX() * (double) vec.getX();
    final double y2 = (double) vec.getY() * (double) vec.getY();
    final double z2 = (double) vec.getZ() * (double) vec.getZ();
    return Math.abs(Math.sqrt(x2 + y2 + z2) - 1.0) < 0.001;
  }

  private void dumpStandardTelemetry()
  {
    SEPP_IADCS_API_STANDARD_TELEMETRY stdTM;
    LOGGER.log(Level.INFO, "Dumping Standard Telemetry...");
    synchronized(this) {
      stdTM = adcsApi.Get_Standard_Telemetry();
    }
    SEPP_IADCS_API_VECTOR3_XYZ_UINT singleAxisStatus = stdTM.getCONTROL_SINGLE_AXIS_STATUS();
    Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO,
        String.format("Standard TM:\n"
            + "SYSTEM_TIME_MSEC = %d\n"
            + "EPOCH_TIME_MSEC = %d\n"
            + "LIVELYHOOD_REGISTER = %d\n"
            + "SYSTEM_STATUS_REGISTER = %d\n"
            + "SYSTEM_SCHEDULER_REGISTER = %d\n"
            + "SYSTEM_ERROR_REGISTER = %d\n"
            + "SENSORS_ERROR_REGISTER = %d\n"
            + "ACTUATORS_ERROR_REGISTER = %d\n"
            + "CONTROL_MAIN_STATUS = %d\n"
            + "CONTROL_MAIN_ERROR = %d\n"
            + "CONTROL_SINGLE_AXIS_STATUS (X,Y,Z) = (%d,%d,%d)\n"
            + "CONTROL_ALL_AXIS_STATUS = %d\n"
            + "SAT_MAIN_REGISTER = %d\n"
            + "SAT_ERROR_REGISTER = %d\n"
            + "SAT_SCHEDULER_REGISTER = %d\n"
            + "NUMBER_OF_RECEIVED_COMMANDS = %d\n"
            + "NUMBER_OF_FAILED_COMMANDS = %d\n",
            stdTM.getSYSTEM_TIME_MSEC(),
            stdTM.getEPOCH_TIME_MSEC(),
            stdTM.getLIVELYHOOD_REGISTER(),
            stdTM.getSYSTEM_STATUS_REGISTER(),
            stdTM.getSYSTEM_SCHEDULER_REGISTER(),
            stdTM.getSYSTEM_ERROR_REGISTER(),
            stdTM.getSENSORS_ERROR_REGISTER(),
            stdTM.getACTUATORS_ERROR_REGISTER(),
            stdTM.getCONTROL_MAIN_STATUS(),
            stdTM.getCONTROL_MAIN_ERROR(),
            singleAxisStatus.getX(),
            singleAxisStatus.getY(),
            singleAxisStatus.getZ(),
            stdTM.getCONTROL_ALL_AXIS_STATUS(),
            stdTM.getSAT_MAIN_REGISTER(),
            stdTM.getSAT_ERROR_REGISTER(),
            stdTM.getSAT_SCHEDULER_REGISTER(),
            stdTM.getNUMBER_OF_RECEIVED_COMMANDS(),
            stdTM.getNUMBER_OF_FAILED_COMMANDS()));
  }

  private void dumpInfoTelemetry()
  {
    SEPP_IADCS_API_INFO_TELEMETRY infoTM;
    LOGGER.log(Level.INFO, "Dumping Info Telemetry...");
    synchronized(this) {
      infoTM = adcsApi.Get_Info_Telemetry();
    }

    SEPP_IADCS_API_SW_VERSION swVer = infoTM.getSW_VERSION();
    SEPP_IADCS_API_COMMIT_ID commitId = infoTM.getSW_COMMIT_ID();
    Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO,
        String.format("Info TM:\n"
            + "FRAME_IDENTIFIER = %s\n"
            + "FRAME_VERSION = %d\n"
            + "SW_VERSION = %d.%d.%d\n"
            + "STARTRACKER_TYPE = %d\n"
            + "STARTRACKER_SERIAL_NUMBER = %d\n"
            + "DEVICE_NAME = %s\n"
            + "DEVICE_SERIAL_NUMBER = %d\n"
            + "BUILD_TIMESTAMP = %s\n"
            + "SW_COMMIT_ID = project %d, library %d\n"
            + "DEBUG_LEVEL = %d\n"
            + "COMPILER_NAME = %s\n"
            + "COMPILER_VERSION = %s\n"
            + "LOW_LEVEL_SW_VERSION = %s\n"
            + "LOW_LEVEL_BUILD_TIMESTAMP = %S\n",
            infoTM.getFRAME_IDENTIFIER(),
            infoTM.getFRAME_VERSION(),
            swVer.getMAJOR(),
            swVer.getMINOR(),
            swVer.getPATCH(),
            infoTM.getSTARTRACKER_TYPE(),
            infoTM.getSTARTRACKER_SERIAL_NUMBER(),
            infoTM.getDEVICE_NAME(),
            infoTM.getDEVICE_SERIAL_NUMBER(),
            infoTM.getBUILD_TIMESTAMP(),
            commitId.getPROJECT(),
            commitId.getLIBRARY(),
            infoTM.getDEBUG_LEVEL(),
            infoTM.getCOMPILER_NAME(),
            infoTM.getCOMPILER_VERSION(),
            infoTM.getLOW_LEVEL_SW_VERSION(),
            infoTM.getLOW_LEVEL_BUILD_TIMESTAMP()));
  }
  private void dumpPowerTelemetry()
  {
    SEPP_IADCS_API_POWER_STATUS_TELEMETRY powerTM;
    LOGGER.log(Level.INFO, "Dumping Power Telemetry...");
    synchronized(this) {
      powerTM = adcsApi.Get_Power_Status_Telemetry();
    }
    LOGGER.log(Level.INFO,
        String.format("Power TM:\n"
            + " MAGNETTORQUER_POWER_CONSUMPTION_W = %.3f\n"
            + " MAGNETTORQUER_SUPPLY_VOLTAGE_V = %.3f\n"
            + " MAGNETTORQUER_CURRENT_CONSUMPTION_A = %.3f\n"
            + " STARTRACKER_POWER_CONSUMPTION_W = %.3f\n"
            + " STARTRACKER_SUPPLY_VOLTAGE_V = %.3f\n"
            + " STARTRACKER_CURRENT_CONSUMPTION_A = %.3f\n"
            + " IADCS_POWER_CONSUMPTION_W = %.3f\n"
            + " IADCS_SUPPLY_VOLTAGE_V = %.3f\n"
            + " IADCS_CURRENT_CONSUMPTION_A = %.3f\n"
            + " REACTIONWHEEL_POWER_CONSUMPTION_W = %.3f\n"
            + " REACTIONWHEEL_SUPPLY_VOLTAGE_V = %.3f\n"
            + " REACTIONWHEEL_CURRENT_CONSUMPTION_A = %.3f\n",
            powerTM.getMAGNETTORQUER_POWER_CONSUMPTION_W(),
            powerTM.getMAGNETTORQUER_SUPPLY_VOLTAGE_V(),
            powerTM.getMAGNETTORQUER_CURRENT_CONSUMPTION_A(),
            powerTM.getSTARTRACKER_POWER_CONSUMPTION_W(),
            powerTM.getSTARTRACKER_SUPPLY_VOLTAGE_V(),
            powerTM.getSTARTRACKER_CURRENT_CONSUMPTION_A(),
            powerTM.getIADCS_POWER_CONSUMPTION_W(),
            powerTM.getIADCS_SUPPLY_VOLTAGE_V(),
            powerTM.getIADCS_CURRENT_CONSUMPTION_A(),
            powerTM.getREACTIONWHEEL_POWER_CONSUMPTION_W(),
            powerTM.getREACTIONWHEEL_SUPPLY_VOLTAGE_V(),
            powerTM.getREACTIONWHEEL_CURRENT_CONSUMPTION_A()));
  }

  static private Quaternion convertAdcsApiQuaternion(final SEPP_IADCS_API_QUATERNION_FLOAT in)
  {
    return new Quaternion(in.getQ(), in.getQ_I(), in.getQ_J(), in.getQ_K());
  }

  static private MagnetorquersState convertAdcsApiMtqState(final long in)
  {
    final int mappedOrdinal;
    switch ((int) in) {
      case 1:
        mappedOrdinal = MagnetorquersState._ACTIVE_INDEX;
        break;
      case 2:
        mappedOrdinal = MagnetorquersState._SUSPEND_INDEX;
        break;
      case 0:
      default:
        mappedOrdinal = MagnetorquersState._INACTIVE_INDEX;
        break;
    }
    return MagnetorquersState.fromOrdinal(mappedOrdinal);
  }

  static private SEPP_IADCS_API_VECTOR3_XYZ_FLOAT convertToAdcsApiVector(final VectorF3D vec)
  {
    final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT sepp = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
    sepp.setX(vec.getX());
    sepp.setY(vec.getY());
    sepp.setZ(vec.getZ());
    return sepp;
  }

  static private VectorF3D convertAdcsApiVector(final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in)
  {
    return new VectorF3D(in.getX(), in.getY(), in.getZ());
  }

  static private VectorF3D convertAdcsApiMagMoment(final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in)
  {
    // Moment is provided in A*m^2
    return new VectorF3D((float) in.getX(), (float) in.getY(), (float) in.getZ());
  }

  static private WheelsSpeed convertAdcsApiWheelSpeed(final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in1,
                                                      final SEPP_IADCS_API_VECTOR3_UVW_FLOAT in2)
  {
    final FloatList list = new FloatList(6);
    list.add(in1.getX());
    list.add(in1.getY());
    list.add(in1.getZ());
    list.add(in2.getU());
    list.add(in2.getV());
    list.add(in2.getW());
    return new WheelsSpeed(list);
  }

  @Override
  public void setDesiredAttitude(final AttitudeMode attitude) throws IOException,
      UnsupportedOperationException
  {
    synchronized(this) {
      if (attitude instanceof AttitudeModeBDot) {
        final AttitudeModeBDot a = (AttitudeModeBDot) attitude;
        final SEPP_IADCS_API_DETUMBLING_MODE_PARAMETERS params =
            new SEPP_IADCS_API_DETUMBLING_MODE_PARAMETERS();
        adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
        params.setSTART_EPOCH_TIME_MSEC(BigInteger.valueOf(System.currentTimeMillis()));
        params.setSTOP_EPOCH_TIME_MSEC(BigInteger.valueOf(Long.MAX_VALUE));
        adcsApi.Start_Operation_Mode_Detumbling(params);
        activeAttitudeMode = a;
      } else if (attitude instanceof AttitudeModeNadirPointing) {
        if (losVector == null || flightVector == null) {
          throw new IOException(
              "LOS vector or flight vector not set. Call setLOSVector and setFlightVector before calling setDesiredAttitudeMode.");
        }
        final AttitudeModeNadirPointing a = (AttitudeModeNadirPointing) attitude;
        final SEPP_IADCS_API_TARGET_POINTING_NADIR_MODE_PARAMETERS params =
            new SEPP_IADCS_API_TARGET_POINTING_NADIR_MODE_PARAMETERS();
        // Set parameters
        params.setLOS_VECTOR_BF(losVector);
        params.setFLIGHT_VECTOR_BF(flightVector);
        params.setUPDATE_INTERVAL_MSEC(BigInteger.valueOf(500));
        params.setDETERMINATION_MODE(
            SEPP_IADCS_API_TARGET_POINTING_ATTITUDE_DETERMINATION_MODES.IADCS_ATTITUDE_DETERMINATION_STARTRACKER_ONLY);

        params.setTOLERANCE_PARAMETERS(tolerance);
        params.setOFFSET_TIME_MSEC(BigInteger.valueOf(0));
        adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
        adcsApi.Init_Orbit_Module(readTLEFile());
        adcsApi.Start_Target_Pointing_Nadir_Mode(params);
        activeAttitudeMode = a;
      } else if (attitude instanceof AttitudeModeSingleSpinning) {
        final AttitudeModeSingleSpinning a = (AttitudeModeSingleSpinning) attitude;
        final VectorF3D target = a.getBodyAxis();
        final SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS vec;
        if (target.equals(new VectorF3D(1.0f, 0f, 0f))) {
          vec = SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X;
        } else if (target.equals(new VectorF3D(0f, 1.0f, 0f))) {
          vec = SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y;
        } else if (target.equals(new VectorF3D(0f, 0f, 1.0f))) {
          vec = SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Z;
        } else {
          throw new UnsupportedOperationException("Only X, Y and Z are valid Single Spinning axis.");
        }
        adcsApi.Start_SingleAxis_AngularVelocity_Controller(vec, a.getAngularVelocity());
        activeAttitudeMode = a;
      } else if (attitude instanceof AttitudeModeSunPointing) {
        if (sunPointingVector == null) {
          throw new IOException(
              "Target vector is not set. Call setSunPointingVector before calling setDesiredAttitudeMode.");
        }
        final AttitudeModeSunPointing sunPointing = (AttitudeModeSunPointing) attitude;
        final SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS params =
            new SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS();
        params.setSTART_EPOCH_TIME_MSEC(BigInteger.valueOf(0));
        params.setSTOP_EPOCH_TIME_MSEC(BigInteger.valueOf(Long.MAX_VALUE));
        params.setTARGET_VECTOR_BF(sunPointingVector);
        adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
        adcsApi.Init_Orbit_Module(readTLEFile());
        adcsApi.Start_Operation_Mode_Sun_Pointing(params);
        activeAttitudeMode = sunPointing;
      } else if (attitude instanceof AttitudeModeTargetTracking) {
        if (losVector == null || flightVector == null) {
          throw new IOException("LOS or flight vector not set.");
        }

        final SEPP_IADCS_API_TARGET_POINTING_FIXED_MODE_PARAMETERS params =
            new SEPP_IADCS_API_TARGET_POINTING_FIXED_MODE_PARAMETERS();

        params.setDETERMINATION_MODE(
            SEPP_IADCS_API_TARGET_POINTING_ATTITUDE_DETERMINATION_MODES.IADCS_ATTITUDE_DETERMINATION_STARTRACKER_ONLY);
        params.setTOLERANCE_PARAMETERS(tolerance);
        params.setFLIGHT_VECTOR_BF(flightVector);
        params.setLOS_VECTOR_BF(losVector);
        params.setUPDATE_INTERVAL_MSEC(BigInteger.valueOf(500));
        final AttitudeModeTargetTracking a = (AttitudeModeTargetTracking) attitude;
        params.setTARGET_LATITUDE_RAD((float)FastMath.toRadians(a.getLatitude()));
        params.setTARGET_LONGITUDE_RAD((float)FastMath.toRadians(a.getLongitude()));
        adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
        adcsApi.Init_Orbit_Module(readTLEFile());
        adcsApi.Start_Target_Pointing_Earth_Fix_Mode(params);
        activeAttitudeMode = a;
      } else if (attitude instanceof AttitudeModeTargetTrackingLinear) {
        if (losVector == null || flightVector == null) {
          throw new IOException("LOS or flight vector not set.");
        }

        final SEPP_IADCS_API_TARGET_POINTING_CONST_VELOCITY_MODE_PARAMETERS params =
            new SEPP_IADCS_API_TARGET_POINTING_CONST_VELOCITY_MODE_PARAMETERS();
        params.setFLIGHT_VECTOR_BF(flightVector);
        params.setLOS_VECTOR_BF(losVector);
        params.setDETERMINATION_MODE(
            SEPP_IADCS_API_TARGET_POINTING_ATTITUDE_DETERMINATION_MODES.IADCS_ATTITUDE_DETERMINATION_STARTRACKER_ONLY);
        params.setOFFSET_TIME_MSEC(BigInteger.valueOf(0));
        final AttitudeModeTargetTrackingLinear a = (AttitudeModeTargetTrackingLinear) attitude;
        params.setSTART_EPOCH_TIME_MSEC(BigInteger.valueOf(a.getStartEpoch()));
        params.setSTOP_EPOCH_TIME_MSEC(BigInteger.valueOf(a.getEndEpoch()));
        params.setSTART_LATITUDE_RAD((float)FastMath.toRadians(a.getLatitudeStart()));
        params.setSTART_LONGITUDE_RAD((float)FastMath.toRadians(a.getLongitudeStart()));
        params.setSTOP_LATITUDE_RAD((float)FastMath.toRadians(a.getLatitudeEnd()));
        params.setSTOP_LONGITUDE_RAD((float)FastMath.toRadians(a.getLongitudeEnd()));
        params.setTOLERANCE_PARAMETERS(tolerance);
        params.setUPDATE_INTERVAL_MSEC(BigInteger.valueOf(500));
        adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
        adcsApi.Init_Orbit_Module(readTLEFile());
        adcsApi.Start_Target_Pointing_Earth_Const_Velocity_Mode(params);
        activeAttitudeMode = a;
      } else if (attitude instanceof AttitudeModeVectorPointing) {

        final AttitudeModeVectorPointing a = (AttitudeModeVectorPointing) attitude;

        holder = new PositionHolder(new Vector3D(a.getTarget().getX(), a.getTarget().getY(),
            a.getTarget().getZ()), a.getMargin());
        final Thread runner = new Thread(holder, "iADCS Vector pointing holder");
        runner.start();
      } else {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    }
  }

  @Override
  public void setAllReactionWheelSpeeds(final float wheelX, final float wheelY, final float wheelZ, final float wheelU,
                                        final float wheelV, final float wheelW)
  {
    synchronized(this) {
      final SEPP_IADCS_API_REACTIONWHEEL_SPEEDS speeds = new SEPP_IADCS_API_REACTIONWHEEL_SPEEDS();

      final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT xyz = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
      xyz.setX(wheelX);
      xyz.setY(wheelY);
      xyz.setZ(wheelZ);
      speeds.setINTERNAL(xyz);

      final SEPP_IADCS_API_VECTOR3_UVW_FLOAT uvw = new SEPP_IADCS_API_VECTOR3_UVW_FLOAT();
      uvw.setU(wheelU);
      uvw.setV(wheelV);
      uvw.setW(wheelW);
      speeds.setEXTERNAL(uvw);

      adcsApi.Set_ReactionWheel_All_Speeds(speeds);
    }
  }

  @Override
  public void setReactionWheelSpeed(final ReactionWheelIdentifier wheel, final float Speed)
  {
    synchronized(this) {
      if (wheel.getOrdinal() == ReactionWheelIdentifier.WHEEL_X.getOrdinal()) {
        adcsApi.Set_ReactionWheel_Speed(SEPP_IADCS_API_REACTIONWHEELS.IADCS_REACTIONWHEEL_X, Speed);
      } else if (wheel.getOrdinal() == ReactionWheelIdentifier.WHEEL_Y.getOrdinal()) {
        adcsApi.Set_ReactionWheel_Speed(SEPP_IADCS_API_REACTIONWHEELS.IADCS_REACTIONWHEEL_Y, Speed);
      } else if (wheel.getOrdinal() == ReactionWheelIdentifier.WHEEL_Z.getOrdinal()) {
        adcsApi.Set_ReactionWheel_Speed(SEPP_IADCS_API_REACTIONWHEELS.IADCS_REACTIONWHEEL_Z, Speed);
      } else if (wheel.getOrdinal() == ReactionWheelIdentifier.WHEEL_U.getOrdinal()) {
        adcsApi.Set_ReactionWheel_Speed(SEPP_IADCS_API_REACTIONWHEELS.IADCS_REACTIONWHEEL_U, Speed);
      } else if (wheel.getOrdinal() == ReactionWheelIdentifier.WHEEL_V.getOrdinal()) {
        adcsApi.Set_ReactionWheel_Speed(SEPP_IADCS_API_REACTIONWHEELS.IADCS_REACTIONWHEEL_V, Speed);
      } else if (wheel.getOrdinal() == ReactionWheelIdentifier.WHEEL_W.getOrdinal()) {
        adcsApi.Set_ReactionWheel_Speed(SEPP_IADCS_API_REACTIONWHEELS.IADCS_REACTIONWHEEL_W, Speed);
      }
    }
  }

  @Override
  public void setAllReactionWheelParameters(final ReactionWheelParameters parameters)
  {
    synchronized(this) {
      final ReactionWheelParameters oldParams = getAllReactionWheelParameters();
      final SEPP_IADCS_API_REACTIONWHEEL_ARRAY_PARAMETERS params =
          new SEPP_IADCS_API_REACTIONWHEEL_ARRAY_PARAMETERS();

      if (parameters.getControlMode() >= 0 && parameters.getControlMode() <= 2) {
        params.setCONTROL_MODE(parameters.getControlMode());
      } else {
        if (parameters.getControlMode() != -1) {
          LOGGER.log(Level.WARNING, "{0} is no valid control mode. control mode has not been changed!",
              parameters.getControlMode());
        }
        params.setCONTROL_MODE(oldParams.getControlMode());
      }

      if (parameters.getMaxSpeed() < MAX_REACTION_WHEEL_SPEED) {
        params.setMAX_SPEED_RADPS(parameters.getMaxSpeed());
      } else {
        if (parameters.getMaxSpeed() < 0) {
          LOGGER.log(Level.WARNING,
              "Negative maximum speed is not allowed! Max speed will not be changed");
          params.setMAX_SPEED_RADPS(oldParams.getMaxSpeed());
        } else {
          LOGGER.log(Level.WARNING,
              "Maximum speed is not allowed to exceed {0}! Max speed will be set to {0}",
              MAX_REACTION_WHEEL_SPEED);
          params.setMAX_SPEED_RADPS(MAX_REACTION_WHEEL_SPEED);
        }
      }

      if (params.getMAX_TORQUE_NM() < MAX_REACTION_WHEEL_TORQUE) {
        params.setMAX_SPEED_RADPS(parameters.getMaxSpeed());
      } else {
        if (parameters.getMaxTorque() < 0) {
          LOGGER.log(Level.WARNING,
              "Negative maximum torque is not allowed! Max torque will not be changed");
          params.setMAX_TORQUE_NM(oldParams.getMaxTorque());
        } else {
          LOGGER.log(Level.WARNING,
              "Maximum torque is not allowed to exceed {0}! Max torque will be set to {0}",
              MAX_REACTION_WHEEL_TORQUE);
          params.setMAX_TORQUE_NM(MAX_REACTION_WHEEL_TORQUE);
        }
      }
      adcsApi.Set_ReactionWheel_All_Parameters(params);
    }
  }

  @Override
  public void setAllMagnetorquersDipoleMoments(final Float dipoleX, final Float dipoleY, final Float dipoleZ)
  {
    synchronized(this) {
      final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT moments = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();

      moments.setX(dipoleX);
      moments.setY(dipoleY);
      moments.setZ(dipoleZ);

      adcsApi.Set_Magnettorquer_All_Dipole_Moments(moments);
    }
  }

  @Override
  public ReactionWheelParameters getAllReactionWheelParameters()
  {
    synchronized(this) {
      final SEPP_IADCS_API_REACTIONWHEEL_ARRAY_PARAMETERS param = adcsApi.Get_ReactionWheel_All_Parameters();
      return new ReactionWheelParameters((int) param.getCONTROL_MODE(), param.getMAX_SPEED_RADPS(),
          param.getMAX_TORQUE_NM(),
          param.getMOMENT_OF_INERTIA_KGM2(), param.getMOTOR_CONSTANT());
    }
  }

  @Override
  public void unset() throws IOException
  {
    synchronized(this) {
      if (activeAttitudeMode instanceof AttitudeModeBDot) {
        adcsApi.Stop_Operation_Mode_Detumbling();
      } else if (activeAttitudeMode instanceof AttitudeModeNadirPointing) {
        adcsApi.Stop_Target_Pointing_Nadir_Mode();
      } else if (activeAttitudeMode instanceof AttitudeModeSingleSpinning) {
        adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
            SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X);
        adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
            SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y);
        adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
            SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Z);
      } else if (activeAttitudeMode instanceof AttitudeModeSunPointing) {
        adcsApi.Stop_Operation_Mode_Sun_Pointing();
      } else if (activeAttitudeMode instanceof AttitudeModeTargetTracking) {
        adcsApi.Stop_Target_Pointing_Earth_Fix_Mode();
      } else if (activeAttitudeMode instanceof AttitudeModeTargetTrackingLinear) {
        adcsApi.Stop_Target_Pointing_Earth_Const_Velocity_Mode();
      } else if (activeAttitudeMode instanceof AttitudeModeVectorPointing) {
        holder.stop();
      }
      activeAttitudeMode = null;
      adcsApi.Set_Operation_Mode_Measurement();
      adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
      adcsApi.Init_Orbit_Module(readTLEFile());
      enableOrbitPropagator();
    }
  }

  private void enableOrbitPropagator()
  {
    synchronized(this) {
      SEPP_IADCS_API_SYSTEM_SCHEDULER_REGISTER reg = adcsApi.Get_System_Scheduler_Register();
      reg.setORBIT_PROPAGATION_ENABLE(true);
      adcsApi.Set_System_Scheduler_Register(reg);
    }
  }

  private boolean isUnitAvailableInternal()
  {
    return apiLoaded && pcAdapter.isDeviceEnabled(DeviceType.ADCS);
  }

  @Override
  public boolean isUnitAvailable()
  {
    return isUnitAvailableInternal() && unitInitialized;
  }

  @Override
  public AttitudeTelemetry getAttitudeTelemetry() throws IOException
  {
    synchronized(this) {
      boolean stateTarget = true;
      stateTarget = adcsApi.Get_Target_Pointing_Operation_Data_Telemetry().getSTATE_TARGET() == (short) 1;
      final SEPP_IADCS_API_ATTITUDE_TELEMETRY attitudeTm = adcsApi.Get_Attitude_Telemetry();
      attitudeTm.getATTITUDE_QUATERNION_BF();
      final Quaternion attitude = convertAdcsApiQuaternion(attitudeTm.getATTITUDE_QUATERNION_BF());
      final VectorF3D angularVel = convertAdcsApiVector(attitudeTm.getANGULAR_VELOCITY_VECTOR_RADPS());
      final VectorF3D magneticField = convertAdcsApiVector(
          attitudeTm.getMEASURED_MAGNETIC_FIELD_VECTOR_BF_T());
      final VectorF3D sunVector = convertAdcsApiVector(attitudeTm.getMEASURED_SUN_VECTOR_BF());
      return new AttitudeTelemetry(attitude, angularVel, sunVector, magneticField, stateTarget);
    }
  }

  @Override
  public ActuatorsTelemetry getActuatorsTelemetry() throws IOException
  {
    synchronized(this) {
      final SEPP_IADCS_API_ACTUATOR_TELEMETRY actuatorTm = adcsApi.Get_Actuator_Telemetry();
      final WheelsSpeed targetSpeed = convertAdcsApiWheelSpeed(
          actuatorTm.getREACTIONWHEEL_TARGET_SPEED_VECTOR_XYZ_RADPS(),
          actuatorTm.getREACTIONWHEEL_TARGET_SPEED_VECTOR_UVW_RADPS());
      final WheelsSpeed currentSpeed = convertAdcsApiWheelSpeed(
          actuatorTm.getREACTIONWHEEL_CURRENT_SPEED_VECTOR_XYZ_RADPS(),
          actuatorTm.getREACTIONWHEEL_CURRENT_SPEED_VECTOR_UVW_RADPS());
      final VectorF3D mtqDipoleMoment = convertAdcsApiMagMoment(
          actuatorTm.getMAGNETORQUERS_TARGET_DIPOLE_MOMENT_VECTOR_AM2());
      final MagnetorquersState mtqState =
          convertAdcsApiMtqState(actuatorTm.getMAGNETORQUERS_CURRENT_STATE());
      return new ActuatorsTelemetry(targetSpeed, currentSpeed, mtqDipoleMoment, mtqState);
    }
  }

  @Override
  public String validateAttitudeDescriptor(final AttitudeMode attitude)
  {
    return ""; //Return no error for now
  }

  @Override
  public AttitudeMode getActiveAttitudeMode()
  {
    return activeAttitudeMode;
  }
}
