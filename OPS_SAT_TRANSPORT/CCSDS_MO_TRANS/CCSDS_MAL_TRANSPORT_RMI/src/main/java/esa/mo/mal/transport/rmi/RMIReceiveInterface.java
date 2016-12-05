/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO RMI Transport
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
package esa.mo.mal.transport.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The RMI interface. Defines a single method for the reception of an encoded message.
 */
public interface RMIReceiveInterface extends Remote
{
  /**
   * Used to pass an encoded message to a RMI Transport instance.
   *
   * @param message The encoded message.
   * @throws RemoteException On remote error.
   */
  void receive(final byte[] message) throws RemoteException;
}
