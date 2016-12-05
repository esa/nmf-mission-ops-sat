/* ----------------------------------------------------------------------------
 * Copyright (C) 2014      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO Generic Transport Framework
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
package esa.mo.mal.transport.gen.sending;

import java.io.IOException;

/**
 * Interface used to map to the low level transport specific send.
 */
public interface GENMessageSender<O>
{
  /**
   * Sends an encoded message to the client (MAL Message encoded as a byte array)
   *
   * @param encodedMessage the MALMessage
   * @throws IOException in case the message cannot be sent to the client
   */
  void sendEncodedMessage(GENOutgoingMessageHolder<O> encodedMessage) throws IOException;

  /**
   * Closes any resources connected to the low level interface.
   */
  void close();
}
