/* ----------------------------------------------------------------------------
 * Copyright (C) 2014      European Space Agency
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
package esa.mo.mal.transport.gen.sending;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static esa.mo.mal.transport.gen.GENTransport.LOGGER;
import java.util.concurrent.TimeUnit;
import org.ccsds.moims.mo.mal.transport.MALMessage;

/**
 * This class holds the message to be sent in encoded format and a reply queue that the internal sender of the message
 * can listen to in order to be informed if the message was successfully sent or not.
 *
 * @param <O> The type of the encoded message.
 */
public class GENOutgoingMessageHolder<O>
{
  /**
   * The reply queue
   */
  private final BlockingQueue<Boolean> replyQueue;

  /**
   * The timeout in seconds to wait for confirmation of delivery
   */
  private final int timeout;
  /**
   * The destination root URI, holds the connection level URI
   */
  private final String destinationRootURI;
  /**
   * The complete destination URI
   */
  private final String destinationURI;
  /**
   * The message handle for multi-send messages, may be NULL
   */
  private final Object multiSendHandle;
  /**
   * True if this is the last message in the multi-send for the supplied handle
   */
  private final boolean lastForHandle;
  /**
   * The encoded message
   */
  private final MALMessage originalMessage;
  /**
   * The encoded message
   */
  private final O encodedMessage;

  /**
   * Will construct a new object and create a new internal reply queue.
   *
   * @param timeout The timeout in seconds to wait for confirmation of delivery.
   * @param destinationRootURI The destination root URI, holds the connection level URI.
   * @param destinationURI The complete destination URI.
   * @param multiSendHandle The message handle for multi-send messages, may be NULL.
   * @param lastForHandle True if this is the last message in the multi-send for the supplied handle.
   * @param originalMessage The un-encoded message to be sent
   * @param encodedMessage The encoded message to be sent
   */
  public GENOutgoingMessageHolder(final int timeout,
          final String destinationRootURI,
          final String destinationURI,
          final Object multiSendHandle,
          final boolean lastForHandle,
          final MALMessage originalMessage,
          O encodedMessage)
  {
    replyQueue = new LinkedBlockingQueue<Boolean>();
    this.timeout = timeout;
    this.destinationRootURI = destinationRootURI;
    this.destinationURI = destinationURI;
    this.multiSendHandle = multiSendHandle;
    this.lastForHandle = lastForHandle;
    this.originalMessage = originalMessage;
    this.encodedMessage = encodedMessage;
  }

  /**
   * This method blocks until there is an attempt to send the message.
   *
   * @return TRUE if the message was successfully sent and FALSE if there was a communication or internal problem.
   * @throws InterruptedException in case of shutting down or internal error
   */
  public Boolean getResult() throws InterruptedException
  {
    return replyQueue.poll(timeout, TimeUnit.SECONDS);
  }

  /**
   * Sets the result indicating if the message was sent successfully.
   *
   * @param result TRUE if the message was successfully sent and FALSE if there was a communication or internal problem.
   */
  public void setResult(Boolean result)
  {
    boolean inserted = replyQueue.add(result);
    if (!inserted)
    {
      // log error. According to the specification (see *add* call
      // documentation) this will always return true, or throw an
      // exception
      LOGGER.log(Level.SEVERE, "Could not insert result to processing queue", new Throwable());
    }
  }

  /**
   * Returns the complete destination URI.
   *
   * @return the complete destination URI.
   */
  public String getDestinationURI()
  {
    return destinationURI;
  }

  /**
   * Returns the destination root URI.
   *
   * @return The root URI.
   */
  public String getDestinationRootURI()
  {
    return destinationRootURI;
  }

  /**
   * returns the multi send message handle.
   *
   * @return the message handle.
   */
  public Object getMultiSendHandle()
  {
    return multiSendHandle;
  }

  /**
   * Returns true is this is the last message in a multi-send for the current handle.
   *
   * @return true if last message.
   */
  public boolean isLastForHandle()
  {
    return lastForHandle;
  }

  /**
   * Getter for the original message to be sent
   *
   * @return the original message
   */
  public MALMessage getOriginalMessage()
  {
    return originalMessage;
  }

  /**
   * Getter for the encoded message to be sent
   *
   * @return the encoded message
   */
  public O getEncodedMessage()
  {
    return encodedMessage;
  }
}
