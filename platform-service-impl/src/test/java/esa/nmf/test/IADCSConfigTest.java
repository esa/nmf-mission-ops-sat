package esa.nmf.test;

import org.junit.FixMethodOrder;
import org.junit.Test;
import java.io.IOException;
import org.junit.runners.MethodSorters;
import esa.mo.platform.impl.provider.opssat.iadcs.NadirPointingConfig;
import esa.mo.platform.impl.provider.opssat.iadcs.SunPointingConfig;
import esa.mo.platform.impl.provider.opssat.iadcs.TargetPointingConfig;
import esa.mo.platform.impl.provider.opssat.iadcs.BasePointingConfig;
import esa.mo.platform.impl.provider.opssat.iadcs.IADCSTools;
import org.junit.Assert;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IADCSConfigTest
{
  
  @Test
  public void testLoadNadir() throws Throwable
  {
    // NadirPointingConfig npc = new NadirPointingConfig();
    // npc.load("resources/tgt_ptg_nadir_cfg.ini", "TGT_PTG_NADIR");
    // npc.getTargetPointingOperationParams();
  }
  @Test
  public void testLoadInertial() throws Throwable
  {
  }
  @Test
  public void testLoadFixed() throws Throwable
  {
  }
  @Test
  public void testLoadSunPtg() throws Throwable
  {
  }
}