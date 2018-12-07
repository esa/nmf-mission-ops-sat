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
import java.io.IOException;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.structures.FloatList;
import org.ccsds.moims.mo.platform.autonomousadcs.structures.*;

/**
 *
 * @author Cesar Coelho
 */
public class AutonomousADCSOPSSATAdapter implements AutonomousADCSAdapterInterface
{

  private AttitudeMode activeAttitudeMode;
  private final SEPP_IADCS_API adcsApi;
  private boolean unitAvailable = false;

  public AutonomousADCSOPSSATAdapter()
  {
    Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO, "Initialisation");
    System.loadLibrary("iadcs_api_jni");
    adcsApi = new SEPP_IADCS_API();
    activeAttitudeMode = null;
    try {
      dumpHKTelemetry();
    } catch (Exception e) {
      Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.SEVERE,
          "Failed to initialize iADCS", e);
      unitAvailable = false;
      return;
    }
    unitAvailable = true;
  }

  private void dumpHKTelemetry()
  {
    Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO,
        "Dumping HK Telemetry...");
    SEPP_IADCS_API_STANDARD_TELEMETRY stdTM = adcsApi.Get_Standard_Telemetry();
    SEPP_IADCS_API_POWER_STATUS_TELEMETRY powerTM = adcsApi.Get_Power_Status_Telemetry();
    SEPP_IADCS_API_INFO_TELEMETRY infoTM = adcsApi.Get_Info_Telemetry();
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
    Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO,
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
            infoTM.getDEVICE_NAME(),
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

  static private Vector3D convertAdcsApiVector(SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in)
  {
    return new Vector3D(in.getX(), in.getY(), in.getZ());
  }

  static private Vector3D convertAdcsApiMagMoment(SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in)
  {
    // Moment is provided in A*m^2
    return new Vector3D((float) in.getX(), (float) in.getY(), (float) in.getZ());
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
    } else if (attitude instanceof AttitudeModeNadirPointing) {
    } else if (attitude instanceof AttitudeModeSingleSpinning) {
    } else if (attitude instanceof AttitudeModeSunPointing) {
      AttitudeModeSunPointing sunPointing = (AttitudeModeSunPointing) attitude;
      SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS params
          = new SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS();
      params.setSTART_EPOCH_TIME(BigInteger.valueOf(0));
      params.setSTOP_EPOCH_TIME(BigInteger.valueOf(0xFFFFFFFF));
      adcsApi.Start_Operation_Mode_Sun_Pointing(params);
      return;
    } else if (attitude instanceof AttitudeModeTargetTracking) {
    }
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void unset() throws IOException
  {
    if (activeAttitudeMode instanceof AttitudeModeBDot) {
    } else if (activeAttitudeMode instanceof AttitudeModeNadirPointing) {
    } else if (activeAttitudeMode instanceof AttitudeModeSingleSpinning) {
    } else if (activeAttitudeMode instanceof AttitudeModeSunPointing) {
      adcsApi.Stop_Operation_Mode_Sun_Pointing();
    } else if (activeAttitudeMode instanceof AttitudeModeTargetTracking) {
    }
    activeAttitudeMode = null;
    adcsApi.Set_Operation_Mode_Idle();
  }

  @Override
  public boolean isUnitAvailable()
  {
    return unitAvailable;
  }

  @Override
  public AttitudeTelemetry getAttitudeTelemetry() throws IOException
  {
    SEPP_IADCS_API_ATTITUDE_TELEMETRY attitudeTm = adcsApi.Get_Attitude_Telemetry();
    attitudeTm.getATTITUDE_QUATERNION_BF();
    Quaternion attitude = convertAdcsApiQuaternion(attitudeTm.getATTITUDE_QUATERNION_BF());
    Vector3D angularVel = convertAdcsApiVector(attitudeTm.getANGULAR_VELOCITY_VECTOR_RADPS());
    Vector3D magneticField = convertAdcsApiVector(
        attitudeTm.getMEASURED_MAGNETIC_FIELD_VECTOR_BF_T());
    Vector3D sunVector = convertAdcsApiVector(attitudeTm.getMEASURED_SUN_VECTOR_BF());
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
    Vector3D mtqDipoleMoment = convertAdcsApiMagMoment(
        actuatorTm.getMAGNETORQUERS_TARGET_DIPOLE_MOMENT_VECTOR_AM2());
    MagnetorquersState mtqState
        = convertAdcsApiMtqState(actuatorTm.getMAGNETORQUERS_CURRENT_STATE());
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
