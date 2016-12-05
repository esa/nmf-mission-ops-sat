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

import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.sending.GENOutgoingMessageHolder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * implementation of the GENMessageSender information for RMI transport.
 */
public class RMIMessageSender implements GENMessageSender<byte[]>
{
  private final String remoteURI;
  private boolean closed = false;
  private RMIReceiveInterface destinationRMI;

  /**
   * Constructor.
   *
   * @param remoteRootURI The remote RMI object URI.
   * @throws NotBoundException If remote RMI object no bound.
   * @throws MalformedURLException If URI not correct.
   * @throws RemoteException If there is a remote error.
   */
  public RMIMessageSender(String remoteRootURI) throws NotBoundException, MalformedURLException, RemoteException
  {
    this.remoteURI = remoteRootURI;
    destinationRMI = (RMIReceiveInterface) Naming.lookup(remoteURI);
  }

  @Override
  public void sendEncodedMessage(GENOutgoingMessageHolder<byte[]> packetData) throws IOException
  {
    try
    {
      internalSendMessage(packetData);
    }
    catch (RemoteException ex)
    {
      // this can be thrown if we have a cached interface that has expired, so we clear the interface and try again
      destinationRMI = null;
      internalSendMessage(packetData);
    }
  }

  private void internalSendMessage(GENOutgoingMessageHolder<byte[]> packetData) throws MalformedURLException, RemoteException, IOException
  {
    if (!closed)
    {
      if (null == destinationRMI)
      {
        try
        {
          destinationRMI = (RMIReceiveInterface) Naming.lookup(remoteURI);
        }
        catch (NotBoundException ex)
        {
          throw new IOException("Remote URI no known " + remoteURI, ex);
        }
      }

      RMIReceiveInterface remoteIf = destinationRMI;

      if (null != remoteIf)
      {
        remoteIf.receive(packetData.getEncodedMessage());
      }
    }
  }

  @Override
  public void close()
  {
    closed = true;
    destinationRMI = null;
  }
}
