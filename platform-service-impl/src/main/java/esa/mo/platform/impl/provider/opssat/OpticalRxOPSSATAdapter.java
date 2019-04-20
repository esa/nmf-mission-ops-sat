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

import esa.mo.platform.impl.provider.gen.OpticalDataReceiverAdapterInterface;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.structures.Duration;
import at.tugraz.ihf.opssat.opt_rx.SEPP_OPT_RX_API;

public class OpticalRxOPSSATAdapter implements OpticalDataReceiverAdapterInterface
{

  private static Logger LOGGER = Logger.getLogger(OpticalRxOPSSATAdapter.class.getName());
  private SEPP_OPT_RX_API optRxApi;
  private boolean initalized = false;

  public OpticalRxOPSSATAdapter()
  {
    LOGGER.log(Level.INFO, "Initialisation");
    try {
      System.loadLibrary("opt_rx_api_jni");
      optRxApi = new SEPP_OPT_RX_API();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE,
          "OPT RX API could not be initialized!", ex);
      initalized = false;
      return;
    }
    initalized = true;

  }

  @Override
  public byte[] recordOpticalReceiverData(Duration duration)
  {
    if (duration == null) {
      return null;
    }
    if (optRxApi == null) {
      return null;
    }
    LOGGER.log(Level.INFO, "Recording optical data for {0}s", duration);
    optRxApi.Clear_SharedMemory_Data_Buffer();
    optRxApi.Enable_RX_Detector();
    try {
      Thread.sleep((long) (duration.getValue() * 1000));
    } catch (InterruptedException e) {
      return null;
    }
    // TODO capture data from the API
    return new byte[0];
  }

  @Override
  public boolean isUnitAvailable()
  {
    return initalized;
  }

}
