/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
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
package esa.mo.mal.transport.gen.receivers;

import org.ccsds.moims.mo.mal.MALException;

/**
 * Small interface that is used to decode a message from an incoming connection.s
 */
public interface GENIncomingMessageDecoder
{
  /**
   * Decodes and returns a new incoming message holder.
   *
   * @return The decoded incoming message.
   * @throws MALException On error.s
   */
  GENIncomingMessageHolder decodeAndCreateMessage() throws MALException;
}
