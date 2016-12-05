/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
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
import esa.mo.mal.transport.gen.GENReceptionHandler;
import esa.mo.mal.transport.gen.GENTransport;
import java.io.InputStream;
import org.ccsds.moims.mo.mal.MALException;

/**
 * Factory class for IO stream decoders.
 */
public class GENIncomingStreamMessageDecoderFactory<O> implements GENIncomingMessageDecoderFactory<InputStream, O>
{
  @Override
  public GENIncomingMessageDecoder createDecoder(GENTransport<InputStream, O> transport, GENReceptionHandler receptionHandler, InputStream messageSource)
  {
    return new GENIncomingStreamMessageDecoder(transport, messageSource);
  }

  /**
   * Implementation of the GENIncomingMessageDecoder class for newly arrived MAL Messages in stream format.
   */
  public static final class GENIncomingStreamMessageDecoder<O> implements GENIncomingMessageDecoder
  {
    private final GENTransport<InputStream, O> transport;
    private final InputStream ios;

    /**
     * Constructor
     *
     * @param transport Containing transport.
     * @param ios The stream message
     */
    public GENIncomingStreamMessageDecoder(final GENTransport<InputStream, O> transport, InputStream ios)
    {
      this.transport = transport;
      this.ios = ios;
    }

    @Override
    public GENIncomingMessageHolder decodeAndCreateMessage() throws MALException
    {
      GENTransport.PacketToString smsg = transport.new PacketToString(null);
      GENMessage malMsg = transport.createMessage(ios);
      return new GENIncomingMessageHolder(malMsg.getHeader().getTransactionId(), malMsg, smsg);
    }
  }
}
