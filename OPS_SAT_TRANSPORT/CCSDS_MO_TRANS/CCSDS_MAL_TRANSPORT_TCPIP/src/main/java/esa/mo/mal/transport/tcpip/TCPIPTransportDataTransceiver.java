/* ----------------------------------------------------------------------------
 * Copyright (C) 2014      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO TCP/IP Transport Framework
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
package esa.mo.mal.transport.tcpip;

import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.sending.GENOutgoingMessageHolder;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * This class implements the low level data (MAL Message) transport protocol. In order to differentiate messages with
 * each other, the protocol has a very simple format: |size|message|
 *
 * If the protocol uses a different message encoding this class can be replaced in the TCPIPTransport.
 *
 */
public class TCPIPTransportDataTransceiver implements esa.mo.mal.transport.gen.util.GENMessagePoller.GENMessageReceiver<byte[]>, GENMessageSender
{
  protected final Socket socket;
  protected final DataOutputStream socketWriteIf;
  protected final DataInputStream socketReadIf;

  /**
   * Constructor.
   *
   * @param socket the TCPIP socket.
   * @throws IOException if there is an error.
   */
  public TCPIPTransportDataTransceiver(Socket socket) throws IOException
  {
    this.socket = socket;
    socketWriteIf = new DataOutputStream(socket.getOutputStream());
    socketReadIf = new DataInputStream(socket.getInputStream());
  }

  @Override
  public void sendEncodedMessage(GENOutgoingMessageHolder packetData) throws IOException
  {
    // write packet length and then the packet
    socketWriteIf.writeInt(packetData.getEncodedMessage().length);

    socketWriteIf.write(packetData.getEncodedMessage());
    socketWriteIf.flush();
  }

  @Override
  public byte[] readEncodedMessage() throws IOException
  {
    try
    {
      // read packet length and then the packet
      int packetSize = socketReadIf.readInt();
      byte[] data = new byte[packetSize];
      socketReadIf.readFully(data);
      return data;
    }
    catch (java.net.SocketException ex)
    {
      if (socket.isClosed())
      {
        // socket has been closed to throw EOF exception higher
        throw new java.io.EOFException();
      }

      throw ex;
    }
  }

  @Override
  public void close()
  {
    try
    {
      socket.close();
    }
    catch (IOException e)
    {
      // ignore
    }
  }
}
