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
package esa.mo.mal.transport.gen;

import esa.mo.mal.transport.gen.sending.GENMessageSender;

/**
 * The GENReceptionHandler interface defines the methods needed for a receiver managing incoming data from a
 * communication channel.
 */
public interface GENReceptionHandler
{
  /**
   * Returns the remote URI for this reception handler.
   *
   * @return the remote URI that this reception handler receives data from
   */
  public String getRemoteURI();

  /**
   * Setter method for the remote URI of this handler
   *
   * @param newURI the remote root URI, i.e. tcpip://10.0.0.1:61617
   */
  public void setRemoteURI(String newURI);

  /**
   * Returns the message sender for this reception handler.
   *
   * @return the message sender for this receiver. Null if this receiver does not support sending messages
   */
  public GENMessageSender getMessageSender();

  /**
   * Closes the communication channel for this reception handler
   */
  public void close();
}
