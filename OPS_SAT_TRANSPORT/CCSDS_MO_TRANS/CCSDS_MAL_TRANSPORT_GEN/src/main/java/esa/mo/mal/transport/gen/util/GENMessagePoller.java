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
package esa.mo.mal.transport.gen.util;

import esa.mo.mal.transport.gen.GENReceptionHandler;
import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.GENTransport;
import esa.mo.mal.transport.gen.receivers.GENIncomingMessageDecoderFactory;
import java.io.IOException;
import java.io.EOFException;
import java.util.logging.Level;
import static esa.mo.mal.transport.gen.GENTransport.LOGGER;

/**
 * This utility class creates a thread to pull encoded messages from a transceiver. It receives messages from it and
 * then forwards the incoming message to an asynchronous processor in order to return immediately and not hold the
 * calling thread while the message is processed.
 *
 * In case of a communication problem it informs the transport and/or closes the resource
 *
 * Only transport adapter that pull messages from their transport layer will need to use this class.
 *
 * @param <I> The type of the encoded messages.
 * @param <O> The type of the outgoing messages.
 */
public class GENMessagePoller<I, O> extends Thread implements GENReceptionHandler
{
  /**
   * Reference to the transport
   */
  protected final GENTransport transport;
  /**
   * the low level message sender
   */
  protected final GENMessageSender messageSender;
  /**
   * the low level message receiver
   */
  protected final MessageAdapter<I, O> messageReceiver;
  /**
   * the remote URI (client) this connection is associated to. This is volatile as it is potentially set by a different
   * thread after its creation
   */
  private volatile String remoteURI = null;

  /**
   * Constructor.
   *
   * @param transport Message transport being used.
   * @param messageSender The message sending interface associated to this connection.
   * @param messageReceiver The message reception interface, used for pulling messaging into this transport.
   * @param decoderFactory The decoder factory to create message decoders from.
   */
  public GENMessagePoller(GENTransport<I, O> transport,
          GENMessageSender messageSender,
          GENMessageReceiver<I> messageReceiver,
          GENIncomingMessageDecoderFactory<I, O> decoderFactory)
  {
    this.transport = transport;
    this.messageSender = messageSender;
    this.messageReceiver = new MessageAdapter<I, O>(transport, this, messageReceiver, decoderFactory);
    setName(getClass().getName());
  }

  /**
   * Constructor.
   *
   * @param transport Message transport being used.
   * @param messageSender The message sending interface associated to this connection.
   * @param messageReceiver The message reception interface, used for pulling messaging into this transport.
   */
  protected GENMessagePoller(GENTransport<I, O> transport, GENMessageSender messageSender, MessageAdapter messageReceiver)
  {
    this.transport = transport;
    this.messageSender = messageSender;
    this.messageReceiver = messageReceiver;
    setName(getClass().getName());
  }

  @Override
  public void run()
  {
    boolean bContinue = true;

    // handles message reads from this client
    while (bContinue && !interrupted())
    {
      try
      {
        messageReceiver.receiveMessage();
      }
      catch (InterruptedException ex)
      {
        LOGGER.log(Level.INFO, "Client closing connection: {0}", remoteURI);

        transport.closeConnection(remoteURI, this);
        close();

        //and terminate
        bContinue = false;
      }
      catch (EOFException ex)
      {
        LOGGER.log(Level.INFO, "Client closing connection: {0}", remoteURI);

        transport.closeConnection(remoteURI, this);
        close();

        //and terminate
        bContinue = false;
      }
      catch (IOException e)
      {
        LOGGER.log(Level.WARNING, "Cannot read message from client", e);

        transport.communicationError(remoteURI, this);
        close();

        //and terminate
        bContinue = false;
      }
    }
  }

  @Override
  public String getRemoteURI()
  {
    return remoteURI;
  }

  @Override
  public void setRemoteURI(String remoteURI)
  {
    this.remoteURI = remoteURI;
    setName(getClass().getName() + " URI:" + remoteURI);
  }

  @Override
  public GENMessageSender getMessageSender()
  {
    return messageSender;
  }

  @Override
  public void close()
  {
    messageSender.close();
    messageReceiver.close();
  }

  /**
   * Simple interface for reading encoded messages from a low level transport. Used by the message poller class.
   *
   * @param <T> The type of the encoded messages.
   */
  public static interface GENMessageReceiver<T>
  {
    /**
     * Reads an encoded MALMessage.
     *
     * @return the object containing the encoded MAL Message, may be null if nothing to read at this time
     * @throws IOException in case the encoded message cannot be read
     * @throws InterruptedException in case IO read is interrupted
     */
    T readEncodedMessage() throws IOException, InterruptedException;

    /**
     * Closes any used resources.
     */
    void close();
  }

  /**
   * Internal class for adapting from the message receivers to the relevant receive operation on the transport.
   *
   * @param <I> The type of the encoded messages.
   */
  protected static class MessageAdapter<I, O>
  {
    private final GENTransport transport;
    private final GENReceptionHandler handler;
    private final GENMessageReceiver<I> receiver;
    private final GENIncomingMessageDecoderFactory<I, O> decoderFactory;

    /**
     * Constructor.
     *
     * @param transport Transport to pass messages to.
     * @param handler The reception handler.
     * @param receiver The receiver to pull messages from.
     * @param decoderFactory The decoder factory to create message decoders from.
     */
    public MessageAdapter(GENTransport transport,
            GENReceptionHandler handler,
            GENMessageReceiver<I> receiver,
            GENIncomingMessageDecoderFactory<I, O> decoderFactory)
    {
      this.transport = transport;
      this.handler = handler;
      this.receiver = receiver;
      this.decoderFactory = decoderFactory;
    }

    /**
     * Takes the message from the receiver and passes it to the transport if not null.
     *
     * @throws IOException in case the encoded message cannot be read
     * @throws InterruptedException in case IO read is interrupted
     */
    public void receiveMessage() throws IOException, InterruptedException
    {
      I msg = receiver.readEncodedMessage();

      if (null != msg)
      {
        transport.receive(handler, decoderFactory.createDecoder(transport, handler, msg));
      }
    }

    /**
     * Closes any used resources.
     */
    public void close()
    {
      receiver.close();
    }
  }
}
