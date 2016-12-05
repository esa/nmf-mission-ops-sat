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

import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.GENReceptionHandler;
import esa.mo.mal.transport.gen.GENTransport;
import esa.mo.mal.transport.gen.GENTransport.PacketToString;
import esa.mo.mal.transport.gen.receivers.GENIncomingMessageDecoder;
import esa.mo.mal.transport.gen.receivers.GENIncomingMessageDecoderFactory;
import esa.mo.mal.transport.gen.receivers.GENIncomingMessageHolder;
import java.nio.ByteBuffer;
import java.util.List;
import org.ccsds.moims.mo.mal.MALException;

/**
 * Factory class for SPPMessage decoders.
 * @param <I>
 */
public class SPPMessageDecoderFactory<I> implements GENIncomingMessageDecoderFactory<I, List<ByteBuffer>>
{
  @Override
  public GENIncomingMessageDecoder createDecoder(GENTransport<I, List<ByteBuffer>> transport, GENReceptionHandler receptionHandler, I messageSource)
  {
    return new SPPMessageDecoder((SPPBaseTransport<I>)transport, messageSource);
  }

  /**
   * Implementation of the GENIncomingMessageDecoder class for newly arrived MAL Messages in SPPMessage format.
   * @param <I>
   */
  public static final class SPPMessageDecoder<I> implements GENIncomingMessageDecoder
  {
    private final SPPBaseTransport<I> transport;
    private final I rawMessage;

    /**
     * Constructor
     *
     * @param transport Containing transport.
     * @param rawMessage The raw message
     */
    public SPPMessageDecoder(SPPBaseTransport<I> transport, I rawMessage)
    {
      this.transport = transport;
      this.rawMessage = rawMessage;
    }

    @Override
    public GENIncomingMessageHolder decodeAndCreateMessage() throws MALException
    {
      GENTransport.PacketToString smsg = transport.new PacketToString(null);
      GENMessage malMsg = transport.createMessage(rawMessage);
      
      if (null != malMsg)
      {
        return new GENIncomingMessageHolder(malMsg.getHeader().getTransactionId(), malMsg, smsg);
      }
      
      return null;
    }
  }
}
