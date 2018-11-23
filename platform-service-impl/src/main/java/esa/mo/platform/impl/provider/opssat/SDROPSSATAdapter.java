/* ----------------------------------------------------------------------------
 * Copyright (C) 2018      European Space Agency
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

import esa.mo.platform.impl.provider.gen.SoftwareDefinedRadioAdapterInterface;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.platform.softwaredefinedradio.structures.IQComponentsList;
import org.ccsds.moims.mo.platform.softwaredefinedradio.structures.SDRConfiguration;
import at.tugraz.ihf.opssat.sdr.SEPP_SDR_API;

public class SDROPSSATAdapter implements SoftwareDefinedRadioAdapterInterface
{

  private SEPP_SDR_API sdrApi;

  public SDROPSSATAdapter()
  {
    Logger.getLogger(SDROPSSATAdapter.class.getName()).log(Level.INFO, "Initialisation");
    System.loadLibrary("sdr_api_jni");
    sdrApi = new SEPP_SDR_API();
    sdrApi.Print_Info();
  }

  @Override
  public boolean isUnitAvailable()
  {
    return false;
  }

  @Override
  public boolean setConfiguration(SDRConfiguration configuration)
  {
    //sdrApi.Set_RX_Gain_in_dB(
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean enableSDR(Boolean enable)
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public IQComponentsList getIQComponents()
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
