/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO SPP Transport Framework
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
package esa.mo.mal.transport.spp;

import java.io.IOException;
import org.ccsds.moims.mo.testbed.util.spp.SPPSocket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;

/**
 */
public class SPPMessageReceiver implements esa.mo.mal.transport.gen.util.GENMessagePoller.GENMessageReceiver<SpacePacket>
{
  protected final SPPSocket socket;

  /**
   * Constructor.
   *
   * @param socket the socket.
   */
  public SPPMessageReceiver(final SPPSocket socket)
  {
    this.socket = socket;
  }

  @Override
  public SpacePacket readEncodedMessage() throws IOException
  {
    try
    {
      final SpacePacket spacePacket = socket.receive();

      // PENDING: SPP TCP implementation allocates a new Space Packet with a body size of
      // 65536 bytes. If the received Space Packet is smaller, the body byte array is not
      // trimmed to fit. Here: Create new byte array of right size, copy contents, and set
      // array as new body of the Space Packet.
      final byte[] trimmedBody = new byte[spacePacket.getLength()];
      System.arraycopy(spacePacket.getBody(), 0, trimmedBody, 0, spacePacket.getLength());
      spacePacket.setBody(trimmedBody);

      return spacePacket;
    }
    catch (final Exception ex)
    {
      // socket has been closed to throw EOF exception higher
      throw new java.io.EOFException();
    }
  }

  @Override
  public void close()
  {
    try
    {
      socket.close();
    }
    catch (final Exception ex)
    {
      ex.printStackTrace();
    }
  }
}
