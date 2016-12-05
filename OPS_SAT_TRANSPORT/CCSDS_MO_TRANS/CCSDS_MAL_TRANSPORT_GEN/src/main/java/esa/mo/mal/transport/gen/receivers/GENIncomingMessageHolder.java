/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
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

import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.GENTransport;

/**
 * Simple structure class for holding related aspects of a decoded MAL message.
 */
public final class GENIncomingMessageHolder
{
  /**
   * The transaction id of this message.
   */
  public final Long transactionId;
  /**
   * The decoded MAL message.
   */
  public final GENMessage malMsg;
  /**
   * A string representation for debug tracing.
   */
  public final GENTransport.PacketToString smsg;

  /**
   * Constructor.
   *
   * @param transactionId the message transaction id.
   * @param malMsg The decoded MAL message.
   * @param smsg A string representation for debug tracing.
   */
  public GENIncomingMessageHolder(final Long transactionId, final GENMessage malMsg, final GENTransport.PacketToString smsg)
  {
    this.transactionId = transactionId;
    this.malMsg = malMsg;
    this.smsg = smsg;
  }
}
