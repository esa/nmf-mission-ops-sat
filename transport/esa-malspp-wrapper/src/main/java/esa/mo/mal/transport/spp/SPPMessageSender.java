/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO SPP Transport Framework
 * ----------------------------------------------------------------------------
 * Licensed under European Space Agency Public License (ESA-PL) Weak Copyleft – v2.4
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

import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.sending.GENOutgoingMessageHolder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import org.ccsds.moims.mo.testbed.util.spp.SPPSocket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacketHeader;

/**
 * This class implements the low level data (MAL Message) transport protocol. In order to differentiate messages with each other,
 * the protocol has a very simple format: |size|message|
 *
 * If the protocol uses a different message encoding this class can be replaced in the TCPIPTransport.
 *
 */
public class SPPMessageSender implements GENMessageSender<List<ByteBuffer>>
{
  protected final SPPSocket socket;
  private final SPPSourceSequenceCounterSimple sscGenerator = new SPPSourceSequenceCounterSimple();

  /**
   * Constructor.
   *
   * @param socket the TCPIP socket.
   */
  public SPPMessageSender(final SPPSocket socket)
  {
    this.socket = socket;
  }

  @Override
  public void sendEncodedMessage(final GENOutgoingMessageHolder<List<ByteBuffer>> packetData) throws IOException
  {
    // write packet
    final List<ByteBuffer> msgs = packetData.getEncodedMessage();

    final SPPMessage msg = (SPPMessage) packetData.getOriginalMessage();
    final SPPMessageHeader malhdr = (SPPMessageHeader) msg.getHeader();
    
//    int count = 0;
    for (final ByteBuffer buf : msgs)
    {
      final int sequenceFlags = (buf.get(buf.position() + 2) & 0xC0) >> 6;
      final short shortVal = buf.getShort(buf.position() + 4);
      int bodyLength = shortVal >= 0 ? shortVal : 0x10000 + shortVal;
      ++bodyLength;

      final SpacePacketHeader hdr = new SpacePacketHeader(0,
              0 == malhdr.getPacketType() ? 0 : 1,
              1, malhdr.getApid(), sequenceFlags, sscGenerator.getNextSourceSequenceCount());

      final SpacePacket pkt = new SpacePacket(hdr, malhdr.getApidQualifier(), buf.array(), buf.position() + 6, bodyLength);
      pkt.setQosProperties(packetData.getOriginalMessage().getQoSProperties());
      
      try
      {
        socket.send(pkt);
      }
      catch (final Exception ex)
      {
        ex.printStackTrace();
        throw new IOException("Unable to send SPP message", ex);
      }
    }
  }

  @Override
  public void close()
  {
  }
}
