package esa.mo.platform.impl.provider.opssat.iadcs;

import java.math.BigInteger;

import at.tugraz.ihf.opssat.iadcs.SEPP_IADCS_API_TARGET_POINTING_CONST_VELOCITY_MODE_PARAMETERS;
import at.tugraz.ihf.opssat.iadcs.SEPP_IADCS_API_TARGET_POINTING_FIXED_MODE_PARAMETERS;

public class TargetPointingConfig extends BasePointingConfig {
    private static final String CONFIG_FILE_NAME = "tgt_ptg_fixed_cfg.ini";
    private static final String CONFIG_SECTION_NAME = "TGT_PTG_FIXED";

    public TargetPointingConfig() {
    }

    @Override
    protected String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

    @Override
    protected String getConfigSectionName() {
        return CONFIG_SECTION_NAME;
    }

    public SEPP_IADCS_API_TARGET_POINTING_FIXED_MODE_PARAMETERS getFixedModeParams() {
        SEPP_IADCS_API_TARGET_POINTING_FIXED_MODE_PARAMETERS ret = new SEPP_IADCS_API_TARGET_POINTING_FIXED_MODE_PARAMETERS();
        ret.setDETERMINATION_MODE(determinationMode);
        ret.setTOLERANCE_PARAMETERS(paTolerance);
        ret.setFLIGHT_VECTOR_BF(flightVector);
        ret.setLOS_VECTOR_BF(targetVector);
        ret.setOFFSET_TIME_MSEC(BigInteger.valueOf(0));
        // Target position provided externally
        return ret;
    }

    public SEPP_IADCS_API_TARGET_POINTING_CONST_VELOCITY_MODE_PARAMETERS getConstVelModeParams() {
        SEPP_IADCS_API_TARGET_POINTING_CONST_VELOCITY_MODE_PARAMETERS ret = new SEPP_IADCS_API_TARGET_POINTING_CONST_VELOCITY_MODE_PARAMETERS();
        ret.setDETERMINATION_MODE(determinationMode);
        ret.setTOLERANCE_PARAMETERS(paTolerance);
        ret.setFLIGHT_VECTOR_BF(flightVector);
        ret.setLOS_VECTOR_BF(targetVector);
        ret.setUPDATE_INTERVAL_MSEC(sensorUpdateIntervalMsec);
        ret.setOFFSET_TIME_MSEC(BigInteger.valueOf(0));
        ret.setSTART_EPOCH_TIME_MSEC(BigInteger.valueOf(0));
        ret.setSTOP_EPOCH_TIME_MSEC(BigInteger.valueOf(Long.MAX_VALUE));
        // Target position provided externally
        return ret;
    }
}