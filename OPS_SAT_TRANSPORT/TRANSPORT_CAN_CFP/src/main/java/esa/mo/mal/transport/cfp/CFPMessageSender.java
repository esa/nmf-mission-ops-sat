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
package esa.mo.mal.transport.cfp;

import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.sending.GENOutgoingMessageHolder;
import esa.mo.transport.can.opssat.CFPFrameHandler;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * implementation of the GENMessageSender information for CFP transport.
 */
public class CFPMessageSender implements GENMessageSender
{
  private boolean closed = false;
  private final CFPFrameHandler canHandler;

  /**
   * Constructor.
   *
     * @param canHandler Handler for CFP
   */
  public CFPMessageSender(CFPFrameHandler canHandler)
  {
    this.canHandler = canHandler;
  }

  @Override
  public void sendEncodedMessage(GENOutgoingMessageHolder packetData) throws IOException
  {
      if (!closed && null != canHandler)
      {
          try {
              canHandler.sendData(packetData.getEncodedMessage());
          } catch (InterruptedException ex) {
              Logger.getLogger(CFPMessageSender.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
  }

  @Override
  public void close()
  {
    closed = true;
  }

}