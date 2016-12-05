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

import esa.mo.mal.transport.gen.GENReceptionHandler;
import esa.mo.mal.transport.gen.GENTransport;

/**
 * Interface for factory classes used by the receivers for creating decoders.
 *
 * @param <I> The type of the incoming messages.
 * @param <O> The type of the outgoing messages.
 */
public interface GENIncomingMessageDecoderFactory<I, O>
{
  /**
   * Creates a decoder for the supplied message source.
   *
   * @param transport Transport to pass messages to.
   * @param receptionHandler The reception handler.
   * @param messageSource The message source to pass to the decoder.
   * @return the new message decoder.
   */
  GENIncomingMessageDecoder createDecoder(final GENTransport<I, O> transport, GENReceptionHandler receptionHandler, I messageSource);
}
