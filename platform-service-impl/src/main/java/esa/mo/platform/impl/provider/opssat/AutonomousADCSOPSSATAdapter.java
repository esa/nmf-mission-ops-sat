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
    SEPP_IADCS_API_POWER_STATUS_TELEMETRY powerTM = stdTM.getPOWER_TM();
    SEPP_IADCS_API_INFO_TELEMETRY infoTM = adcsApi.Get_Info_Telemetry();
    Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO,
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
            stdTM.getGYRO_2_TEMPERATURE_DEGC()));
    Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO,
        String.format("Power TM:\n"
            + " MAGNETTORQUER_POWER_CONSUMPTION_mW = %d\n"
            + " MAGNETTORQUER_SUPPLY_VOLTAGE_mV = %d\n"
            + " STARTRACKER_CURRENT_CONSUMPTION_mA = %d\n"
            + " STARTRACKER_POWER_CONSUMPTION_mW = %d\n"
            + " STARTRACKER_SUPPLY_VOLTAGE_mV = %d\n"
            + " IADCS_3V3_CURRENTtrue_CONSUMPTION_mA = %d\n"
            + " IADCS_3V3_POWER_CONSUMPTION_mW = %d\n"
            + " IADCS_3V3_SUPPLY_VOLTAGE_mV = %d\n"
            + " REACTIONWHEEL_CURRENT_CONSUMPTION_mA = %d\n"
            + " REACTIONWHEEL_POWER_CONSUMPTION_mW = %d\n"
            + " REACTIONWHEEL_SUPPLY_VOLTAGE_mV = %d\n",
            powerTM.getMAGNETTORQUER_POWER_CONSUMPTION_mW(),
            powerTM.getMAGNETTORQUER_SUPPLY_VOLTAGE_mV(),
            powerTM.getSTARTRACKER_CURRENT_CONSUMPTION_mA(),
            powerTM.getSTARTRACKER_POWER_CONSUMPTION_mW(),
            powerTM.getSTARTRACKER_SUPPLY_VOLTAGE_mV(),
            powerTM.getIADCS_3V3_CURRENT_CONSUMPTION_mA(),
            powerTM.getIADCS_3V3_POWER_CONSUMPTION_mW(),
            powerTM.getIADCS_3V3_SUPPLY_VOLTAGE_mV(),
            powerTM.getREACTIONWHEEL_CURRENT_CONSUMPTION_mA(),
            powerTM.getREACTIONWHEEL_POWER_CONSUMPTION_mW(),
            powerTM.getREACTIONWHEEL_SUPPLY_VOLTAGE_mV()));
    Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO,
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
            infoTM.getCOMPILER_VERSION()));
  }

  static public Vector3D convertAdcsApiVector(SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in)
  {
    return new Vector3D(in.getX(), in.getY(), in.getZ());
  }

  static public Vector3D convertAdcsApiMagMoment(SEPP_IADCS_API_VECTOR3_XYZ_SHORT in)
  {
    // Moment is provided in mA*m^2, convert to A*m^2
    return new Vector3D(((float) in.getX()) * 1000, ((float) in.getY()) * 1000,
        ((float) in.getZ()) * 1000);
  }

  static public WheelsSpeed convertAdcsApiWheelSpeed(SEPP_IADCS_API_VECTOR3_XYZ_SHORT in1,
      SEPP_IADCS_API_VECTOR3_UVW_SHORT in2)
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
    } else if (attitude instanceof AttitudeModeTargetTracking) {
    }
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void unset() throws IOException
  {
    this.activeAttitudeMode = null;
    adcsApi.Set_Operation_Mode(SEPP_IADCS_API_DEV_OPERATION_MODE.IADCS_OPERATION_MODE_IDLE);
  }

  @Override
  public boolean isUnitAvailable()
  {
    return unitAvailable;
  }

  /**
   *
   * @return Measured magnetic flux density in teslas [T]
   */
  public Vector3D getMagneticField()
  {
    SEPP_IADCS_API_MAGNETIC_TELEMETRY sensorTM = adcsApi.Get_Magnetic_Telemetry();
    return convertAdcsApiVector(sensorTM.getMEASURED_FIELD_XYZ());
  }

  @Override
  public AttitudeTelemetry getAttitudeTelemetry() throws IOException
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    /*
    SEPP_IADCS_API_ATTITUDE_TELEMETRY attitudeTm = adcsApi.Get_Attitude_Telemetry();


    AttitudeTelemetry ret = new AttitudeTelemetry();
    ret.setAngularVelocity(convertAdcsApiVector(attitudeTm.getANGULAR_RATE_XYZ()));*/
 /*ret.setAttitude(attitudeTm);
    ret.setMagneticField(HelperIADCS100.getMagneticFieldFromSensorTM(tmBuffer));
    ret.setSunVector(new Vector3D((float)1, (float)0, (float)0)); // TODO provide real data
    return ret;*/
//    byte[] tmBuffer = instrumentsSimulator.getpFineADCS().GetSensorTelemetry();
//    AttitudeTelemetry ret = new AttitudeTelemetry();
//    ret.setAngularVelocity(HelperIADCS100.getAngularVelocityFromSensorTM(tmBuffer));
//    ret.setAttitude(HelperIADCS100.getAttitudeFromSensorTM(tmBuffer));
//    ret.setMagneticField(HelperIADCS100.getMagneticFieldFromSensorTM(tmBuffer));
//    ret.setSunVector(new Vector3D((float)1, (float)0, (float)0)); // TODO provide real data
//    return ret;
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ActuatorsTelemetry getActuatorsTelemetry() throws IOException
  {
    SEPP_IADCS_API_ACTUATOR_TELEMETRY actuatorTm = adcsApi.Get_Actuator_Telemetry();
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String validateAttitudeDescriptor(AttitudeMode attitude)
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public AttitudeMode getActiveAttitudeMode()
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
