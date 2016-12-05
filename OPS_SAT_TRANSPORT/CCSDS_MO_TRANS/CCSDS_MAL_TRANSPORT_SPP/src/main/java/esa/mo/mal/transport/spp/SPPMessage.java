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

import esa.mo.mal.encoder.spp.SPPFixedBinaryElementOutputStream;
import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.GENMessageHeader;
import static esa.mo.mal.transport.spp.SPPBaseTransport.LOGGER;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Level;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.MALOperation;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;

/**
 * SPP message class.
 */
public class SPPMessage extends GENMessage
{
  private final MALElementStreamFactory hdrStreamFactory;
  private final SPPConfiguration configuration;
  private final SPPSegmentCounter segmentCounter;

  /**
   * Constructor.
   *
   * @param configuration The SPP configuration to use for this message.
   * @param wrapBodyParts True if the encoded body parts should be wrapped in BLOBs.
   * @param header The message header to use.
   * @param qosProperties The QoS properties for this message.
   * @param operation The details of the operation being encoding, can be null.
   * @param body the body of the message.
   * @throws org.ccsds.moims.mo.mal.MALInteractionException If the operation is unknown.
   */
  public SPPMessage(final MALElementStreamFactory hdrStreamFactory,
          final SPPConfiguration configuration,
          final SPPSegmentCounter segmentCounter,
          boolean wrapBodyParts, GENMessageHeader header, Map qosProperties, MALOperation operation,
          MALElementStreamFactory encFactory, Object... body) throws MALInteractionException
  {
    super(wrapBodyParts, header, qosProperties, operation, encFactory, body);

    this.hdrStreamFactory = hdrStreamFactory;
    this.configuration = configuration;
    this.segmentCounter = segmentCounter;
  }

  /**
   * Constructor.
   *
   * @param configuration The SPP configuration to use for this message.
   * @param wrapBodyParts True if the encoded body parts should be wrapped in BLOBs.
   * @param readHeader True if the header should be read from the packet.
   * @param header An instance of the header class to use.
   * @param qosProperties The QoS properties for this message.
   * @param packet The message in encoded form.
   * @param encFactory The stream factory to use for decoding.
   * @throws MALException On decoding error.
   */
  public SPPMessage(final MALElementStreamFactory hdrStreamFactory,
          final SPPConfiguration configuration,
          final SPPSegmentCounter segmentCounter,
          boolean wrapBodyParts, boolean readHeader, GENMessageHeader header, Map qosProperties, byte[] packet, MALElementStreamFactory encFactory) throws MALException
  {
    super(wrapBodyParts, readHeader, header, qosProperties, packet, encFactory);

    this.hdrStreamFactory = hdrStreamFactory;
    this.configuration = configuration;
    this.segmentCounter = segmentCounter;
  }

  /**
   * Constructor.
   *
   * @param configuration The SPP configuration to use for this message.
   * @param wrapBodyParts True if the encoded body parts should be wrapped in BLOBs.
   * @param readHeader True if the header should be read from the stream.
   * @param header An instance of the header class to use.
   * @param qosProperties The QoS properties for this message.
   * @param ios The message in encoded form.
   * @param encFactory The stream factory to use for decoding.
   * @throws MALException On decoding error.
   */
  public SPPMessage(final MALElementStreamFactory hdrStreamFactory,
          final SPPConfiguration configuration,
          final SPPSegmentCounter segmentCounter,
          boolean wrapBodyParts, boolean readHeader, GENMessageHeader header, Map qosProperties, InputStream ios, MALElementStreamFactory encFactory) throws MALException
  {
    super(wrapBodyParts, readHeader, header, qosProperties, ios, encFactory);

    this.hdrStreamFactory = hdrStreamFactory;
    this.configuration = configuration;
    this.segmentCounter = segmentCounter;
  }

  @Override
  public void encodeMessage(final MALElementStreamFactory streamFactory,
          final MALElementOutputStream enc,
          final OutputStream lowLevelOutputStream,
          final boolean writeHeader) throws MALException
  {
    try
    {
      MALElementStreamFactory localBodyStreamFactory = hdrStreamFactory;
      if (!configuration.isFixedBody())
      {
        localBodyStreamFactory = streamFactory;
      }

      final ByteArrayOutputStream hdrBaos = new ByteArrayOutputStream();
      SPPFixedBinaryElementOutputStream hdrEnc = (SPPFixedBinaryElementOutputStream) hdrStreamFactory.createOutputStream(hdrBaos);
      final ByteArrayOutputStream bodyBaos = new ByteArrayOutputStream();
      final MALElementOutputStream bodyEnc = localBodyStreamFactory.createOutputStream(bodyBaos);

      super.encodeMessage(localBodyStreamFactory, bodyEnc, bodyBaos, false);

      MALEncodingContext ctx = new MALEncodingContext(header, operation, 0, qosProperties, qosProperties);
      hdrEnc.writeElement(header, ctx);
      byte[] hdrBuf = hdrBaos.toByteArray();
      byte[] bodyBuf = bodyBaos.toByteArray();

      LOGGER.log(Level.FINE, "Check segmenting: Segment size is {0} and required length is {1}", new Object[]
      {
        configuration.getSegmentSize(), bodyBuf.length + hdrBuf.length - 6
      });
      if ((bodyBuf.length + hdrBuf.length - 6) > configuration.getSegmentSize())
      {
        // encode segmented header
        ((SPPMessageHeader) header).setSegmentFlags((byte) 0x40);
        hdrBaos.reset();
        hdrEnc = (SPPFixedBinaryElementOutputStream) hdrStreamFactory.createOutputStream(hdrBaos);
        hdrEnc.writeElement(header, ctx);
        hdrBuf = hdrBaos.toByteArray();

        final int adjustedSegmentSize = configuration.getSegmentSize() - (hdrBuf.length - 6);
        // first check to see if we can actually fit any data in the body when we have a large header and small segment size
        if (0 >= adjustedSegmentSize)
        {
          throw new MALException("SPP Segment size of " + configuration.getSegmentSize() + " is too small for encoded MAL Message header or size " + (hdrBuf.length - 6),
                  new MALInteractionException(new MALStandardError(MALHelper.INTERNAL_ERROR_NUMBER, null)));
        }

        // segment data
        ByteBuffer hdrBytes = ByteBuffer.wrap(hdrBuf);
        int index = 0;
        int extra = (hdrBuf[26] & 0x80) != 0 ? 1 : 0;
        extra += (hdrBuf[26] & 0x40) != 0 ? 1 : 0;
        boolean first = true;

        SPPSegmentCounter localSegmentCounter = new SPPSegmentCounter();
        
        while (index < bodyBuf.length)
        {
          int packetSize = Math.min(adjustedSegmentSize, bodyBuf.length - index);

          if (first)
          {
            first = false;
          }
          //else if (packetSize < adjustedSegmentSize)
          else if ((index + packetSize) >= bodyBuf.length)
          {
            hdrBuf[2] = (byte) ((hdrBuf[2] & 0x3F) | 0x80);
          }
          else
          {
            hdrBuf[2] = (byte) ((hdrBuf[2] & 0x3F));
          }

          // increment the SSC
          hdrBytes.putShort(4, (short) (packetSize + hdrBuf.length - 7));
          int count = localSegmentCounter.getNextSegmentCount();
          hdrBytes.putInt(27 + extra, count);

          LOGGER.log(Level.FINE, "Segment: {0} : {1} : {2} : {3}", new Object[]
          {
            hdrBuf[2] & 0xC0, packetSize + hdrBuf.length - 7, count, index
          });
          lowLevelOutputStream.write(hdrBuf);
          lowLevelOutputStream.write(bodyBuf, index, packetSize);
          index += packetSize;
        }
      }
      else
      {
        java.nio.ByteBuffer.wrap(hdrBuf).putShort(4, (short) (bodyBuf.length + hdrBuf.length - 7));
        lowLevelOutputStream.write(hdrBuf);
        lowLevelOutputStream.write(bodyBuf);
      }
    }
    catch (IOException ex)
    {
      throw new MALException("Internal error encoding message", ex);
    }
  }
}
