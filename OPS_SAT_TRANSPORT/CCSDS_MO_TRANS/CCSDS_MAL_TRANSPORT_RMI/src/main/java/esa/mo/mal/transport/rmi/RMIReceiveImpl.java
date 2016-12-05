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

import esa.mo.mal.transport.gen.receivers.GENIncomingByteMessageDecoderFactory.GENIncomingByteMessageDecoder;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * The implementation of the RMIReceiveInterface interface. Holds a reference to the transport instance that created it.
 */
public class RMIReceiveImpl extends UnicastRemoteObject implements RMIReceiveInterface
{
  private static final long serialVersionUID = 0x1000001111100L;
  private final transient RMITransport transport;

  /**
   * Creates a new instance of RMIRecvImpl
   *
   * @param transport The transport instance to pass received messages to.
   * @throws RemoteException On error.
   */
  public RMIReceiveImpl(final RMITransport transport) throws RemoteException
  {
    this.transport = transport;
  }

  @Override
  public void receive(final byte[] packet) throws RemoteException
  {
    transport.receive(null, new GENIncomingByteMessageDecoder(transport, packet));
  }
}
