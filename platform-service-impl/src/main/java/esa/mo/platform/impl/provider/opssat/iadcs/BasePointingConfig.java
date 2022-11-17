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
package esa.mo.platform.impl.provider.opssat.iadcs;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.ini4j.Ini;
import at.tugraz.ihf.opssat.iadcs.*;

/**
 * Fundamental class used to load pointing config from ini files. The child
 * classes can then provide their specific behaviour, along with conversions to
 * specific mode-parameter structures.
 */
public abstract class BasePointingConfig {
    private static final Logger LOGGER = Logger.getLogger(BasePointingConfig.class.getName());
    private static final int DEFAULT_SENSOR_UPDATE_INTERVAL_MS = 500;
    private static final float DEFAULT_PA_ANGLE_TOL_RAD = 0.0872665f;
    private static final float DEFAULT_PA_ANGLE_TOL_PERCENT = 20.0f;
    private static final float DEFAULT_PA_ANGLE_VEL_TOL_RADPS = 0.00872665f;
    private static final float DEFAULT_PA_STEP_X_RAD = 0.f;
    private static final float DEFAULT_PA_STEP_Y_RAD = 1.396263f;
    private static final float DEFAULT_PA_STEP_Z_RAD = -1.396263f;
    private static final long DEFAULT_PA_WAIT_TIME_MS = 60000;
    private static final float DEFAULT_PA_TARGET_THRESHOLD_RAD = 0.261799f;
    // Determination Mode: 0 = MAG+SUN only, 1 = STR+MAG+SUN, 2 = STR only
    // Note that not using STR only means that it is not necessary to assume
    // that there is an initial attitude lock - it will be used if available anyway
    private static final int DEFAULT_DETERMINATION_MODE = 0;

    private static final float DEFAULT_GYRO_BIAS_X_RADPS = 0.00037699f;
    private static final float DEFAULT_GYRO_BIAS_Y_RADPS = 0.00053930f;
    private static final float DEFAULT_GYRO_BIAS_Z_RADPS = -0.00134390f;

    private static final float DEFAULT_BIAS_PROCESS_VARIANCE = 1.000000000E-16f;
    private static final float DEFAULT_ATTITUDE_PROCESS_VARIANCE = 2.741556778E-9f;
    private static final float DEFAULT_SUN_MAG_MEAS_VARIANCE = 0.0076153f;
    private static final float DEFAULT_STARTRACKER_MEAS_VARIANCE = 1.504283555E-7f;

    private static final float DEFAULT_SLIDING_CONTROLLER_GAIN_K1 = 0.2f;
    private static final float DEFAULT_SLIDING_CONTROLLER_GAIN_K2 = 0.2f;
    private static final int DEFAULT_SLIDING_CONTROLLER_UPDATE_INTERVAL_MSEC = 200;

    // Internal sensor polling interval
    BigInteger sensorUpdateIntervalMsec;

    // Prealignment configuration
    SEPP_IADCS_API_TARGET_POINTING_TOLERANCE_PARAMETERS paTolerance;
    // Size of each rotation step during the PA
    SEPP_IADCS_API_VECTOR3_XYZ_FLOAT paAnglesRad;
    // Wait time between reaching startracker lock and going into pointing mode, for
    // gyro bias to converge
    BigInteger paWaitTimeMsec;

    // Attitude determination mode 6.5.4.7.6
    SEPP_IADCS_API_TARGET_POINTING_ATTITUDE_DETERMINATION_MODES determinationMode;

    // Line of sight vector [vlos,x, vlos,y, vlos,z] towards the target
    // (earth-based, sun, or inertial) as a unit vector in body frame
    SEPP_IADCS_API_VECTOR3_XYZ_FLOAT targetVector;
    // Flight vector [vvel,x, vvel,y, vvel,z ] as a unit vector in body frame
    SEPP_IADCS_API_VECTOR3_XYZ_FLOAT flightVector;

    // Enable gyro-bias correction
    boolean enableGyroBiasCorrection;
    // gyro bias vector [x,y,z] in rad/s
    SEPP_IADCS_API_VECTOR3_XYZ_FLOAT gyroBiasVector;

    // Kalman Filter parameters
    SEPP_IADCS_API_KALMAN_FILTER_PARAMETERS kfParams;

    // Enable external magnetometer readings
    boolean externalMagEnable;
    SEPP_IADCS_API_MAGNETOMETER_PARAMETERS externalMagParams;

    // Enable SC reconfiguration
    boolean slidingControllerConfigEnable;
    SEPP_IADCS_API_SLIDING_CONTROLLER_PARAMETERS scConfig;

    protected BasePointingConfig() {
        kfParams = new SEPP_IADCS_API_KALMAN_FILTER_PARAMETERS();
        paTolerance = new SEPP_IADCS_API_TARGET_POINTING_TOLERANCE_PARAMETERS();
        paAnglesRad = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
        targetVector = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
        flightVector = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
        gyroBiasVector = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();

        // External mag params are hardcoded for now
        externalMagParams = new SEPP_IADCS_API_MAGNETOMETER_PARAMETERS();
        externalMagParams.setSENSOR_STDDEV(IADCSTools.convertToApiVector(0.0000001f, 0.0000001f, 0.0000001f));
        externalMagParams.setSENSOR_SENSITIVITY(IADCSTools.convertToApiVector(1.0f, 1.0f, 1.0f));
        externalMagParams.setTRANSFORMATION_QUATERNION(IADCSTools.convertToApiQuaternion(0, 0, 0, 1.0f));
        externalMagParams.setUPDATE_INTERVAL_MSEC(BigInteger.valueOf(1000));
        externalMagParams.setSENSOR_CORRECTION_ENABLE((short) 0);
        externalMagParams.setCORRECTION_SCALE(IADCSTools.convertToApiMatrix(1, 0, 0, 0, 1, 0, 0, 0, 1));
        externalMagParams.setCORRECTION_OFFSET(IADCSTools.convertToApiVector(0, 0, 0));
        externalMagParams.setAVG_FILTER_ENABLE((short) 1);
        externalMagParams.setAVG_FILTER_COUNTER(1);
        externalMagParams.setAVG_FILTER_CRITERION(0.0001f);
        externalMagParams.setMOVING_AVG_FILTER_ENABLE((short) 0);
        externalMagParams.setMOVING_AVG_FILTER_GAIN(0.9f);
        externalMagParams.setMOVING_AVG_FILTER_CRITERION(0.0001f);

        // External SC is partially hardcoded
        scConfig = new SEPP_IADCS_API_SLIDING_CONTROLLER_PARAMETERS();
        scConfig.setUPDATE_INTERVAL_MSEC(BigInteger.valueOf(DEFAULT_SLIDING_CONTROLLER_UPDATE_INTERVAL_MSEC));
        scConfig.setCTRL_ACTUATOR_CMD_IF(
            SEPP_IADCS_API_CONTROLLER_ACTUATOR_CMD_INTERFACES.IADCS_CTRL_ACTUATOR_CMD_IF_3);
    }

    protected abstract String getConfigFileName();

    protected abstract String getConfigSectionName();

    public void load() {
        load(getConfigFileName(), getConfigSectionName());
    }

    public void load(String configFileName, String mainSectionName) {
        Ini ini = new Ini();
        try {
            ini.load(new File(configFileName));
            readFromMap(ini.get(mainSectionName));
        } catch (IOException e) {
            LOGGER.warning("Cannot load " + getConfigFileName());
            readFromMap(new HashMap<String, String>());
        }
    }

    /**
     * 
     * @param map Key-value pairs read from ini
     */
    protected void readFromMap(Map<String, String> map) {
        sensorUpdateIntervalMsec = BigInteger.valueOf(strToLong(map.get("update_interval_msec"),
            DEFAULT_SENSOR_UPDATE_INTERVAL_MS));
        paTolerance.setPREALIGNMENT_ANGLE_TOLERANCE_RAD(strToFloat(map.get("pa_angle_tol_rad"),
            DEFAULT_PA_ANGLE_TOL_RAD));
        paTolerance.setPREALIGNMENT_ANGLE_TOLERANCE_PERCENT(strToFloat(map.get("pa_angle_tol_perc"),
            DEFAULT_PA_ANGLE_TOL_PERCENT));
        paTolerance.setPREALIGNMENT_ANGULAR_VELOCITY_TOLERANCE_RADPS(strToFloat(map.get("pa_angvel_tol_radps"),
            DEFAULT_PA_ANGLE_VEL_TOL_RADPS));
        paTolerance.setPREALIGNMENT_TARGET_THRESHOLD_RAD(strToFloat(map.get("pa_tgt_thd_rad"),
            DEFAULT_PA_TARGET_THRESHOLD_RAD));

        paAnglesRad.setX(strToFloat(map.get("pa_angles_x_rad"), DEFAULT_PA_STEP_X_RAD));
        paAnglesRad.setY(strToFloat(map.get("pa_angles_y_rad"), DEFAULT_PA_STEP_Y_RAD));
        paAnglesRad.setZ(strToFloat(map.get("pa_angles_z_rad"), DEFAULT_PA_STEP_Z_RAD));

        paWaitTimeMsec = BigInteger.valueOf(strToLong(map.get("pa_wait_time_msec"), DEFAULT_PA_WAIT_TIME_MS));

        determinationMode = SEPP_IADCS_API_TARGET_POINTING_ATTITUDE_DETERMINATION_MODES.swigToEnum(strToInt(map.get(
            "det_mode"), DEFAULT_DETERMINATION_MODE));

        targetVector.setX(strToFloat(map.get("los_vec_x_bf"), 0));
        targetVector.setY(strToFloat(map.get("los_vec_y_bf"), 0));
        targetVector.setZ(strToFloat(map.get("los_vec_z_bf"), -1));

        flightVector.setX(strToFloat(map.get("flight_vec_x_bf"), 0));
        flightVector.setY(strToFloat(map.get("flight_vec_x_bf"), -1));
        flightVector.setZ(strToFloat(map.get("flight_vec_x_bf"), 0));

        enableGyroBiasCorrection = strToBool(map.get("gyro_bias_correction_en"), true);
        gyroBiasVector.setX(strToFloat(map.get("gyro_bias_x"), DEFAULT_GYRO_BIAS_X_RADPS));
        gyroBiasVector.setY(strToFloat(map.get("gyro_bias_y"), DEFAULT_GYRO_BIAS_Y_RADPS));
        gyroBiasVector.setZ(strToFloat(map.get("gyro_bias_z"), DEFAULT_GYRO_BIAS_Z_RADPS));

        // Disable Kalman-Filter Bias correction by default - does not work properly
        kfParams.setDISABLE_BIAS(strToBool(map.get("disable_kf_bias"), true));

        kfParams.setATTITUDE_PROCESS_VARIANCE(strToFloat(map.get("bias_proc_variance"), DEFAULT_BIAS_PROCESS_VARIANCE));
        kfParams.setATTITUDE_PROCESS_VARIANCE(strToFloat(map.get("attitude_process_variance"),
            DEFAULT_ATTITUDE_PROCESS_VARIANCE));
        kfParams.setSUN_MAG_MEASUREMENT_VARIANCE(strToFloat(map.get("sun_mag_measurement_variance"),
            DEFAULT_SUN_MAG_MEAS_VARIANCE));
        kfParams.setSTS_MEASUREMENT_VARIANCE(strToFloat(map.get("startracker_measurement_variance"),
            DEFAULT_STARTRACKER_MEAS_VARIANCE));

        // TODO - the below should likely be tied to the determination mode
        externalMagEnable = strToBool(map.get("external_mm_en"), true);

        slidingControllerConfigEnable = strToBool(map.get("sliding_control_config_en"), true);
        scConfig.setK_1(strToFloat(map.get("sliding_controller_k1"), DEFAULT_SLIDING_CONTROLLER_GAIN_K1));
        scConfig.setK_2(strToFloat(map.get("sliding_controller_k2"), DEFAULT_SLIDING_CONTROLLER_GAIN_K2));
    }

    public SEPP_IADCS_API_TARGET_POINTING_OPERATION_PARAMETERS getTargetPointingOperationParams() {
        SEPP_IADCS_API_TARGET_POINTING_OPERATION_PARAMETERS ret = new SEPP_IADCS_API_TARGET_POINTING_OPERATION_PARAMETERS();
        ret.setANGLES_RAD(paAnglesRad);
        ret.setANGLE_TOLERANCE_PERC(paTolerance.getPREALIGNMENT_ANGLE_TOLERANCE_PERCENT());
        ret.setANGLE_TOLERANCE_RAD(paTolerance.getPREALIGNMENT_ANGLE_TOLERANCE_RAD());
        ret.setSPEED_TOLERANCE_RADPS(paTolerance.getPREALIGNMENT_ANGULAR_VELOCITY_TOLERANCE_RADPS());
        ret.setUPDATE_INTERVAL_MSEC(sensorUpdateIntervalMsec);
        ret.setWAIT_TIME_MSEC(paWaitTimeMsec);
        return ret;
    }

    public SEPP_IADCS_API_SYSTEM_SCHEDULER_REGISTER getSystemSchedulerRegister() {
        SEPP_IADCS_API_SYSTEM_SCHEDULER_REGISTER ret = new SEPP_IADCS_API_SYSTEM_SCHEDULER_REGISTER();
        ret.setORBIT_PROPAGATION_ENABLE(true);
        ret.setCONTROL_MODE_ENABLE(true);
        ret.setREACTIONWHEEL_READING_ENABLE(true);
        ret.setTEMPERATURE_READING_ENABLE(true);
        ret.setPOWER_READING_ENABLE(true);
        ret.setSTARTRACKER_READING_ENABLE(true);
        ret.setSUNSENSOR_READING_ENABLE(true);
        ret.setMAGNETOMETER_READING_ENABLE(!externalMagEnable);
        ret.setHIGHPRECISION_GYRO_READING_ENABLE(true);
        ret.setHIGHSPEED_GYRO_READING_ENABLE(true);
        ret.setLOWSPEED_GYRO_READING_ENABLE(true);
        return ret;
    }

    public static boolean strToBool(String s, boolean defaultValue) {
        if (s == null)
            return defaultValue;
        return s.equalsIgnoreCase("Yes") || s.equalsIgnoreCase("Y");
    }

    public static int strToInt(String s, int defaultValue) {
        if (s == null)
            return defaultValue;
        return Integer.parseInt(s);
    }

    public static long strToLong(String s, long defaultValue) {
        if (s == null)
            return defaultValue;
        return Long.parseLong(s);
    }

    public static float strToFloat(String s, float defaultValue) {
        if (s == null)
            return defaultValue;
        return Float.parseFloat(s);
    }

    public static double strToDouble(String s, double defaultValue) {
        if (s == null)
            return defaultValue;
        return Double.parseDouble(s);
    }

    // Getters and setters
    public BigInteger getSensorUpdateIntervalMsec() {
        return this.sensorUpdateIntervalMsec;
    }

    public void setSensorUpdateIntervalMsec(BigInteger sensorUpdateIntervalMsec) {
        this.sensorUpdateIntervalMsec = sensorUpdateIntervalMsec;
    }

    public SEPP_IADCS_API_TARGET_POINTING_TOLERANCE_PARAMETERS getPaTolerance() {
        return this.paTolerance;
    }

    public void setPaTolerance(SEPP_IADCS_API_TARGET_POINTING_TOLERANCE_PARAMETERS paTolerance) {
        this.paTolerance = paTolerance;
    }

    public SEPP_IADCS_API_VECTOR3_XYZ_FLOAT getPaAnglesRad() {
        return this.paAnglesRad;
    }

    public void setPaAnglesRad(SEPP_IADCS_API_VECTOR3_XYZ_FLOAT paAnglesRad) {
        this.paAnglesRad = paAnglesRad;
    }

    public BigInteger getPaWaitTimeMsec() {
        return this.paWaitTimeMsec;
    }

    public void setPaWaitTimeMsec(BigInteger paWaitTimeMsec) {
        this.paWaitTimeMsec = paWaitTimeMsec;
    }

    public SEPP_IADCS_API_TARGET_POINTING_ATTITUDE_DETERMINATION_MODES getDeterminationMode() {
        return this.determinationMode;
    }

    public void setDeterminationMode(SEPP_IADCS_API_TARGET_POINTING_ATTITUDE_DETERMINATION_MODES determinationMode) {
        this.determinationMode = determinationMode;
    }

    public SEPP_IADCS_API_VECTOR3_XYZ_FLOAT getTargetVector() {
        return this.targetVector;
    }

    public void setTargetVector(SEPP_IADCS_API_VECTOR3_XYZ_FLOAT targetVector) {
        this.targetVector = targetVector;
    }

    public SEPP_IADCS_API_VECTOR3_XYZ_FLOAT getFlightVector() {
        return this.flightVector;
    }

    public void setFlightVector(SEPP_IADCS_API_VECTOR3_XYZ_FLOAT flightVector) {
        this.flightVector = flightVector;
    }

    public boolean isEnableGyroBiasCorrection() {
        return this.enableGyroBiasCorrection;
    }

    public boolean getEnableGyroBiasCorrection() {
        return this.enableGyroBiasCorrection;
    }

    public void setEnableGyroBiasCorrection(boolean enableGyroBiasCorrection) {
        this.enableGyroBiasCorrection = enableGyroBiasCorrection;
    }

    public SEPP_IADCS_API_VECTOR3_XYZ_FLOAT getGyroBiasVector() {
        return this.gyroBiasVector;
    }

    public void setGyroBiasVector(SEPP_IADCS_API_VECTOR3_XYZ_FLOAT gyroBiasVector) {
        this.gyroBiasVector = gyroBiasVector;
    }

    public SEPP_IADCS_API_KALMAN_FILTER_PARAMETERS getKfParams() {
        return this.kfParams;
    }

    public void setKfParams(SEPP_IADCS_API_KALMAN_FILTER_PARAMETERS kfParams) {
        this.kfParams = kfParams;
    }

    public boolean isExternalMagEnable() {
        return this.externalMagEnable;
    }

    public boolean getExternalMagEnable() {
        return this.externalMagEnable;
    }

    public void setExternalMagEnable(boolean externalMagEnable) {
        this.externalMagEnable = externalMagEnable;
    }

    public SEPP_IADCS_API_MAGNETOMETER_PARAMETERS getExternalMagParams() {
        return this.externalMagParams;
    }

    public void setExternalMagParams(SEPP_IADCS_API_MAGNETOMETER_PARAMETERS externalMagParams) {
        this.externalMagParams = externalMagParams;
    }

    public boolean isSlidingControllerConfigEnable() {
        return this.slidingControllerConfigEnable;
    }

    public boolean getSlidingControllerConfigEnable() {
        return this.slidingControllerConfigEnable;
    }

    public void setSlidingControllerConfigEnable(boolean slidingControllerConfigEnable) {
        this.slidingControllerConfigEnable = slidingControllerConfigEnable;
    }

    public SEPP_IADCS_API_SLIDING_CONTROLLER_PARAMETERS getScConfig() {
        return this.scConfig;
    }

    public void setScConfig(SEPP_IADCS_API_SLIDING_CONTROLLER_PARAMETERS scConfig) {
        this.scConfig = scConfig;
    }
}