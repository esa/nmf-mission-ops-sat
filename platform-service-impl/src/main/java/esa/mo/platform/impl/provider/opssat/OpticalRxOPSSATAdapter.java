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

public class OpticalRxOPSSATAdapter implements OpticalDataReceiverAdapterInterface
{

  public OpticalRxOPSSATAdapter()
  {
    Logger.getLogger(OpticalRxOPSSATAdapter.class.getName()).log(Level.INFO, "Initialisation");
    System.loadLibrary("opt_rx_api_jni");
  }

  @Override
  public byte[] recordOpticalReceiverData(Duration duration)
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
