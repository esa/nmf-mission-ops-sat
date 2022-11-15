package esa.mo.platform.impl.provider.opssat.iadcs;

import java.math.BigInteger;
import java.util.Map;

import at.tugraz.ihf.opssat.iadcs.SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS;

public class SunPointingConfig extends BasePointingConfig {
    private static final String CONFIG_FILE_NAME = "sun_ptg_cfg.ini";
    private static final String CONFIG_SECTION_NAME = "SUN_PTG";

    public SunPointingConfig() {
    }

    @Override
    protected String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

    @Override
    protected String getConfigSectionName() {
        return CONFIG_SECTION_NAME;
    }

    @Override
    protected void readFromMap(Map<String, String> map) {
        super.readFromMap(map);
        targetVector.setX(BasePointingConfig.strToFloat(map.get("tgt_vec_x_bf"), -1));
        targetVector.setY(BasePointingConfig.strToFloat(map.get("tgt_vec_y_bf"), 0));
        targetVector.setZ(BasePointingConfig.strToFloat(map.get("tgt_vec_z_bf"), 0));
    }

    public SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS getModeParams() {
        SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS ret = new SEPP_IADCS_API_SUN_POINTING_MODE_PARAMETERS();
        ret.setTARGET_VECTOR_BF(targetVector);
        ret.setSTART_EPOCH_TIME_MSEC(BigInteger.valueOf(0));
        ret.setSTOP_EPOCH_TIME_MSEC(BigInteger.valueOf(Long.MAX_VALUE));
        return ret;
    }
}