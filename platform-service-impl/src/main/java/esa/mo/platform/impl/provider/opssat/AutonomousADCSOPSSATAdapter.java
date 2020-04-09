/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under the European Space Agency Public License, Version 2.0
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.structures.FloatList;
import org.ccsds.moims.mo.platform.autonomousadcs.structures.*;
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

  private static final Logger LOGGER = Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName());
  private static final float ANGLE_TOL_RAD = 0.0872665f;
  private static final float ANGLE_TOL_PERCENT = 20.0f;
  private static final float ANGLE_VEL_TOL_RADPS = 0.00872665f;
  private static final float TARGET_THRESHOLD_RAD = 0.261799f;

  private AttitudeMode activeAttitudeMode;
  private SEPP_IADCS_API adcsApi;
  private boolean initialized = false;

  // Additional parameters which need to be used for attitude mode changes.
  private SEPP_IADCS_API_VECTOR3_XYZ_FLOAT losVector;
  private SEPP_IADCS_API_VECTOR3_XYZ_FLOAT flightVector;
  private SEPP_IADCS_API_VECTOR3_XYZ_FLOAT targetVector; // For sun pointing
  private SEPP_IADCS_API_TARGET_POINTING_TOLERANCE_PARAMETERS tolerance;

  private PositionHolder holder;

  public AutonomousADCSOPSSATAdapter()
  {
    LOGGER.log(Level.INFO, "Initialisation");
    try {
      System.loadLibrary("iadcs_api_jni");
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "iADCS library could not be loaded!", ex);
      initialized = false;
      return;
    }
    adcsApi = new SEPP_IADCS_API();
    activeAttitudeMode = null;
    try {
      // Try running a short command as a ping
      adcsApi.Get_Epoch_Time();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize iADCS", e);
      initialized = false;
      return;
    }
    try {
      dumpHKTelemetry();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to dump iADCS TM", e);
    }
    initialized = true;

    tolerance = new SEPP_IADCS_API_TARGET_POINTING_TOLERANCE_PARAMETERS();
    tolerance.setPREALIGNMENT_ANGLE_TOLERANCE_RAD(ANGLE_TOL_RAD);
    tolerance.setPREALIGNMENT_ANGLE_TOLERANCE_PERCENT(ANGLE_TOL_PERCENT);
    tolerance.setPREALIGNMENT_ANGULAR_VELOCITY_TOLERANCE_RADPS(ANGLE_VEL_TOL_RADPS);
    tolerance.setPREALIGNMENT_TARGET_THRESHOLD_RAD(TARGET_THRESHOLD_RAD); // See section 6.2.2.4 in ICD
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
    private boolean isY = false;
    private boolean isZ = false;
    private boolean isFinshed;

    public PositionHolder(Vector3D targetVec, float margin)
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
      while (!isFinshed) {
        try {
          wait(1);
        } catch (InterruptedException ex) {
          Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }

    public void run()
    {
      do {
        try {
          wait(1);

          // get current attitude telemetry
          SEPP_IADCS_API_QUATERNION_FLOAT telemetry =
              adcsApi.Get_Attitude_Telemetry().getATTITUDE_QUATERNION_BF();
          Rotation currentRotation = new Rotation(telemetry.getQ(), telemetry.getQ_I(),
              telemetry.getQ_J(), telemetry.getQ_K(), true);

          /* calculate rotation angles
             by creating a rotation from the camera vector (in spacecraft frame)
             to the target vector
             (which has to be transformed into spacecraft frame from ICRF, hence the applyInverse)*/
          Vector3D diff = new Vector3D(
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

        } catch (InterruptedException ex) {
          Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.SEVERE, null,
              ex);
        }

      } while (isHoldingPosition); // do this while the vectorpointing mode is active

      // cleanup: stop every mode that is possibly running
      adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
          SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X);
      adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
          SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y);
      adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
          SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y);

      adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
          SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X);
      adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
          SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y);
      adcsApi.Stop_SingleAxis_AngularVelocity_Controller(
          SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Z);

      isFinshed = true;
    }
  }

  private SEPP_IADCS_API_ORBIT_TLE_DATA readTLEFile() throws IOException, FileNotFoundException
  {
    File f = new File("/etc/tle");
    BufferedReader br = new BufferedReader(new FileReader(f));
    String s;
    List<String> lines = new ArrayList<String>();
    while ((s = br.readLine()) != null) {
      String l = new String(s);
      lines.add(l);
    }
    if (lines.size() == 3) {
      lines.remove(0);
    }
    SEPP_IADCS_API_ORBIT_TLE_DATA tle = new SEPP_IADCS_API_ORBIT_TLE_DATA();
    byte[] l1 = lines.get(0).getBytes();
    byte[] l2 = lines.get(1).getBytes();

    LOGGER.log(Level.INFO,
        "Successfully loaded " + l1.length + " bytes of line 1.");
    LOGGER.log(Level.INFO,
        "Successfully loaded " + l2.length + " bytes of line 2.");
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
  public void setLOSVector(VectorF3D los) throws IllegalArgumentException
  {
    double x2 = (double) los.getX() * (double) los.getX();
    double y2 = (double) los.getY() * (double) los.getY();
    double z2 = (double) los.getZ() * (double) los.getZ();
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
  public void setFlightVector(VectorF3D flight) throws IllegalArgumentException
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
  public void setTargetVector(VectorF3D target) throws IllegalArgumentException
  {
    if (!isUnity(target)) {
      throw new IllegalArgumentException("The provided target vector needs to have length 1.");
    }
    targetVector = convertToAdcsApiVector(target);
  }

  /**
   * Checks if a given VectorF3D has the euclidian length 1 (is a unit vector).
   *
   * @param vec The vector to be checked.
   * @return True iff length is 1.
   */
  private boolean isUnity(VectorF3D vec)
  {
    double x2 = (double) vec.getX() * (double) vec.getX();
    double y2 = (double) vec.getY() * (double) vec.getY();
    double z2 = (double) vec.getZ() * (double) vec.getZ();
    return Math.sqrt(x2 + y2 + z2) == 1.0;
  }

  private void dumpHKTelemetry()
  {
    LOGGER.log(Level.INFO, "Dumping HK Telemetry...");
    //SEPP_IADCS_API_STANDARD_TELEMETRY stdTM = adcsApi.Get_Standard_Telemetry();
    SEPP_IADCS_API_POWER_STATUS_TELEMETRY powerTM = adcsApi.Get_Power_Status_Telemetry();
    //SEPP_IADCS_API_INFO_TELEMETRY infoTM = adcsApi.Get_Info_Telemetry();
    /*Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO,
        String.format("Standard TM:\n"
            + "IADCS_STATUS_REGISTER = %d\n"
            + "IADCS_ERROR_REGISTER = %d\n"
            + "CONTROL_STATUS_REGISTER = %d\n"
            + "CONTROL_ERROR_REGISTER = %d\n"
            + "LIVELYHOOD_REGISTER = %d\n"
            + "ELAPSED_SECONDS_SINCE_EPOCH_SEC = %d\n"
            + "ELAPSED_SUBSECONDS_SINCE_EPOCH_MSEC = %d\n"
            + "GYRO_1_TEMPERATURE_DEGC = %d\n"
            + "GYRO_2_TEMPERATURE_DEGC = %d\n", stdTM.getIADCS_STATUS_REGISTER(),
            stdTM.getIADCS_ERROR_REGISTER(),
            stdTM.getCONTROL_STATUS_REGISTER(),
            stdTM.getCONTROL_ERROR_REGISTER(),
            stdTM.getLIVELYHOOD_REGISTER(),
            stdTM.getELAPSED_SECONDS_SINCE_EPOCH_SEC(),
            stdTM.getELAPSED_SUBSECONDS_SINCE_EPOCH_MSEC(),
            stdTM.getGYRO_1_TEMPERATURE_DEGC(),
            stdTM.getGYRO_2_TEMPERATURE_DEGC()));*/
    LOGGER.log(Level.INFO,
        String.format("Power TM:\n"
            + " MAGNETTORQUER_POWER_CONSUMPTION_W = %.3f\n"
            + " MAGNETTORQUER_SUPPLY_VOLTAGE_V = %.3f\n"
            + " STARTRACKER_CURRENT_CONSUMPTION_A = %.3f\n"
            + " STARTRACKER_POWER_CONSUMPTION_W = %.3f\n"
            + " STARTRACKER_SUPPLY_VOLTAGE_V = %.3f\n"
            + " IADCS_CURRENT_CONSUMPTION_A = %.3f\n"
            + " IADCS_POWER_CONSUMPTION_W = %.3f\n"
            + " IADCS_SUPPLY_VOLTAGE_V = %.3f\n"
            + " REACTIONWHEEL_CURRENT_CONSUMPTION_A = %.3f\n"
            + " REACTIONWHEEL_POWER_CONSUMPTION_W = %.3f\n"
            + " REACTIONWHEEL_SUPPLY_VOLTAGE_V = %.3f\n",
            powerTM.getMAGNETTORQUER_POWER_CONSUMPTION_W(),
            powerTM.getMAGNETTORQUER_SUPPLY_VOLTAGE_V(),
            powerTM.getSTARTRACKER_CURRENT_CONSUMPTION_A(),
            powerTM.getSTARTRACKER_POWER_CONSUMPTION_W(),
            powerTM.getSTARTRACKER_SUPPLY_VOLTAGE_V(),
            powerTM.getIADCS_CURRENT_CONSUMPTION_A(),
            powerTM.getIADCS_POWER_CONSUMPTION_W(),
            powerTM.getIADCS_SUPPLY_VOLTAGE_V(),
            powerTM.getREACTIONWHEEL_CURRENT_CONSUMPTION_A(),
            powerTM.getREACTIONWHEEL_POWER_CONSUMPTION_W(),
            powerTM.getREACTIONWHEEL_SUPPLY_VOLTAGE_V()));
    /*Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO,
        String.format("Info TM:\n"
            + "PRIMARY_TARGET_TYPE = %d\n"
            + "SECONDARY_TARGET_TYPE = %d\n"
            + "DEVICE_NAME = %s\n"
            + "DEVICE_MODEL_NAME = %s\n"
            + "SERIAL_NUMBER = %d\n"
            + "COMPILE_TIME = %s\n"
            + "SOFTWARE_VERSION = %s\n"
            + "DEBUG_LEVEL = %d\n"
            + "GIT_COMMIT_ID = %s\n"
            + "COMPILER = %s\n"
            + "COMPILER_VERSION = %s\n",
            infoTM.getPRIMARY_TARGET_TYPE(),
            infoTM.getSECONDARY_TARGET_TYPE(),
            infoTM.getDEVICE_NAME(),z2
            infoTM.getDEVICE_MODEL_NAME(),
            infoTM.getSERIAL_NUMBER(),
            infoTM.getCOMPILE_TIME(),
            infoTM.getSOFTWARE_VERSION(),
            infoTM.getDEBUG_LEVEL(),
            infoTM.getGIT_COMMIT_ID(),
            infoTM.getCOMPILER(),
            infoTM.getCOMPILER_VERSION()));*/
  }

  static private Quaternion convertAdcsApiQuaternion(SEPP_IADCS_API_QUATERNION_FLOAT in)
  {
    return new Quaternion(in.getQ(), in.getQ_I(), in.getQ_J(), in.getQ_K());
  }

  static private MagnetorquersState convertAdcsApiMtqState(long in)
  {
    int mappedOrdinal;
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

  static private SEPP_IADCS_API_VECTOR3_XYZ_FLOAT convertToAdcsApiVector(VectorF3D vec)
  {
    SEPP_IADCS_API_VECTOR3_XYZ_FLOAT sepp = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
    sepp.setX(vec.getX());
    sepp.setY(vec.getY());
    sepp.setZ(vec.getZ());
    return sepp;
  }

  static private VectorF3D convertAdcsApiVector(SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in)
  {
    return new VectorF3D(in.getX(), in.getY(), in.getZ());
  }

  static private VectorF3D convertAdcsApiMagMoment(SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in)
  {
    // Moment is provided in A*m^2
    return new VectorF3D((float) in.getX(), (float) in.getY(), (float) in.getZ());
  }

  static private WheelsSpeed convertAdcsApiWheelSpeed(SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in1,
      SEPP_IADCS_API_VECTOR3_UVW_FLOAT in2)
  {
    FloatList list = new FloatList(6);
    list.add((float) in1.getX());
    list.add((float) in1.getY());
    list.add((float) in1.getZ());
    list.add((float) in2.getU());
    list.add((float) in2.getV());
    list.add((float) in2.getW());
    return new WheelsSpeed(list);
  }

  @Override
  public void setDesiredAttitude(AttitudeMode attitude) throws IOException,
      UnsupportedOperationException
  {
    if (attitude instanceof AttitudeModeBDot) {
      AttitudeModeBDot a = (AttitudeModeBDot) attitude;
      SEPP_IADCS_API_DETUMBLING_MODE_PARAMETERS params =
          new SEPP_IADCS_API_DETUMBLING_MODE_PARAMETERS();
      adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
      params.setSTART_EPOCH_TIME(BigInteger.valueOf(System.currentTimeMillis()));
      params.setSTOP_EPOCH_TIME(BigInteger.valueOf(Long.MAX_VALUE));
      adcsApi.Start_Operation_Mode_Detumbling(params);
      activeAttitudeMode = a;
    } else if (attitude instanceof AttitudeModeNadirPointing) {
      if (losVector == null || flightVector == null) {
        throw new IOException(
            "LOS vector or flight vector not set. Call setLOSVector and setFlightVector before calling setDesiredAttitudeMode.");
      }
      AttitudeModeNadirPointing a = (AttitudeModeNadirPointing) attitude;
      SEPP_IADCS_API_TARGET_POINTING_NADIR_MODE_PARAMETERS params =
          new SEPP_IADCS_API_TARGET_POINTING_NADIR_MODE_PARAMETERS();
      // Set parameters
      params.setLOS_VECTOR_BF(losVector);
      params.setFLIGHT_VECTOR_BF(flightVector);
      params.setUPDATE_INTERVAL(BigInteger.valueOf(500));
      params.setMODE(
          SEPP_IADCS_API_TARGET_POINTING_ATTITUDE_DETERMINATION_MODES.IADCS_ATTITUDE_DETERMINATION_STARTRACKER_ONLY);

      params.setTOLERANCE_PARAMETERS(tolerance);
      params.setOFFSET_TIME(.0f);
      adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
      adcsApi.Init_Orbit_Module(readTLEFile());
      adcsApi.Start_Target_Pointing_Nadir_Mode(params);
      activeAttitudeMode = a;
    } else if (attitude instanceof AttitudeModeSingleSpinning) {
      AttitudeModeSingleSpinning a = (AttitudeModeSingleSpinning) attitude;
      VectorF3D target = a.getBodyAxis();
      SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS vec;
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
      if (targetVector == null) {
        throw new IOException(
            "Target vector is not set. Call setTargetVector before calling setDesiredAttitudeMode.");
      }
      AttitudeModeSunPointing sunPointing = (AttitudeModeSunPointing) attitude;
      SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS params =
          new SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS();
      params.setSTART_EPOCH_TIME(BigInteger.valueOf(0));
      params.setSTOP_EPOCH_TIME(BigInteger.valueOf(Long.MAX_VALUE));
      params.setTARGET_VECTOR_BF(targetVector);
      adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
      adcsApi.Init_Orbit_Module(readTLEFile());
      adcsApi.Start_Operation_Mode_Sun_Pointing(params);
      activeAttitudeMode = sunPointing;
    } else if (attitude instanceof AttitudeModeTargetTracking) {
      if (losVector == null || flightVector == null) {
        throw new IOException("LOS or flight vector not set.");
      }

      SEPP_IADCS_API_EARTH_TARGET_POINTING_FIXED_MODE_PARAMETERS params =
          new SEPP_IADCS_API_EARTH_TARGET_POINTING_FIXED_MODE_PARAMETERS();

      params.setMODE(
          SEPP_IADCS_API_TARGET_POINTING_ATTITUDE_DETERMINATION_MODES.IADCS_ATTITUDE_DETERMINATION_STARTRACKER_ONLY);
      params.setTOLERANCE_PARAMETERS(tolerance);
      params.setFLIGHT_VECTOR_BF(flightVector);
      params.setLOS_VECTOR_BF(losVector);
      params.setUPDATE_INTERVAL(BigInteger.valueOf(500));
      AttitudeModeTargetTracking a = (AttitudeModeTargetTracking) attitude;
      params.setTARGET_LATITUDE(a.getLatitude());
      params.setTARGET_LONGITUDE(a.getLongitude());
      adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
      adcsApi.Init_Orbit_Module(readTLEFile());
      adcsApi.Start_Target_Pointing_Earth_Fix_Mode(params);
      activeAttitudeMode = a;
    } else if (attitude instanceof AttitudeModeTargetTrackingLinear) {
      if (losVector == null || flightVector == null) {
        throw new IOException("LOS or flight vector not set.");
      }

      SEPP_IADCS_API_EARTH_TARGET_POINTING_CONST_VELOCITY_MODE_PARAMETERS params =
          new SEPP_IADCS_API_EARTH_TARGET_POINTING_CONST_VELOCITY_MODE_PARAMETERS();
      params.setFLIGHT_VECTOR_BF(flightVector);
      params.setLOS_VECTOR_BF(losVector);
      params.setMODE(
          SEPP_IADCS_API_TARGET_POINTING_ATTITUDE_DETERMINATION_MODES.IADCS_ATTITUDE_DETERMINATION_STARTRACKER_ONLY);
      params.setOFFSET_TIME(BigInteger.valueOf(0));
      AttitudeModeTargetTrackingLinear a = (AttitudeModeTargetTrackingLinear) attitude;
      params.setSTART_EPOCH_TIME(BigInteger.valueOf(a.getStartEpoch()));
      params.setSTOP_EPOCH_TIME(BigInteger.valueOf(a.getEndEpoch()));
      params.setSTART_LATITUDE(a.getLatitudeStart());
      params.setSTART_LONGITUDE(a.getLongitudeStart());
      params.setSTOP_LATITUDE(a.getLatitudeEnd());
      params.setSTOP_LONGITUDE(a.getLongitudeEnd());
      params.setTOLERANCE_PARAMETERS(tolerance);
      params.setUPDATE_INTERVAL(BigInteger.valueOf(500));
      adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.currentTimeMillis()));
      adcsApi.Init_Orbit_Module(readTLEFile());
      adcsApi.Start_Target_Pointing_Earth_Const_Velocity_Mode(params);
      activeAttitudeMode = a;
    } else if (attitude instanceof AttitudeModeVectorPointing) {

      AttitudeModeVectorPointing a = (AttitudeModeVectorPointing) attitude;

      holder = new PositionHolder(new Vector3D(a.getTarget().getX(), a.getTarget().getY(),
          a.getTarget().getZ()), a.getMargin());
      Thread runner = new Thread(holder);
      runner.start();
    } else {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }

  @Override
  public void setAllReactionWheelSpeeds(float wheelX, float wheelY, float wheelZ, float wheelU,
      float wheelV, float wheelW)
  {
    SEPP_IADCS_API_REACTIONWHEEL_SPEEDS speeds = new SEPP_IADCS_API_REACTIONWHEEL_SPEEDS();

    SEPP_IADCS_API_VECTOR3_XYZ_FLOAT xyz = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
    xyz.setX(wheelX);
    xyz.setY(wheelY);
    xyz.setZ(wheelZ);
    speeds.setINTERNAL(xyz);

    SEPP_IADCS_API_VECTOR3_UVW_FLOAT uvw = new SEPP_IADCS_API_VECTOR3_UVW_FLOAT();
    uvw.setU(wheelU);
    uvw.setV(wheelV);
    uvw.setW(wheelW);
    speeds.setEXTERNAL(uvw);

    adcsApi.Set_ReactionWheel_All_Speeds(speeds);
  }

  @Override
  public void setReactionWheelSpeed(ReactionWheelIdentifier wheel, float Speed)
  {
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

  @Override
  public void setAllMagnetorquersDipoleMoments(Float dipoleX, Float dipoleY, Float dipoleZ)
  {
    SEPP_IADCS_API_VECTOR3_XYZ_FLOAT moments = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();

    moments.setX(dipoleX);
    moments.setY(dipoleY);
    moments.setZ(dipoleZ);

    adcsApi.Set_Magnettorquer_All_Dipole_Moments(moments);
  }

  @Override
  public void unset() throws IOException
  {
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
          SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y);
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
    adcsApi.Set_Operation_Mode_Idle();
  }

  @Override
  public boolean isUnitAvailable()
  {
    return initialized;
  }

  @Override
  public AttitudeTelemetry getAttitudeTelemetry() throws IOException
  {
    SEPP_IADCS_API_ATTITUDE_TELEMETRY attitudeTm = adcsApi.Get_Attitude_Telemetry();
    attitudeTm.getATTITUDE_QUATERNION_BF();
    Quaternion attitude = convertAdcsApiQuaternion(attitudeTm.getATTITUDE_QUATERNION_BF());
    VectorF3D angularVel = convertAdcsApiVector(attitudeTm.getANGULAR_VELOCITY_VECTOR_RADPS());
    VectorF3D magneticField = convertAdcsApiVector(
        attitudeTm.getMEASURED_MAGNETIC_FIELD_VECTOR_BF_T());
    VectorF3D sunVector = convertAdcsApiVector(attitudeTm.getMEASURED_SUN_VECTOR_BF());
    return new AttitudeTelemetry(attitude, angularVel, sunVector, magneticField);
  }

  @Override
  public ActuatorsTelemetry getActuatorsTelemetry() throws IOException
  {
    SEPP_IADCS_API_ACTUATOR_TELEMETRY actuatorTm = adcsApi.Get_Actuator_Telemetry();
    WheelsSpeed targetSpeed = convertAdcsApiWheelSpeed(
        actuatorTm.getREACTIONWHEEL_TARGET_SPEED_VECTOR_XYZ_RADPS(),
        actuatorTm.getREACTIONWHEEL_TARGET_SPEED_VECTOR_UVW_RADPS());
    WheelsSpeed currentSpeed = convertAdcsApiWheelSpeed(
        actuatorTm.getREACTIONWHEEL_CURRENT_SPEED_VECTOR_XYZ_RADPS(),
        actuatorTm.getREACTIONWHEEL_CURRENT_SPEED_VECTOR_UVW_RADPS());
    VectorF3D mtqDipoleMoment = convertAdcsApiMagMoment(
        actuatorTm.getMAGNETORQUERS_TARGET_DIPOLE_MOMENT_VECTOR_AM2());
    MagnetorquersState mtqState =
        convertAdcsApiMtqState(actuatorTm.getMAGNETORQUERS_CURRENT_STATE());
    return new ActuatorsTelemetry(targetSpeed, currentSpeed, mtqDipoleMoment, mtqState);
  }

  @Override
  public String validateAttitudeDescriptor(AttitudeMode attitude)
  {
    return ""; //Return no error for now
  }

  @Override
  public AttitudeMode getActiveAttitudeMode()
  {
    return activeAttitudeMode;
  }
}
