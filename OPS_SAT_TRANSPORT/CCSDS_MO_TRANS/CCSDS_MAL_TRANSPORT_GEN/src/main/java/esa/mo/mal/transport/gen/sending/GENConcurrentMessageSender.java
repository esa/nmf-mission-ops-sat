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

import esa.mo.mal.transport.gen.GENTransport;
import static esa.mo.mal.transport.gen.GENTransport.LOGGER;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import java.io.IOException;
import static java.lang.Thread.interrupted;

/**
 * This class manages a set of threads that are able to send messages via transceivers. It uses a blocking queue from
 * where all threads consume messages to be sent. The worker threads are created via the addProcessor method which is
 * called when a new connection is associated to a given URI by the transport.
 *
 * Each object of this class is associated with a URI at the transport level.
 *
 * There is normally a numConnections (transport configuration item) number of threads.
 *
 * It accepts requests to send the message, which is done via the worker threads. A reply is provided indicating if the
 * message was sent successfully or not.
 *
 */
public class GENConcurrentMessageSender
{
  /**
   * input message queue
   */
  private final BlockingQueue<GENOutgoingMessageHolder> outgoingQueue;

  /**
   * the list of processing threads that send the messages
   */
  private final List<GENSenderThread> processingThreads;

  /**
   * reference to the transport
   */
  private final GENTransport transport;

  /**
   * reference to target URI
   */
  private final String targetURI;

  /**
   * Creates a new instance. Typically each instance is associated with a given URI.
   *
   * @param transport reference to the transport
   * @param targetURI
   */
  public GENConcurrentMessageSender(GENTransport transport, String targetURI)
  {
    outgoingQueue = new LinkedBlockingQueue<GENOutgoingMessageHolder>();
    processingThreads = Collections.synchronizedList(new ArrayList<GENSenderThread>());
    this.transport = transport;
    this.targetURI = targetURI;
  }

  /**
   * This method will try to send the message via one of the available connections and provide a reply through the
   * GENOutgoingMessageHolder object if the message was successful or not. Users of this method should call getResult to
   * block waiting for an indication if the message was sent successfully or not.
   *
   * @param message the message to be sent.
   */
  public void sendMessage(GENOutgoingMessageHolder message)
  {
    if (processingThreads.isEmpty())
    {
      //this should never happen. Only possibly in boundary cases where this object is asked
      //to terminate and there is another thread trying to send message in parallel.
      LOGGER.log(Level.SEVERE, "No active processors in this processing queue!");
      message.setResult(Boolean.FALSE);

      return;
    }

    boolean inserted = outgoingQueue.add(message);
    if (!inserted)
    {
      // log error. According to the specification (see *add* call
      // documentation) this will always return true, or throw an
      // exception
      LOGGER.log(Level.SEVERE, "Could not insert message to processing queue");
      message.setResult(Boolean.FALSE);
    }
  }

  /**
   * Adds a processor which is able to send messages to a specific URI
   *
   * @param messageSender the socket that this processor will use
   * @param uriTo the target URI
   * @return number of active processors
   */
  public synchronized int addProcessor(GENMessageSender messageSender, String uriTo)
  {
    // create new thread
    GENSenderThread procThread = new GENSenderThread(messageSender, uriTo);

    // keep reference to thread
    processingThreads.add(procThread);

    // start thread
    procThread.start();

    LOGGER.log(Level.FINE, "Adding processor for URI:{0} total processors:{1}", new Object[]
    {
      uriTo, processingThreads.size()
    });

    // return number of processors
    return processingThreads.size();
  }

  /**
   * Returns the URI this class sends to.
   *
   * @return the target URI.
   */
  public String getTargetURI()
  {
    return targetURI;
  }

  /**
   * Returns the number of concurrent processors.
   *
   * @return the number of processing threads.
   */
  public synchronized int getNumberOfProcessors()
  {
    return processingThreads.size();
  }

  /**
   * This method will shutdown all processing threads (by calling their interrupt method) which will result in all of
   * them closing their sockets and terminating their processing.
   *
   * Typically Called by the transport in order to shutdown all processing threads and close all remote connections.
   */
  public synchronized void terminate()
  {
    LOGGER.log(Level.INFO, "Terminating all processing threads for sender for URI:{0}", targetURI);

    for (GENSenderThread t : processingThreads)
    {
      // this will cause all threads to terminate
      LOGGER.log(Level.FINE, "Terminating sender processing thread for URI:{0}", t.getUriTo());
      t.interrupt();
    }

    // clear the references to active threads
    processingThreads.clear();
  }

  /**
   * This thread will listen for outgoing messages through a blocking queue and send them through a transceiver. In case
   * of communication problems it will inform the transport and terminate.
   *
   * In any case, a reply is send back to the originator of the request using a blocking queue from the outgoing
   * message.
   *
   */
  private class GENSenderThread extends Thread
  {
    /**
     * The destination URI
     */
    private final String uriTo;

    /**
     * The message sender
     */
    private final GENMessageSender messageSender;

    /**
     * Constructor
     *
     * @param messageSender
     * @param uriTo
     */
    public GENSenderThread(GENMessageSender messageSender, String uriTo)
    {
      this.uriTo = uriTo;
      this.messageSender = messageSender;
      setName(getClass().getName() + " URI:" + uriTo);
    }

    @Override
    public void run()
    {
      boolean bContinue = true;

      // read forever while not interrupted
      while (bContinue && !interrupted())
      {
        GENOutgoingMessageHolder messageHolder = null;
        try
        {
          messageHolder = outgoingQueue.take();

          messageSender.sendEncodedMessage(messageHolder);

          //send back reply that the message was sent succesfully
          messageHolder.setResult(Boolean.TRUE);
        }
        catch (IOException e)
        {
          LOGGER.log(Level.WARNING, "Cannot send packet to destination:{0} informing transport", uriTo);
          LOGGER.log(Level.FINE, "Cannot send packet to destination:{0} informing transport", e);

          //send back reply that the message was not sent successfully
          if (null != messageHolder)
          {
            messageHolder.setResult(Boolean.FALSE);
          }

          //inform transport about communication error 
          transport.communicationError(uriTo, null);
          bContinue = false;
        }
        catch (InterruptedException e)
        {
          // finish processing
          bContinue = false;
        }
        catch (Throwable ex)
        {
          ex.printStackTrace();
        }
      }

      // finished processing, close connection if not already closed
      messageSender.close();
    }

    /**
     * Returns the destination URI being sent to.
     *
     * @return the URI to.
     */
    public String getUriTo()
    {
      return uriTo;
    }
  }
}
