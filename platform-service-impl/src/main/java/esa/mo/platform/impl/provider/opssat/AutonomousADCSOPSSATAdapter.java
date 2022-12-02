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

import at.tugraz.ihf.opssat.iadcs.*;
import at.tugraz.ihf.opssat.sepp_api_core.*;
import esa.mo.platform.impl.provider.gen.PowerControlAdapterInterface;
import esa.mo.platform.impl.provider.opssat.iadcs.NadirPointingConfig;
import esa.mo.platform.impl.provider.opssat.iadcs.SunPointingConfig;
import esa.mo.platform.impl.provider.opssat.iadcs.TargetPointingConfig;
import esa.mo.platform.impl.provider.opssat.iadcs.BasePointingConfig;
import esa.mo.platform.impl.provider.opssat.iadcs.IADCSTools;
import esa.mo.platform.impl.provider.gen.AutonomousADCSAdapterInterface;

import org.ccsds.moims.mo.platform.autonomousadcs.structures.*;
import org.ccsds.moims.mo.platform.powercontrol.structures.Device;
import org.ccsds.moims.mo.platform.powercontrol.structures.DeviceList;
import org.ccsds.moims.mo.platform.powercontrol.structures.DeviceType;
import org.ccsds.moims.mo.platform.structures.VectorF3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;

public class AutonomousADCSOPSSATAdapter implements AutonomousADCSAdapterInterface {

    private static final String TLE_PATH = "/etc/tle";
    private static final Logger LOGGER = Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName());

    private static final float MAX_REACTION_WHEEL_SPEED = 1047.197551f;
    private static final float MAX_REACTION_WHEEL_TORQUE = 0.0001f;
    private int iadcsWatchPeriodMS = 10 * 1000;
    private int iadcsInitBackoffMS = 10 * 1000;
    private int initFailedStopThreshold = 3;
    private int powercycleFailedStopThreshold = 2;
    private int powerdownWaitTimeMS = 20 * 1000;
    private int powerupWaitTimeMS = 90 * 1000;

    private AttitudeMode activeAttitudeMode;
    private SEPP_IADCS_API adcsApi;
    private final boolean apiLoaded;
    private int initFailedCount = 0;
    private int powercycleCount = 0;
    private boolean unitInitialized = false;

    private PowerControlAdapterInterface pcAdapter;

    private PositionHolder holder;
    private Thread watcherThread;

    public AutonomousADCSOPSSATAdapter(PowerControlAdapterInterface pcAdapter) {
        this.pcAdapter = pcAdapter;
        LOGGER.log(Level.INFO, "Initialisation");
        try {
            System.loadLibrary("iadcs_api_jni");
            System.loadLibrary("sepp_api_core_jni");
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
    private boolean initIADCS() {
        synchronized (this) {
            loadProperties();
            try {
                SEPP_API_Debug.clearAllLevel();
                adcsApi = new SEPP_IADCS_API();
            } catch (final Exception ex) {
                LOGGER.log(Level.SEVERE, "iADCS API could not get initialized!", ex);
                return false;
            }
            SEPP_IADCS_API_SYSTEM_LOWLEVEL_DCDC_REGISTER dcdc_config = new SEPP_IADCS_API_SYSTEM_LOWLEVEL_DCDC_REGISTER();
            dcdc_config.setEXTERNAL_5V_POWER_SUPPLY_ENABLED(true);
            dcdc_config.setINTERNAL_REACTIONWHEEL_POWER_SUPPLY_ENABLED(true);
            dcdc_config.setMAINBOARD_POWER_SUPPLY_ENABLED(true);
            dcdc_config.setSTARTRACKER_POWER_SUPPLY_ENABLED(true);
            LOGGER.fine("Powering the star tracker on...");
            try {
                adcsApi.Set_DCDC_Configuration(dcdc_config);
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Failed to power star tracker on", e);
                return false;
            }
            try {
                unset(); // Transition to measurement mode
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Failed to switch to measurement mode upon init", e);
                return false;
            }
            try {
                dumpPowerTelemetry();
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Failed to dump iADCS TM", e);
                return false;
            }
            return true;
        }
    }

    public void loadProperties() {
        String iadcsWatchPeriodMSProp = "opssat.adcs.iadcsWatchPeriodMS";
        iadcsWatchPeriodMS = getIntegerProperty(iadcsWatchPeriodMSProp, iadcsWatchPeriodMS);

        String iadcsInitBackoffMSProp = "opssat.adcs.iadcsInitBackoffMS";
        iadcsInitBackoffMS = getIntegerProperty(iadcsInitBackoffMSProp, iadcsInitBackoffMS);

        String initFailedStopThresholdProp = "opssat.adcs.initFailedStopThreshold";
        initFailedStopThreshold = getIntegerProperty(initFailedStopThresholdProp, initFailedStopThreshold);

        String powercycleFailedStopThresholdProp = "opssat.adcs.powercycleFailedStopThreshold";
        powercycleFailedStopThreshold = getIntegerProperty(powercycleFailedStopThresholdProp,
            powercycleFailedStopThreshold);

        String powerdownWaitTimeMSProp = "opssat.adcs.powerdownWaitTimeMS";
        powerdownWaitTimeMS = getIntegerProperty(powerdownWaitTimeMSProp, powerdownWaitTimeMS);

        String powerupWaitTimeMSProp = "opssat.adcs.powerupWaitTimeMS";
        powerupWaitTimeMS = getIntegerProperty(powerupWaitTimeMSProp, powerupWaitTimeMS);
    }

    /**
    * Tries to get a system property and parse it as an Integer.
    * 
    * @param propertyKey The property key
    * @param defaultValue Default value to return if the property is not found
    * @return the parsed system property
    */
    public static int getIntegerProperty(String propertyKey, int defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null) {
            try {
                return Integer.parseInt(propertyValue);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, String.format("Error parsing properties %s to Integer, defaulting to %d",
                    propertyKey, defaultValue), e);
                return defaultValue;
            }
        }
        LOGGER.log(Level.WARNING, String.format("Properties %s not found, defaulting to %d", propertyKey,
            defaultValue));
        return defaultValue;
    }

    public void iADCSPowercycle() {
        DeviceList list = new DeviceList();
        Device d = new Device(false, null, null, DeviceType.ADCS);
        list.add(d);
        try {
            pcAdapter.enableDevices(list);
            Thread.sleep(powerdownWaitTimeMS);
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
        }

        list.clear();
        d = new Device(true, null, null, DeviceType.ADCS);
        list.add(d);
        try {
            pcAdapter.enableDevices(list);
            Thread.sleep(powerupWaitTimeMS);
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
        }
        list.clear();
    }

    /**
     * Monitors the iADCS offline->online transitions and configures it into default mode
     */
    private class IADCSWatcher implements Runnable {
        public IADCSWatcher() {
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(iadcsWatchPeriodMS);
                    boolean isAvailable = isUnitAvailableInternal();
                    if (isAvailable && !unitInitialized && initFailedCount < initFailedStopThreshold) {
                        LOGGER.log(Level.INFO, "iADCS came online - attempting initialisation");
                        if (initIADCS()) {
                            LOGGER.log(Level.INFO, "iADCS initialised - marking available");
                            unitInitialized = true;
                        } else {
                            LOGGER.log(Level.WARNING, "iADCS init failed");
                            initFailedCount++;
                            if (initFailedCount >= initFailedStopThreshold) {
                                LOGGER.log(Level.WARNING,
                                    "iADCS init failed {0} times. Will not retry until it gets powercycled.",
                                    initFailedCount);
                                if (powercycleCount < powercycleFailedStopThreshold) {
                                    LOGGER.log(Level.INFO, "iADCS init failed {0} times - attempting powercycle.",
                                        initFailedCount);
                                    iADCSPowercycle();
                                    powercycleCount++;
                                    initFailedCount = 0;
                                }
                                continue;
                            }
                            LOGGER.log(Level.WARNING, "Sleeping for an extra {0} ms before checking again.",
                                iadcsInitBackoffMS);
                            Thread.sleep(iadcsInitBackoffMS);
                        }
                    } else if (!isAvailable && unitInitialized) {
                        LOGGER.log(Level.INFO, "iADCS gone offline - marking unavailable");
                        adcsApi = null;
                        unitInitialized = false;
                    } else if (!isAvailable) {
                        initFailedCount = 0;
                        powercycleCount = 0; //Edge case - could cause the watcher thread to loop indefinitely if ADCS 
                                            //power-on wait time is not long enough for PowerAdapter to pick up the ADCS unit
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
    private class PositionHolder implements Runnable {

        private final Vector3D targetVec;

        private final float margin;
        private boolean isHoldingPosition;
        private boolean isX = true;
        private boolean isY;
        private boolean isZ;
        private boolean isFinished;

        public PositionHolder(final Vector3D targetVec, final float margin) {
            this.targetVec = targetVec;
            this.margin = margin;
            isHoldingPosition = true;
            LOGGER.log(Level.INFO, "OPSSAT vector pointing initiated");
        }

        /**
         * stops the vector pointing mode
         */
        public void stop() {
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
        public void run() {
            do {
                try {
                    Thread.sleep(1000);
                    synchronized (this) {
                        // get current attitude telemetry
                        final SEPP_IADCS_API_QUATERNION_FLOAT telemetry = adcsApi.Get_Attitude_Telemetry()
                            .getATTITUDE_QUATERNION_BF();
                        final Rotation currentRotation = new Rotation(telemetry.getQ(), telemetry.getQ_I(), telemetry
                            .getQ_J(), telemetry.getQ_K(), true);

                        /* calculate rotation angles
                          by creating a rotation from the camera vector (in spacecraft frame)
                          to the target vector
                          (which has to be transformed into spacecraft frame from ICRF, hence the applyInverse)*/
                        final Vector3D diff = new Vector3D(new Rotation(new Vector3D(0, 0, -1), currentRotation
                            .applyInverseTo(targetVec)).getAngles(RotationOrder.XYZ,
                                RotationConvention.VECTOR_OPERATOR));

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
                                    SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_X, 0);
                                adcsApi.Start_SingleAxis_AngularVelocity_Controller(
                                    SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Y, 0);
                                adcsApi.Start_SingleAxis_AngularVelocity_Controller(
                                    SEPP_IADCS_API_SINGLEAXIS_CONTROL_TARGET_AXIS.IADCS_SINGLEAXIS_CONTROL_TARGET_Z, 0);
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
                    Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
                }

            } while (isHoldingPosition); // do this while the vectorpointing mode is active

            synchronized (this) {
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

    private SEPP_IADCS_API_ORBIT_TLE_DATA readTLEFile(BasePointingConfig config) throws IOException {
        final File f = new File(TLE_PATH);
        final BufferedReader br = new BufferedReader(new FileReader(f));
        String s;
        final List<String> lines = new ArrayList<>();
        while ((s = br.readLine()) != null) {
            lines.add(s);
        }
        br.close();
        if (lines.size() == 3) {
            lines.remove(0);
        }
        final SEPP_IADCS_API_ORBIT_TLE_DATA tle = new SEPP_IADCS_API_ORBIT_TLE_DATA();
        if (lines.get(0).length() != 69) {
            throw new IOException(MessageFormat.format("TLE Line 1 is not 69 characters long ({0}): {1}", lines.get(0)
                .length(), lines.get(0)));
        } else if (lines.get(1).length() != 69) {
            throw new IOException(MessageFormat.format("TLE Line 2 is not 69 characters long ({0}): {1}", lines.get(1)
                .length(), lines.get(1)));
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

        if (config != null) {
            tle.setUPDATE_INTERVAL_MSEC(config.getSensorUpdateIntervalMsec());
        } else {
            tle.setUPDATE_INTERVAL_MSEC(BigInteger.valueOf(500));
        }
        tle.setTLE_1(l1);
        tle.setTLE_2(l2);
        return tle;
    }

    /**
     * Checks if a given VectorF3D has the euclidian length 1 (is a unit vector).
     *
     * @param vec The vector to be checked.
     * @return True iff length is 1.
     */
    private boolean isUnity(final VectorF3D vec) {
        final double x2 = (double) vec.getX() * (double) vec.getX();
        final double y2 = (double) vec.getY() * (double) vec.getY();
        final double z2 = (double) vec.getZ() * (double) vec.getZ();
        return Math.abs(Math.sqrt(x2 + y2 + z2) - 1.0) < 0.001;
    }

    private void dumpStandardTelemetry() {
        SEPP_IADCS_API_STANDARD_TELEMETRY stdTM;
        LOGGER.log(Level.INFO, "Dumping Standard Telemetry...");
        synchronized (this) {
            stdTM = adcsApi.Get_Standard_Telemetry();
        }
        SEPP_IADCS_API_VECTOR3_XYZ_UINT singleAxisStatus = stdTM.getCONTROL_SINGLE_AXIS_STATUS();
        Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO, String.format("Standard TM:\n" +
            "SYSTEM_TIME_MSEC = %d\n" + "EPOCH_TIME_MSEC = %d\n" + "LIVELYHOOD_REGISTER = %d\n" +
            "SYSTEM_STATUS_REGISTER = %d\n" + "SYSTEM_SCHEDULER_REGISTER = %d\n" + "SYSTEM_ERROR_REGISTER = %d\n" +
            "SENSORS_ERROR_REGISTER = %d\n" + "ACTUATORS_ERROR_REGISTER = %d\n" + "CONTROL_MAIN_STATUS = %d\n" +
            "CONTROL_MAIN_ERROR = %d\n" + "CONTROL_SINGLE_AXIS_STATUS (X,Y,Z) = (%d,%d,%d)\n" +
            "CONTROL_ALL_AXIS_STATUS = %d\n" + "SAT_MAIN_REGISTER = %d\n" + "SAT_ERROR_REGISTER = %d\n" +
            "SAT_SCHEDULER_REGISTER = %d\n" + "NUMBER_OF_RECEIVED_COMMANDS = %d\n" + "NUMBER_OF_FAILED_COMMANDS = %d\n",
            stdTM.getSYSTEM_TIME_MSEC(), stdTM.getEPOCH_TIME_MSEC(), stdTM.getLIVELYHOOD_REGISTER(), stdTM
                .getSYSTEM_STATUS_REGISTER(), stdTM.getSYSTEM_SCHEDULER_REGISTER(), stdTM.getSYSTEM_ERROR_REGISTER(),
            stdTM.getSENSORS_ERROR_REGISTER(), stdTM.getACTUATORS_ERROR_REGISTER(), stdTM.getCONTROL_MAIN_STATUS(),
            stdTM.getCONTROL_MAIN_ERROR(), singleAxisStatus.getX(), singleAxisStatus.getY(), singleAxisStatus.getZ(),
            stdTM.getCONTROL_ALL_AXIS_STATUS(), stdTM.getSAT_MAIN_REGISTER(), stdTM.getSAT_ERROR_REGISTER(), stdTM
                .getSAT_SCHEDULER_REGISTER(), stdTM.getNUMBER_OF_RECEIVED_COMMANDS(), stdTM
                    .getNUMBER_OF_FAILED_COMMANDS()));
    }

    private void dumpInfoTelemetry() {
        SEPP_IADCS_API_INFO_TELEMETRY infoTM;
        LOGGER.log(Level.INFO, "Dumping Info Telemetry...");
        synchronized (this) {
            infoTM = adcsApi.Get_Info_Telemetry();
        }

        SEPP_IADCS_API_SW_VERSION swVer = infoTM.getSW_VERSION();
        SEPP_IADCS_API_COMMIT_ID commitId = infoTM.getSW_COMMIT_ID();
        Logger.getLogger(AutonomousADCSOPSSATAdapter.class.getName()).log(Level.INFO, String.format("Info TM:\n" +
            "FRAME_IDENTIFIER = %s\n" + "FRAME_VERSION = %d\n" + "SW_VERSION = %d.%d.%d\n" + "STARTRACKER_TYPE = %d\n" +
            "STARTRACKER_SERIAL_NUMBER = %d\n" + "DEVICE_NAME = %s\n" + "DEVICE_SERIAL_NUMBER = %d\n" +
            "BUILD_TIMESTAMP = %s\n" + "SW_COMMIT_ID = project %d, library %d\n" + "DEBUG_LEVEL = %d\n" +
            "COMPILER_NAME = %s\n" + "COMPILER_VERSION = %s\n" + "LOW_LEVEL_SW_VERSION = %s\n" +
            "LOW_LEVEL_BUILD_TIMESTAMP = %S\n", infoTM.getFRAME_IDENTIFIER(), infoTM.getFRAME_VERSION(), swVer
                .getMAJOR(), swVer.getMINOR(), swVer.getPATCH(), infoTM.getSTARTRACKER_TYPE(), infoTM
                    .getSTARTRACKER_SERIAL_NUMBER(), infoTM.getDEVICE_NAME(), infoTM.getDEVICE_SERIAL_NUMBER(), infoTM
                        .getBUILD_TIMESTAMP(), commitId.getPROJECT(), commitId.getLIBRARY(), infoTM.getDEBUG_LEVEL(),
            infoTM.getCOMPILER_NAME(), infoTM.getCOMPILER_VERSION(), infoTM.getLOW_LEVEL_SW_VERSION(), infoTM
                .getLOW_LEVEL_BUILD_TIMESTAMP()));
    }

    private void dumpPowerTelemetry() {
        SEPP_IADCS_API_POWER_STATUS_TELEMETRY powerTM;
        LOGGER.log(Level.INFO, "Dumping Power Telemetry...");
        synchronized (this) {
            powerTM = adcsApi.Get_Power_Status_Telemetry();
        }
        LOGGER.log(Level.INFO, String.format("Power TM:\n" + " MAGNETTORQUER_POWER_CONSUMPTION_W = %.3f\n" +
            " MAGNETTORQUER_SUPPLY_VOLTAGE_V = %.3f\n" + " MAGNETTORQUER_CURRENT_CONSUMPTION_A = %.3f\n" +
            " STARTRACKER_POWER_CONSUMPTION_W = %.3f\n" + " STARTRACKER_SUPPLY_VOLTAGE_V = %.3f\n" +
            " STARTRACKER_CURRENT_CONSUMPTION_A = %.3f\n" + " IADCS_POWER_CONSUMPTION_W = %.3f\n" +
            " IADCS_SUPPLY_VOLTAGE_V = %.3f\n" + " IADCS_CURRENT_CONSUMPTION_A = %.3f\n" +
            " REACTIONWHEEL_POWER_CONSUMPTION_W = %.3f\n" + " REACTIONWHEEL_SUPPLY_VOLTAGE_V = %.3f\n" +
            " REACTIONWHEEL_CURRENT_CONSUMPTION_A = %.3f\n", powerTM.getMAGNETTORQUER_POWER_CONSUMPTION_W(), powerTM
                .getMAGNETTORQUER_SUPPLY_VOLTAGE_V(), powerTM.getMAGNETTORQUER_CURRENT_CONSUMPTION_A(), powerTM
                    .getSTARTRACKER_POWER_CONSUMPTION_W(), powerTM.getSTARTRACKER_SUPPLY_VOLTAGE_V(), powerTM
                        .getSTARTRACKER_CURRENT_CONSUMPTION_A(), powerTM.getIADCS_POWER_CONSUMPTION_W(), powerTM
                            .getIADCS_SUPPLY_VOLTAGE_V(), powerTM.getIADCS_CURRENT_CONSUMPTION_A(), powerTM
                                .getREACTIONWHEEL_POWER_CONSUMPTION_W(), powerTM.getREACTIONWHEEL_SUPPLY_VOLTAGE_V(),
            powerTM.getREACTIONWHEEL_CURRENT_CONSUMPTION_A()));
    }

    @Override
    public void setDesiredAttitude(final AttitudeMode attitude) throws IOException, UnsupportedOperationException {
        LOGGER.log(Level.INFO, "Setting desired attitude: {0}: {1}", new Object[]{attitude.getClass().getSimpleName(),
                                                                                  attitude});
        synchronized (this) {
            if (attitude instanceof AttitudeModeBDot) {
                final AttitudeModeBDot bDot = (AttitudeModeBDot) attitude;
                gotoBDot(bDot);
            } else if (attitude instanceof AttitudeModeNadirPointing) {
                final AttitudeModeNadirPointing nadirPointing = (AttitudeModeNadirPointing) attitude;
                gotoNadirPointing(nadirPointing);
            } else if (attitude instanceof AttitudeModeSingleSpinning) {
                final AttitudeModeSingleSpinning singleSpin = (AttitudeModeSingleSpinning) attitude;
                gotoSingleSpin(singleSpin);
            } else if (attitude instanceof AttitudeModeSunPointing) {
                final AttitudeModeSunPointing sunPointing = (AttitudeModeSunPointing) attitude;
                gotoSunPointing(sunPointing);
            } else if (attitude instanceof AttitudeModeTargetTracking) {
                final AttitudeModeTargetTracking targetTracking = (AttitudeModeTargetTracking) attitude;
                gotoTargetTracking(targetTracking);
            } else if (attitude instanceof AttitudeModeTargetTrackingLinear) {
                final AttitudeModeTargetTrackingLinear targetLinearTracking = (AttitudeModeTargetTrackingLinear) attitude;
                gotoTargetLinearTracking(targetLinearTracking);
            } else if (attitude instanceof AttitudeModeVectorPointing) {
                final AttitudeModeVectorPointing a = (AttitudeModeVectorPointing) attitude;
                gotoVectorPointing(a);
            } else {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }
    }

    private void gotoVectorPointing(final AttitudeModeVectorPointing a) {
        holder = new PositionHolder(new Vector3D(a.getTarget().getX(), a.getTarget().getY(), a.getTarget().getZ()), a
            .getMargin());
        final Thread runner = new Thread(holder, "iADCS Vector pointing holder");
        runner.start();
    }

    private void gotoTargetLinearTracking(final AttitudeModeTargetTrackingLinear targetLinearTracking)
        throws IOException {
        TargetPointingConfig config = new TargetPointingConfig();
        config.load();
        prepareForPointingMode(config);
        final SEPP_IADCS_API_TARGET_POINTING_CONST_VELOCITY_MODE_PARAMETERS params = config.getConstVelModeParams();
        params.setSTART_LATITUDE_RAD((float) FastMath.toRadians(targetLinearTracking.getLatitudeStart()));
        params.setSTART_LONGITUDE_RAD((float) FastMath.toRadians(targetLinearTracking.getLongitudeStart()));
        params.setSTOP_LATITUDE_RAD((float) FastMath.toRadians(targetLinearTracking.getLatitudeEnd()));
        params.setSTOP_LONGITUDE_RAD((float) FastMath.toRadians(targetLinearTracking.getLongitudeEnd()));
        LOGGER.fine("Start target pointing linear mode");
        adcsApi.Start_Target_Pointing_Earth_Const_Velocity_Mode(params);
        LOGGER.fine("Set System Scheduler Register");
        adcsApi.Set_System_Scheduler_Register(config.getSystemSchedulerRegister());
        activeAttitudeMode = targetLinearTracking;
    }

    private void gotoTargetTracking(final AttitudeModeTargetTracking targetTracking) throws IOException {
        TargetPointingConfig config = new TargetPointingConfig();
        config.load();
        prepareForPointingMode(config);
        final SEPP_IADCS_API_TARGET_POINTING_FIXED_MODE_PARAMETERS params = config.getFixedModeParams();
        params.setTARGET_LATITUDE_RAD((float) FastMath.toRadians(targetTracking.getLatitude()));
        params.setTARGET_LONGITUDE_RAD((float) FastMath.toRadians(targetTracking.getLongitude()));
        LOGGER.fine("Start target pointing fixed mode");
        adcsApi.Start_Target_Pointing_Earth_Fix_Mode(params);
        LOGGER.fine("Set System Scheduler Register");
        adcsApi.Set_System_Scheduler_Register(config.getSystemSchedulerRegister());
        activeAttitudeMode = targetTracking;
    }

    private void gotoSunPointing(final AttitudeModeSunPointing sunPointing) throws IOException {
        SunPointingConfig config = new SunPointingConfig();
        config.load();
        prepareForPointingMode(config);
        final SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS params = config.getModeParams();
        adcsApi.Start_Operation_Mode_Sun_Pointing(params);
        LOGGER.fine("Set System Scheduler Register");
        adcsApi.Set_System_Scheduler_Register(config.getSystemSchedulerRegister());
        activeAttitudeMode = sunPointing;
    }

    private void gotoSingleSpin(final AttitudeModeSingleSpinning singleSpin) {
        final VectorF3D target = singleSpin.getBodyAxis();
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
        adcsApi.Start_SingleAxis_AngularVelocity_Controller(vec, singleSpin.getAngularVelocity());
        activeAttitudeMode = singleSpin;
    }

    private void gotoNadirPointing(final AttitudeModeNadirPointing nadirPointing) throws IOException {
        NadirPointingConfig config = new NadirPointingConfig();
        config.load();
        prepareForPointingMode(config);
        LOGGER.fine("Set the target pointing operation parameters");
        adcsApi.Set_Target_Pointing_Operation_Parameters(config.getTargetPointingOperationParams());
        SEPP_IADCS_API_TARGET_POINTING_NADIR_MODE_PARAMETERS params = config.getModeParams();
        // TODO - expose some of the params with the next version of the API
        LOGGER.fine("Start target pointing nadir mode");
        adcsApi.Start_Target_Pointing_Nadir_Mode(params);
        LOGGER.fine("Set System Scheduler Register");
        adcsApi.Set_System_Scheduler_Register(config.getSystemSchedulerRegister());
        activeAttitudeMode = nadirPointing;
    }

    private void gotoBDot(final AttitudeModeBDot a) {
        final SEPP_IADCS_API_DETUMBLING_MODE_PARAMETERS params = new SEPP_IADCS_API_DETUMBLING_MODE_PARAMETERS();
        adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.nanoTime()/1000000)); //@TODO ok here ?
        params.setSTART_EPOCH_TIME_MSEC(BigInteger.valueOf(0));
        params.setSTOP_EPOCH_TIME_MSEC(BigInteger.valueOf(Long.MAX_VALUE));
        adcsApi.Start_Operation_Mode_Detumbling(params);
        activeAttitudeMode = a;
    }

    private void prepareForPointingMode(BasePointingConfig config) throws IOException {
        LOGGER.fine("Set Epoch Time");
        adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.nanoTime()/1000000)); //@TODO ok here ?
        if (config.getEnableGyroBiasCorrection()) {
            LOGGER.fine("Set Sensor Zero Bias Values - Gyroscope");
            adcsApi.Set_Gyro_Bias_Value(SEPP_IADCS_API_GYROSCOPES.IADCS_EXTERNAL_HIGHPERFORMANCE_GYRO, config
                .getGyroBiasVector());
            adcsApi.Enable_Gyro_Bias_Removement(SEPP_IADCS_API_GYROSCOPES.IADCS_EXTERNAL_HIGHPERFORMANCE_GYRO);
        } else {
            LOGGER.fine("Skip Bias Correction for Gyroscope (use defaults)");
        }
        LOGGER.fine("Init Orbit Module");
        adcsApi.Init_Orbit_Module(readTLEFile(config));
        LOGGER.fine("Set Kalman Filter Parameters");
        adcsApi.Set_Kalman_Filter_Parameters(config.getKfParams());
        if (config.getSlidingControllerConfigEnable()) {
            LOGGER.fine("Set Sliding Controller Parameters");
            adcsApi.Set_ThreeAxis_Sliding_Controller_Parameters(config.getScConfig());
        } else {
            LOGGER.fine("Skip setting sliding controller parameters (use defaults)");
        }
    }

    @Override
    public void setAllReactionWheelSpeeds(final float wheelX, final float wheelY, final float wheelZ,
        final float wheelU, final float wheelV, final float wheelW) {
        synchronized (this) {
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
    public void setReactionWheelSpeed(final ReactionWheelIdentifier wheel, final float Speed) {
        synchronized (this) {
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
    public void setAllReactionWheelParameters(final ReactionWheelParameters parameters) {
        synchronized (this) {
            final ReactionWheelParameters oldParams = getAllReactionWheelParameters();
            final SEPP_IADCS_API_REACTIONWHEEL_ARRAY_PARAMETERS params = new SEPP_IADCS_API_REACTIONWHEEL_ARRAY_PARAMETERS();

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
                    LOGGER.log(Level.WARNING, "Negative maximum speed is not allowed! Max speed will not be changed");
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
                    LOGGER.log(Level.WARNING, "Negative maximum torque is not allowed! Max torque will not be changed");
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
    public void setAllMagnetorquersDipoleMoments(final Float dipoleX, final Float dipoleY, final Float dipoleZ) {
        synchronized (this) {
            final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT moments = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();

            moments.setX(dipoleX);
            moments.setY(dipoleY);
            moments.setZ(dipoleZ);

            adcsApi.Set_Magnettorquer_All_Dipole_Moments(moments);
        }
    }

    @Override
    public ReactionWheelParameters getAllReactionWheelParameters() {
        synchronized (this) {
            final SEPP_IADCS_API_REACTIONWHEEL_ARRAY_PARAMETERS param = adcsApi.Get_ReactionWheel_All_Parameters();
            return new ReactionWheelParameters((int) param.getCONTROL_MODE(), param.getMAX_SPEED_RADPS(), param
                .getMAX_TORQUE_NM(), param.getMOMENT_OF_INERTIA_KGM2(), param.getMOTOR_CONSTANT());
        }
    }

    @Override
    public void unset() throws IOException {
        synchronized (this) {
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
            adcsApi.Set_Epoch_Time(BigInteger.valueOf(System.nanoTime()/1000000)); //@TODO ok here?
            adcsApi.Set_Operation_Mode_Measurement();
            adcsApi.Init_Orbit_Module(readTLEFile(null));
            enableOrbitPropagator();
        }
    }

    private void enableOrbitPropagator() {
        synchronized (this) {
            SEPP_IADCS_API_SYSTEM_SCHEDULER_REGISTER reg = adcsApi.Get_System_Scheduler_Register();
            reg.setORBIT_PROPAGATION_ENABLE(true);
            adcsApi.Set_System_Scheduler_Register(reg);
        }
    }

    private boolean isUnitAvailableInternal() {
        return apiLoaded && pcAdapter.isDeviceEnabled(DeviceType.ADCS);
    }

    @Override
    public boolean isUnitAvailable() {
        return isUnitAvailableInternal() && unitInitialized;
    }

    @Override
    public AttitudeTelemetry getAttitudeTelemetry() throws IOException {
        synchronized (this) {
            boolean stateTarget = true;
            stateTarget = adcsApi.Get_Target_Pointing_Operation_Data_Telemetry().getSTATE_TARGET() == (short) 1;
            final SEPP_IADCS_API_ATTITUDE_TELEMETRY attitudeTm = adcsApi.Get_Attitude_Telemetry();
            attitudeTm.getATTITUDE_QUATERNION_BF();
            final Quaternion attitude = IADCSTools.convertFromApiQuaternion(attitudeTm.getATTITUDE_QUATERNION_BF());
            final VectorF3D angularVel = IADCSTools.convertFromApiVector(attitudeTm.getANGULAR_VELOCITY_VECTOR_RADPS());
            final VectorF3D magneticField = IADCSTools.convertFromApiVector(attitudeTm
                .getMEASURED_MAGNETIC_FIELD_VECTOR_BF_T());
            final VectorF3D sunVector = IADCSTools.convertFromApiVector(attitudeTm.getMEASURED_SUN_VECTOR_BF());
            return new AttitudeTelemetry(attitude, angularVel, sunVector, magneticField, stateTarget);
        }
    }

    @Override
    public ActuatorsTelemetry getActuatorsTelemetry() throws IOException {
        synchronized (this) {
            final SEPP_IADCS_API_ACTUATOR_TELEMETRY actuatorTm = adcsApi.Get_Actuator_Telemetry();
            final WheelsSpeed targetSpeed = IADCSTools.convertFromApiWheelSpeed(actuatorTm
                .getREACTIONWHEEL_TARGET_SPEED_VECTOR_XYZ_RADPS(), actuatorTm
                    .getREACTIONWHEEL_TARGET_SPEED_VECTOR_UVW_RADPS());
            final WheelsSpeed currentSpeed = IADCSTools.convertFromApiWheelSpeed(actuatorTm
                .getREACTIONWHEEL_CURRENT_SPEED_VECTOR_XYZ_RADPS(), actuatorTm
                    .getREACTIONWHEEL_CURRENT_SPEED_VECTOR_UVW_RADPS());
            final VectorF3D mtqDipoleMoment = IADCSTools.convertFromApiMagMoment(actuatorTm
                .getMAGNETORQUERS_TARGET_DIPOLE_MOMENT_VECTOR_AM2());
            final MagnetorquersState mtqState = IADCSTools.convertApiMtqState(actuatorTm
                .getMAGNETORQUERS_CURRENT_STATE());
            return new ActuatorsTelemetry(targetSpeed, currentSpeed, mtqDipoleMoment, mtqState);
        }
    }

    @Override
    public String validateAttitudeDescriptor(final AttitudeMode attitude) {
        // TODO do some rudimentary checks (i.e. if the angles make sense)
        return null;
    }

    @Override
    public AttitudeMode getActiveAttitudeMode() {
        return activeAttitudeMode;
    }
}
