/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
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
package esa.mo.mal.transport.gen;

import esa.mo.mal.transport.gen.receivers.GENIncomingMessageDecoder;
import esa.mo.mal.transport.gen.receivers.GENIncomingMessageHolder;
import esa.mo.mal.transport.gen.sending.GENConcurrentMessageSender;
import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.sending.GENOutgoingMessageHolder;
import esa.mo.mal.transport.gen.util.GENHelper;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.*;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.structures.*;
import org.ccsds.moims.mo.mal.transport.*;

/**
 * A generic implementation of the transport interface.
 * @param <I> The type of incoming message
 * @param <O> The type of the outgoing encoded message
 */
public abstract class GENTransport<I, O> implements MALTransport
{
  /**
   * System property to control whether message parts of wrapped in BLOBs.
   */
  public static final String WRAP_PROPERTY = "org.ccsds.moims.mo.mal.transport.gen.wrap";
  /**
   * System property to control whether in-process processing supported.
   */
  public static final String INPROC_PROPERTY = "org.ccsds.moims.mo.mal.transport.gen.fastInProcessMessages";
  /**
   * System property to control whether debug messages are generated.
   */
  public static final String DEBUG_PROPERTY = "org.ccsds.moims.mo.mal.transport.gen.debug";
  /**
   * System property to control the number of input processors.
   */
  public static final String INPUT_PROCESSORS_PROPERTY = "org.ccsds.moims.mo.mal.transport.gen.inputprocessors";
  /**
   * System property to control the number of input processors.
   */
  public static final String MIN_INPUT_PROCESSORS_PROPERTY = "org.ccsds.moims.mo.mal.transport.gen.mininputprocessors";
  /**
   * System property to control the number of input processors.
   */
  public static final String IDLE_INPUT_PROCESSORS_PROPERTY = "org.ccsds.moims.mo.mal.transport.gen.idleinputprocessors";
  /**
   * System property to control the number of connections per client.
   */
  public static final String NUM_CLIENT_CONNS_PROPERTY = "org.ccsds.moims.mo.mal.transport.gen.numconnections";
  /**
   * The timeout in seconds to wait for confirmation of delivery.
   */
  public static final String DELIVERY_TIMEOUT_PROPERTY = "org.ccsds.moims.mo.mal.transport.gen.deliverytimeout";
  /**
   * Charset used for converting the encoded message into a string for debugging.
   */
  public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
  /**
   * Logger
   */
  public static final java.util.logging.Logger LOGGER = Logger.getLogger("org.ccsds.moims.mo.mal.transport.gen");
  /**
   * Used to create random local names for endpoints.
   */
  protected static final Random RANDOM_NAME = new Random();
  /**
   * Reference to our factory.
   */
  protected final MALTransportFactory factory;
  /**
   * The delimiter to use to separate the protocol part from the address part of the URL.
   */
  protected final String protocolDelim;
  /**
   * The delimiter to use to separate the external address part from the internal object part of the URL.
   */
  protected final char serviceDelim;
  /**
   * If the protocol delimiter is the same as the service delimiter then we need a count to find the correct service delimiter.
   */
  protected final int serviceDelimCounter;
  /**
   * Delimiter to use when holding routing information in a URL
   */
  protected final char routingDelim;
  /**
   * True if protocol supports the concept of routing.
   */
  protected final boolean supportsRouting;
  /**
   * True if string based stream, can be logged as a string rather than hex.
   */
  protected final boolean streamHasStrings;
  /**
   * True if body parts should be wrapped in blobs for encoded element support.
   */
  protected final boolean wrapBodyParts;
  /**
   * True if calls to ourselves should be handled in-process i.e. not via the underlying transport.
   */
  protected final boolean inProcessSupport;
  /**
   * The timeout in seconds to wait for confirmation of delivery.
   */
  protected final int deliveryTimeout;
  /**
   * True if want to log the packet data
   */
  protected final boolean logFullDebug;
  /**
   * The string used to represent this protocol.
   */
  protected final String protocol;
  /**
   * Map of string MAL names to endpoints.
   */
  protected final Map<String, GENEndpoint> endpointMalMap = new HashMap<String, GENEndpoint>();
  /**
   * Map of string transport routing names to endpoints.
   */
  protected final Map<String, GENEndpoint> endpointRoutingMap = new HashMap<String, GENEndpoint>();
  /**
   * Map of QoS properties.
   */
  protected final Map qosProperties;
  /**
   * The number of connections per client or server. The Transport will connect numConnections times to the predefined port and host
   * per different client/server.
   */
  private final int numConnections;
  /**
   * The thread that receives incoming message from the underlying transport. All incoming raw data packets are processed by this
   * thread.
   */
  private final ExecutorService asyncInputReceptionProcessor;
  /**
   * The thread pool of input message processors. All incoming messages are processed by this thread pool after they have been
   * decoded by the asyncInputReceptionProcessor thread.
   */
  private final ExecutorService asyncInputDataProcessors;
  /**
   * The map of message queues, segregated by transaction id.
   */
  private final Map<Long, GENIncomingMessageProcessor> transactionQueues = new HashMap<Long, GENIncomingMessageProcessor>();
  /**
   * Map of outgoing channels. This associates a URI to a transport resource that is able to send messages to this URI.
   */
  private final Map<String, GENConcurrentMessageSender> outgoingDataChannels = new HashMap<String, GENConcurrentMessageSender>();
  /**
   * The stream factory used for encoding and decoding messages.
   */
  private final MALElementStreamFactory streamFactory;
  /**
   * The base string for URL for this protocol.
   */
  protected String uriBase;

  /**
   * Constructor.
   *
   * @param protocol The protocol string.
   * @param serviceDelim The delimiter to use for separating the URL
   * @param supportsRouting True if routing is supported by the naming convention
   * @param wrapBodyParts True is body parts should be wrapped in BLOBs
   * @param factory The factory that created us.
   * @param properties The QoS properties.
   * @throws MALException On error.
   */
  public GENTransport(final String protocol,
          final char serviceDelim,
          final boolean supportsRouting,
          final boolean wrapBodyParts,
          final MALTransportFactory factory,
          final java.util.Map properties) throws MALException
  {
    this.factory = factory;
    this.protocol = protocol;
    this.supportsRouting = supportsRouting;
    this.protocolDelim = "://";
    this.serviceDelim = serviceDelim;
    this.routingDelim = '@';
    this.qosProperties = properties;
    this.streamFactory = MALElementStreamFactory.newFactory(protocol, properties);

    if (protocolDelim.contains("" + serviceDelim))
    {
      serviceDelimCounter = protocolDelim.length() - protocolDelim.replace("" + serviceDelim, "").length();
    }
    else
    {
      serviceDelimCounter = 0;
    }

    LOGGER.log(Level.FINE, "GEN Creating element stream : {0}", streamFactory.getClass().getName());

    // very crude and faulty test but it will do for testing
    this.streamHasStrings = streamFactory.getClass().getName().contains("String");

    // default values
    boolean lLogFullDebug = false;
    boolean lWrapBodyParts = wrapBodyParts;
    boolean lInProcessSupport = true;
    int lNumConnections = 1;
    int lDeliveryTime = 10;

    // decode configuration
    if (properties != null)
    {
      if (properties.containsKey(DEBUG_PROPERTY))
      {
        lLogFullDebug = Boolean.parseBoolean((String) properties.get(DEBUG_PROPERTY));
      }

      if (properties.containsKey(WRAP_PROPERTY))
      {
        lWrapBodyParts = Boolean.parseBoolean((String) properties.get(WRAP_PROPERTY));
      }

      if (properties.containsKey(INPROC_PROPERTY))
      {
        lInProcessSupport = Boolean.parseBoolean((String) properties.get(INPROC_PROPERTY));
      }

      // number of connections per client/server
      if (properties.containsKey(NUM_CLIENT_CONNS_PROPERTY))
      {
        lNumConnections = Integer.parseInt((String) properties.get(NUM_CLIENT_CONNS_PROPERTY));
      }

      if (properties.containsKey(DELIVERY_TIMEOUT_PROPERTY))
      {
        lDeliveryTime = Integer.parseInt((String) properties.get(DELIVERY_TIMEOUT_PROPERTY));
      }
    }

    this.logFullDebug = lLogFullDebug;
    this.wrapBodyParts = lWrapBodyParts;
    this.inProcessSupport = lInProcessSupport;
    this.numConnections = lNumConnections;
    this.deliveryTimeout = lDeliveryTime;

    this.asyncInputReceptionProcessor = Executors.newSingleThreadExecutor();
    this.asyncInputDataProcessors = createThreadPoolExecutor(properties);

    LOGGER.log(Level.FINE, "GEN Wrapping body parts set to  : {0}", this.wrapBodyParts);
  }

  /**
   * Constructor.
   *
   * @param protocol The protocol string.
   * @param protocolDelim The delimiter to use for separating the protocol part in the URL
   * @param serviceDelim The delimiter to use for separating the URL
   * @param routingDelim The delimiter to use for separating the URL for routing
   * @param supportsRouting True if routing is supported by the naming convention
   * @param wrapBodyParts True is body parts should be wrapped in BLOBs
   * @param factory The factory that created us.
   * @param properties The QoS properties.
   * @throws MALException On error.
   */
  public GENTransport(final String protocol,
          final String protocolDelim,
          final char serviceDelim,
          final char routingDelim,
          final boolean supportsRouting,
          final boolean wrapBodyParts,
          final MALTransportFactory factory,
          final java.util.Map properties) throws MALException
  {
    this.factory = factory;
    this.protocol = protocol;
    this.supportsRouting = supportsRouting;
    this.protocolDelim = protocolDelim;
    this.serviceDelim = serviceDelim;
    this.routingDelim = routingDelim;
    this.qosProperties = properties;
    streamFactory = MALElementStreamFactory.newFactory(protocol, properties);

    if (protocolDelim.contains("" + serviceDelim))
    {
      serviceDelimCounter = protocolDelim.length() - protocolDelim.replace("" + serviceDelim, "").length();
    }
    else
    {
      serviceDelimCounter = 0;
    }

    LOGGER.log(Level.FINE, "GEN Creating element stream : {0}", streamFactory.getClass().getName());

    // very crude and faulty test but it will do for testing
    streamHasStrings = streamFactory.getClass().getName().contains("String");

    // default values
    boolean lLogFullDebug = false;
    boolean lWrapBodyParts = wrapBodyParts;
    boolean lInProcessSupport = true;
    int lNumConnections = 1;
    int lDeliveryTime = 10;

    // decode configuration
    if (properties != null)
    {
      if (properties.containsKey(DEBUG_PROPERTY))
      {
        lLogFullDebug = Boolean.parseBoolean((String) properties.get(DEBUG_PROPERTY));
      }

      if (properties.containsKey(WRAP_PROPERTY))
      {
        lWrapBodyParts = Boolean.parseBoolean((String) properties.get(WRAP_PROPERTY));
      }

      if (properties.containsKey(INPROC_PROPERTY))
      {
        lInProcessSupport = Boolean.parseBoolean((String) properties.get(INPROC_PROPERTY));
      }

      // number of connections per client/server
      if (properties.containsKey(NUM_CLIENT_CONNS_PROPERTY))
      {
        lNumConnections = Integer.parseInt((String) properties.get(NUM_CLIENT_CONNS_PROPERTY));
      }

      if (properties.containsKey(DELIVERY_TIMEOUT_PROPERTY))
      {
        lDeliveryTime = Integer.parseInt((String) properties.get(DELIVERY_TIMEOUT_PROPERTY));
      }
    }

    this.logFullDebug = lLogFullDebug;
    this.wrapBodyParts = lWrapBodyParts;
    this.inProcessSupport = lInProcessSupport;
    this.numConnections = lNumConnections;
    this.deliveryTimeout = lDeliveryTime;

    asyncInputReceptionProcessor = Executors.newSingleThreadExecutor();
    this.asyncInputDataProcessors = createThreadPoolExecutor(properties);

    LOGGER.log(Level.FINE, "GEN Wrapping body parts set to  : {0}", this.wrapBodyParts);
  }

  /**
   * Initialises this transport.
   *
   * @throws MALException On error
   */
  public void init() throws MALException
  {
    String protocolString = protocol;
    if (protocol.contains(":"))
    {
      protocolString = protocol.substring(0, protocol.indexOf(':'));
    }

    uriBase = protocolString + protocolDelim + createTransportAddress() + serviceDelim;
  }

  @Override
  public MALEndpoint createEndpoint(final String localName, final Map qosProperties) throws MALException
  {
    final Map localProperties = new HashMap();

    if (null != this.qosProperties)
    {
      localProperties.putAll(this.qosProperties);
    }
    if (null != qosProperties)
    {
      localProperties.putAll(qosProperties);
    }

    final String strRoutingName = getLocalName(localName, localProperties);
    GENEndpoint endpoint = endpointRoutingMap.get(strRoutingName);

    if (null == endpoint)
    {
      LOGGER.log(Level.FINE, "GEN Creating endpoint {0} : {1}", new Object[]
      {
        localName, strRoutingName
      });
      endpoint = internalCreateEndpoint(localName, strRoutingName, localProperties);
      endpointMalMap.put(localName, endpoint);
      endpointRoutingMap.put(strRoutingName, endpoint);
    }

    return endpoint;
  }

  @Override
  public MALEndpoint getEndpoint(final String localName) throws IllegalArgumentException
  {
    return endpointMalMap.get(localName);
  }

  @Override
  public MALEndpoint getEndpoint(final URI uri) throws IllegalArgumentException
  {
    String endpointUriPart = getRoutingPart(uri.getValue());

    return endpointRoutingMap.get(endpointUriPart);
  }

  /**
   * Returns the stream factory.
   *
   * @return the stream factory
   */
  public MALElementStreamFactory getStreamFactory()
  {
    return streamFactory;
  }

  public abstract GENMessage createMessage(I packet) throws MALException;
  
  /**
   * On reception of an IO stream this method should be called. This is the main reception entry point into the generic transport
   * for stream based transports.
   *
   * @param receptionHandler The reception handler to pass them to.
   * @param decoder The class responsible for decoding the message from the incoming connection
   */
  public void receive(final GENReceptionHandler receptionHandler, final GENIncomingMessageDecoder decoder)
  {
    asyncInputReceptionProcessor.submit(new GENIncomingMessageReceiver(this, receptionHandler, decoder));
  }

  /**
   * The main exit point for messages from this transport.
   *
   * @param multiSendHandle A context handle for multi send
   * @param lastForHandle True if that is the last message in a multi send for the handle
   * @param msg The message to send.
   * @throws MALTransmitErrorException On transmit error.
   */
  public void sendMessage(final Object multiSendHandle,
          final boolean lastForHandle,
          final GENMessage msg) throws MALTransmitErrorException
  {
    if ((null == msg.getHeader().getURITo()) || (null == msg.getHeader().getURITo().getValue()))
    {
      throw new MALTransmitErrorException(msg.getHeader(),
              new MALStandardError(MALHelper.DESTINATION_UNKNOWN_ERROR_NUMBER, "URI To field must not be null"), qosProperties);
    }
    
    // get the root URI, (e.g. tcpip://10.0.0.1:61616 )
    String destinationURI = msg.getHeader().getURITo().getValue();
    String remoteRootURI = getRootURI(destinationURI);

    // first check if its actually a message to ourselves
    String endpointUriPart = getRoutingPart(destinationURI);

    if (inProcessSupport &&
            (uriBase.startsWith(remoteRootURI) || remoteRootURI.startsWith(uriBase))
            && endpointRoutingMap.containsKey(endpointUriPart))
    {
      LOGGER.log(Level.FINE, "GEN routing msg internally to {0}", new Object[]
      {
        endpointUriPart
      });

      // if local then just send internally
      receiveIncomingMessage(new GENIncomingMessageHolder(msg.getHeader().getTransactionId(), msg, new PacketToString(null)));
    }
    else
    {
      try
      {
        LOGGER.log(Level.FINE, "GEN sending msg. Target root URI: {0} full URI:{1}", new Object[]
        {
          remoteRootURI, destinationURI
        });

        // get outgoing channel
        GENConcurrentMessageSender dataSender = manageCommunicationChannel(msg, false, null);

        GENOutgoingMessageHolder outgoingPacket = internalEncodeMessage(remoteRootURI, destinationURI, multiSendHandle, lastForHandle, dataSender.getTargetURI(), msg);

        dataSender.sendMessage(outgoingPacket);

        if (!Boolean.TRUE.equals(outgoingPacket.getResult()))
        {
          // data was not sent succesfully, throw an exception for the
          // higher MAL layers
          throw new MALTransmitErrorException(msg.getHeader(), new MALStandardError(MALHelper.DELIVERY_FAILED_ERROR_NUMBER, null), null);
        }

        LOGGER.log(Level.FINE, "GEN finished Sending data to {0}", remoteRootURI);
      }
      catch (MALTransmitErrorException e)
      {
        // this stops any true MAL exceptoins getting caught by the generic catch all below
        throw e;
      }
      catch (InterruptedException e)
      {
        LOGGER.log(Level.SEVERE, "Interrupted while waiting for data reply", e);
        throw new MALTransmitErrorException(msg.getHeader(), new MALStandardError(MALHelper.INTERNAL_ERROR_NUMBER, null), null);
      }
      catch (Exception t)
      {
        LOGGER.log(Level.SEVERE, "GEN could not send message!", t);
        throw new MALTransmitErrorException(msg.getHeader(), new MALStandardError(MALHelper.INTERNAL_ERROR_NUMBER, null), null);
      }
    }
  }

  /**
   * Used to request the transport close a connection with a client. In this case the transport will terminate all communication
   * channels with the destination in order for them to be re-established.
   *
   * @param uriTo the connection handler that received this message
   * @param receptionHandler
   */
  public void closeConnection(final String uriTo, final GENReceptionHandler receptionHandler)
  {
    String localUriTo = uriTo;
    // remove all associations with this target URI
    if ((null == localUriTo) && (null != receptionHandler))
    {
      localUriTo = receptionHandler.getRemoteURI();
    }

    if (localUriTo != null)
    {
      GENConcurrentMessageSender commsChannel;

      synchronized (this)
      {
        commsChannel = outgoingDataChannels.get(localUriTo);
        if (commsChannel != null)
        {
          outgoingDataChannels.remove(localUriTo);
        }
        else
        {
          LOGGER.log(Level.WARNING, "Could not locate associated data to close communications for URI : {0} ", localUriTo);
        }
      }
      if (commsChannel != null)
      {
        // need to do this outside the sync block so that we do not affect other threads
        commsChannel.terminate();
      }
    }

    if (null != receptionHandler)
    {
      receptionHandler.close();
    }
  }

  /**
   * Used to inform the transport about communication problems with clients. In this case the transport will terminate all
   * communication channels with the destination in order for them to be re-established.
   *
   * @param uriTo the connection handler that received this message
   * @param receptionHandler
   */
  public void communicationError(String uriTo, GENReceptionHandler receptionHandler)
  {
    LOGGER.log(Level.WARNING, "GEN Communication Error with {0} ", uriTo);

    closeConnection(uriTo, receptionHandler);
  }

  @Override
  public void deleteEndpoint(final String localName) throws MALException
  {
    final GENEndpoint endpoint = endpointMalMap.get(localName);

    if (null != endpoint)
    {
      LOGGER.log(Level.INFO, "GEN Deleting endpoint", localName);
      endpointMalMap.remove(localName);
      endpointRoutingMap.remove(endpoint.getRoutingName());
      endpoint.close();
    }
  }

  @Override
  public void close() throws MALException
  {
    for (Map.Entry<String, GENEndpoint> entry : endpointMalMap.entrySet())
    {
      entry.getValue().close();
    }

    endpointMalMap.clear();
    endpointRoutingMap.clear();

    asyncInputReceptionProcessor.shutdown();
    asyncInputDataProcessors.shutdown();

    LOGGER.fine("Closing outgoing channels");
    for (Map.Entry<String, GENConcurrentMessageSender> entry : outgoingDataChannels.entrySet())
    {
      final GENConcurrentMessageSender sender = entry.getValue();

      sender.terminate();
    }

    outgoingDataChannels.clear();
    LOGGER.fine("Closed outgoing channels");
  }

  /**
   * This method receives an incoming message and adds to to the correct queue based on its transaction id.
   *
   * @param malMsg the message
   */
  protected void receiveIncomingMessage(final GENIncomingMessageHolder malMsg)
  {
    LOGGER.log(Level.FINE, "GEN Queuing message : {0} : {1}", new Object[]
    {
      malMsg.malMsg.getHeader().getTransactionId(), malMsg.smsg
    });

    synchronized (transactionQueues)
    {
      GENIncomingMessageProcessor proc = transactionQueues.get(malMsg.transactionId);

      if (null == proc)
      {
        proc = new GENIncomingMessageProcessor(malMsg);
        transactionQueues.put(malMsg.transactionId, proc);
        asyncInputDataProcessors.submit(proc);
      }
      else if (proc.addMessage(malMsg))
      {
        // need to resubmit this to the processing threads
        asyncInputDataProcessors.submit(proc);
      }

      Set<Long> transactionsToRemove = new HashSet<Long>();
      for (Map.Entry<Long, GENIncomingMessageProcessor> entrySet : transactionQueues.entrySet())
      {
        Long key = entrySet.getKey();
        GENIncomingMessageProcessor lproc = entrySet.getValue();

        if (lproc.isFinished())
        {
          transactionsToRemove.add(key);
        }
      }

      for (Long transId : transactionsToRemove)
      {
        transactionQueues.remove(transId);
      }
    }
  }

  /**
   * This method processes an incoming message by routing it to the appropriate endpoint, returning an error if the message cannot
   * be processed.
   *
   * @param msg The source message.
   * @param smsg The message in a string representation for logging.
   */
  protected void processIncomingMessage(final GENMessage msg, PacketToString smsg)
  {
    try
    {
      LOGGER.log(Level.FINE, "GEN Processing message : {0} : {1}", new Object[]
      {
        msg.getHeader().getTransactionId(), smsg
      });

      String endpointUriPart = getRoutingPart(msg.getHeader().getURITo().getValue());

      final GENEndpoint oSkel = endpointRoutingMap.get(endpointUriPart);

      if (null != oSkel)
      {
        LOGGER.log(Level.FINE, "GEN Passing to message handler {0} : {1}", new Object[]
        {
          oSkel.getLocalName(), smsg
        });
        oSkel.receiveMessage(msg);
      }
      else
      {
        LOGGER.log(Level.WARNING, "GEN Message handler NOT FOUND {0} : {1}", new Object[]
        {
          endpointUriPart, smsg
        });
        returnErrorMessage(null,
                msg,
                MALHelper.DESTINATION_UNKNOWN_ERROR_NUMBER,
                "GEN Cannot find endpoint: " + endpointUriPart);
      }
    }
    catch (Exception e)
    {
      LOGGER.log(Level.WARNING, "GEN Error occurred when receiving data : {0}", e);

      final StringWriter wrt = new StringWriter();
      e.printStackTrace(new PrintWriter(wrt));

      try
      {
        returnErrorMessage(null,
                msg,
                MALHelper.INTERNAL_ERROR_NUMBER,
                "GEN Error occurred: " + e.toString() + " : " + wrt.toString());
      }
      catch (MALException ex)
      {
        LOGGER.log(Level.SEVERE, "GEN Error occurred when return error data : {0}", ex);
      }
    }
    catch (Error e)
    {
      // This is bad, Java errors are serious, so inform the other side if we can
      LOGGER.log(Level.SEVERE, "GEN Error occurred when processing message : {0}", e);

      final StringWriter wrt = new StringWriter();
      e.printStackTrace(new PrintWriter(wrt));

      try
      {
        returnErrorMessage(null,
                msg,
                MALHelper.INTERNAL_ERROR_NUMBER,
                "GEN Error occurred: " + e.toString() + " : " + wrt.toString());
      }
      catch (MALException ex)
      {
        LOGGER.log(Level.SEVERE, "GEN Error occurred when return error data : {0}", ex);
      }
    }
  }

  /**
   * Creates a return error message based on a received message.
   *
   * @param ep The endpoint to use for sending the error.
   * @param oriMsg The original message
   * @param errorNumber The error number
   * @param errorMsg The error message.
   * @throws MALException if cannot encode a response message
   */
  protected void returnErrorMessage(GENEndpoint ep,
          final GENMessage oriMsg,
          final UInteger errorNumber,
          final String errorMsg) throws MALException
  {
    try
    {
      final int type = oriMsg.getHeader().getInteractionType().getOrdinal();
      final short stage = (null != oriMsg.getHeader().getInteractionStage()) ?
              oriMsg.getHeader().getInteractionStage().getValue() :  0;

      // first check that message should be responded to
      if (((type == InteractionType._SUBMIT_INDEX) && (stage == MALSubmitOperation._SUBMIT_STAGE))
              || ((type == InteractionType._REQUEST_INDEX) && (stage == MALRequestOperation._REQUEST_STAGE))
              || ((type == InteractionType._INVOKE_INDEX) && (stage == MALInvokeOperation._INVOKE_STAGE))
              || ((type == InteractionType._PROGRESS_INDEX) && (stage == MALProgressOperation._PROGRESS_STAGE))
              || ((type == InteractionType._PUBSUB_INDEX) && (stage == MALPubSubOperation._REGISTER_STAGE))
              || ((type == InteractionType._PUBSUB_INDEX) && (stage == MALPubSubOperation._DEREGISTER_STAGE))
              || ((type == InteractionType._PUBSUB_INDEX) && (stage == MALPubSubOperation._PUBLISH_REGISTER_STAGE))
              || ((type == InteractionType._PUBSUB_INDEX) && (stage == MALPubSubOperation._PUBLISH_DEREGISTER_STAGE)))
      {
        final MALMessageHeader srcHdr = oriMsg.getHeader();

        if ((null == ep) && (!endpointMalMap.isEmpty()))
        {
          GENEndpoint endpoint = endpointMalMap.entrySet().iterator().next().getValue();

          final GENMessage retMsg = (GENMessage) endpoint.createMessage(srcHdr.getAuthenticationId(),
                  srcHdr.getURIFrom(),
                  new Time(new Date().getTime()),
                  srcHdr.getQoSlevel(),
                  srcHdr.getPriority(),
                  srcHdr.getDomain(),
                  srcHdr.getNetworkZone(),
                  srcHdr.getSession(),
                  srcHdr.getSessionName(),
                  srcHdr.getInteractionType(),
                  new UOctet((short) (srcHdr.getInteractionStage().getValue() + 1)),
                  srcHdr.getTransactionId(),
                  srcHdr.getServiceArea(),
                  srcHdr.getService(),
                  srcHdr.getOperation(),
                  srcHdr.getAreaVersion(),
                  true,
                  oriMsg.getQoSProperties(),
                  errorNumber, new Union(errorMsg));

          retMsg.getHeader().setURIFrom(srcHdr.getURITo());

          sendMessage(null, true, retMsg);
        }
        else
        {
          LOGGER.log(Level.WARNING, "GEN Unable to return error number ({0}) as no endpoint supplied : {1}", new Object[]
          {
            errorNumber, oriMsg.getHeader()
          });
        }
      }
      else
      {
        LOGGER.log(Level.WARNING, "GEN Unable to return error number ({0}) as already a return message : {1}", new Object[]
        {
          errorNumber, oriMsg.getHeader()
        });
      }
    }
    catch (MALTransmitErrorException ex)
    {
      LOGGER.log(Level.WARNING, "GEN Error occurred when attempting to return previous error : {0}", ex);
    }
  }

  /**
   * Returns the local name or creates a random one if null.
   *
   * @param localName The existing local name string to check.
   * @param properties The QoS properties.
   * @return The local name to use.
   */
  protected String getLocalName(String localName, final java.util.Map properties)
  {
    if ((null == localName) || (0 == localName.length()))
    {
      localName = String.valueOf(RANDOM_NAME.nextInt());
    }

    return localName;
  }

  /**
   * Returns the "root" URI from the full URI. The root URI only contains the protocol and the main destination and is something
   * unique for all URIs of the same MAL.
   *
   * @param fullURI the full URI, for example tcpip://10.0.0.1:61616-serviceXYZ
   * @return the root URI, for example tcpip://10.0.0.1:61616
   */
  public String getRootURI(String fullURI)
  {
    // get the root URI, (e.g. tcpip://10.0.0.1:61616 )
    int serviceDelimPosition = nthIndexOf(fullURI, serviceDelim, serviceDelimCounter);

    if (serviceDelimPosition < 0)
    {
      // does not exist, return as is      
      return fullURI;
    }

    return fullURI.substring(0, serviceDelimPosition);
  }

  /**
   * Returns the routing part of the URI.
   *
   * @param uriValue The URI value
   * @return the routing part of the URI
   */
  public String getRoutingPart(String uriValue)
  {
    String endpointUriPart = uriValue;
    final int iFirst = nthIndexOf(endpointUriPart, serviceDelim, serviceDelimCounter);
    int iSecond = supportsRouting ? endpointUriPart.indexOf(routingDelim) : endpointUriPart.length();
    if (0 > iSecond)
    {
      iSecond = endpointUriPart.length();
    }

    return endpointUriPart.substring(iFirst + 1, iSecond);
  }

  /**
   * Returns the nth index of a character in a String
   *
   * @param uriValue The URI value
   * @param delimiter the delimiter character
   * @param count The number of occurrences to skip.
   * @return the routing part of the URI
   */
  protected static int nthIndexOf(String uriValue, char delimiter, int count)
  {
    int index = -1;

    while (0 <= count)
    {
      index = uriValue.indexOf(delimiter, index + 1);

      if (-1 == index)
      {
        return index;
      }

      --count;
    }

    return index;
  }

  /**
   * Overridable internal method for the creation of endpoints.
   *
   * @param localName The local mal name to use.
   * @param routingName The local routing name to use.
   * @param qosProperties the QoS properties.
   * @return The new endpoint
   * @throws MALException on Error.
   */
  protected GENEndpoint internalCreateEndpoint(final String localName, final String routingName, final Map qosProperties) throws MALException
  {
    return new GENEndpoint(this, localName, routingName, uriBase + routingName, wrapBodyParts);
  }

  /**
   * This method checks if there is a communication channel for sending a particular message and in addition stores the
   * communication channel on incoming messages in case of bi-directional transports for re-use. If there is no communication
   * channel for sending a message the transport creates and registers it.
   *
   * @param msg The message received or to be sent
   * @param isIncomingMsgDirection the message direction
   * @param receptionHandler the message reception handler, null if the message is an outgoing message
   * @return returns an existing or newly created message sender
   * @throws MALTransmitErrorException in case of communication problems
   */
  protected synchronized GENConcurrentMessageSender manageCommunicationChannel(GENMessage msg, boolean isIncomingMsgDirection, GENReceptionHandler receptionHandler) throws MALTransmitErrorException
  {
    GENConcurrentMessageSender sender = null;

    if (isIncomingMsgDirection)
    {
      // incoming msg
      if ((null != receptionHandler) && (null == receptionHandler.getRemoteURI()))
      {
        // transport supports bi-directional communication
        // this is the first message received form this reception handler
        // add the remote base URI it is receiving messages from
        String sourceURI = msg.getHeader().getURIFrom().getValue();
        String sourceRootURI = getRootURI(sourceURI);

        receptionHandler.setRemoteURI(sourceRootURI);

        //register the communication channel with this URI if needed
        sender = registerMessageSender(receptionHandler.getMessageSender(), sourceRootURI);
      }
    }
    else
    {
      // outgoing message
      // get target URI
      String remoteRootURI = getRootURI(msg.getHeader().getURITo().getValue());

      // get sender if it exists
      sender = outgoingDataChannels.get(remoteRootURI);

      if (null == sender)
      {
        // we do not have any channel for this URI
        // try to create a set of connections to this URI 
        LOGGER.log(Level.INFO, "GEN received request to create connections to URI:{0}", remoteRootURI);

        try
        {
          // create new sender for this URI
          sender = registerMessageSender(createMessageSender(msg, remoteRootURI), remoteRootURI);

          LOGGER.log(Level.FINE, "GEN opening {0}", numConnections);

          for (int i = 1; i < numConnections; i++)
          {
            // insert new processor (message sender) to root data sender for the URI
            sender.addProcessor(createMessageSender(msg, remoteRootURI), remoteRootURI);
          }
        }
        catch (MALException e)
        {
          LOGGER.log(Level.WARNING, "GEN could not connect to :" + remoteRootURI, e);
          throw new MALTransmitErrorException(msg.getHeader(),
                  new MALStandardError(MALHelper.DESTINATION_UNKNOWN_ERROR_NUMBER, null), null);
        }
      }
    }

    return sender;
  }

  /**
   * Registers a message sender for a given root URI. If this is the first data sender for the URI, it also creates a
   * GENConcurrentMessageSender to manage all the senders. If there are already enough connections (numConnections) to the given URI
   * the method does not register the sender. This ensures that we will have at maximum numConnections to the target root URI.
   *
   * @param dataTransmitter The data sender that is able to send messages to the URI
   * @param remoteRootURI the remote root URI
   * @return returns the GENConcurrentMessageSender for this URI.
   */
  protected synchronized GENConcurrentMessageSender registerMessageSender(GENMessageSender dataTransmitter, String remoteRootURI)
  {
    //check if we already have a communication channel for this URI
    GENConcurrentMessageSender dataSender = outgoingDataChannels.get(remoteRootURI);
    if (dataSender != null)
    {
      //we already have a communication channel for this URI
      //check if we have enough connections for the URI, if not then add the data sender 
      if (dataSender.getNumberOfProcessors() < numConnections)
      {
        LOGGER.log(Level.FINE, "GEN registering data sender for URI:{0}", remoteRootURI);
        // insert new processor (message sender) to root data sender for the URI
        dataSender.addProcessor(dataTransmitter, remoteRootURI);
      }
    }
    else
    {
      //we do not have a communication channel, create a data sender manager and add the first data sender
      // create new sender manager for this URI
      LOGGER.log(Level.FINE, "GEN creating data sender manager for URI:{0}", remoteRootURI);
      dataSender = new GENConcurrentMessageSender(this, remoteRootURI);

      LOGGER.log(Level.FINE, "GEN registering data sender for URI:{0}", remoteRootURI);
      outgoingDataChannels.put(remoteRootURI, dataSender);

      // insert new processor (message sender) to root data sender for the URI
      dataSender.addProcessor(dataTransmitter, remoteRootURI);
    }

    return dataSender;
  }

  /**
   * Internal method for encoding the message.
   *
   * @param destinationRootURI The destination root URI.
   * @param destinationURI The complete destination URI.
   * @param multiSendHandle Handle for multi send messages.
   * @param lastForHandle true if last message in a multi send.
   * @param targetURI The target URI.
   * @param msg The message to send.
   * @return The message holder for the outgoing message.
   * @throws Exception if an error.
   */
  protected abstract GENOutgoingMessageHolder<O> internalEncodeMessage(final String destinationRootURI,
          final String destinationURI,
          final Object multiSendHandle,
          final boolean lastForHandle,
          final String targetURI,
          final GENMessage msg) throws Exception;

  /**
   * Internal method for encoding the message.
   *
   * @param destinationRootURI The destination root URI.
   * @param destinationURI The complete destination URI.
   * @param multiSendHandle Handle for multi send messages.
   * @param lastForHandle true if last message in a multi send.
   * @param targetURI The target URI.
   * @param msg The message to send.
   * @return The message holder for the outgoing message.
   * @throws MALTransmitErrorException if an error.
   */
  protected byte[] internalEncodeByteMessage(final String destinationRootURI,
          final String destinationURI,
          final Object multiSendHandle,
          final boolean lastForHandle,
          final String targetURI,
          final GENMessage msg) throws MALTransmitErrorException
  {
    // encode the message
    try
    {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final MALElementOutputStream enc = getStreamFactory().createOutputStream(baos);
      msg.encodeMessage(getStreamFactory(), enc, baos, true);
      byte[] data = baos.toByteArray();

      // message is encoded!
      LOGGER.log(Level.FINE, "GEN Sending data to {0} : {1}", new Object[]
      {
        targetURI, new PacketToString(data)
      });

      return data;
    }
    catch (MALException ex)
    {
      LOGGER.log(Level.SEVERE, "GEN could not encode message!", ex);
      throw new MALTransmitErrorException(msg.getHeader(), new MALStandardError(MALHelper.BAD_ENCODING_ERROR_NUMBER, null), null);
    }
  }

  /**
   * Creates the part of the URL specific to this transport instance.
   *
   * @return The transport specific address part.
   * @throws MALException On error
   */
  protected abstract String createTransportAddress() throws MALException;

  /**
   * Method to be implemented by the transport in order to return a message sender capable if sending messages to a target root URI.
   *
   * @param msg the message to be send
   * @param remoteRootURI the remote root URI.
   * @return returns a message sender capable of sending messages to the target URI
   * @throws MALException in case of error trying to create the communication channel
   * @throws MALTransmitErrorException in case of error connecting to the target URI
   */
  protected abstract GENMessageSender createMessageSender(GENMessage msg, String remoteRootURI) throws MALException, MALTransmitErrorException;

  /**
   * This Runnable task is responsible for decoding newly arrived MAL Messages and passing to the transport executor.
   */
  private static class GENIncomingMessageReceiver implements Runnable
  {
    protected final GENTransport transport;
    protected final GENReceptionHandler receptionHandler;
    protected final GENIncomingMessageDecoder decoder;

    /**
     * Constructor
     *
     * @param transport Containing transport.
     * @param receptionHandler The reception handler to pass them to.
     * @param decoder The class responsible for decoding the message from the incoming connection
     */
    protected GENIncomingMessageReceiver(final GENTransport transport,
            final GENReceptionHandler receptionHandler,
            final GENIncomingMessageDecoder decoder)
    {
      this.transport = transport;
      this.receptionHandler = receptionHandler;
      this.decoder = decoder;
    }

    /**
     * This method processes an incoming message and then forwards it for routing to the appropriate message queue. The processing
     * consists of transforming the raw message to the appropriate format and then registering if necessary the communication
     * channel.
     */
    @Override
    public void run()
    {
      try
      {
        GENIncomingMessageHolder msg = decoder.decodeAndCreateMessage();

        // the decoder may return null for transports that support fragmentation
        if (null != msg)
        {
          GENTransport.LOGGER.log(Level.FINE, "GEN Receving message : {0} : {1}", new Object[]
          {
            msg.malMsg.getHeader().getTransactionId(), msg.smsg
          });
          //register communication channel if needed
          transport.manageCommunicationChannel(msg.malMsg, true, receptionHandler);
          transport.receiveIncomingMessage(msg);
        }
      }
      catch (MALException e)
      {
        GENTransport.LOGGER.log(Level.WARNING, "GEN Error occurred when decoding data : {0}", e);
        transport.communicationError(null, receptionHandler);
      }
      catch (MALTransmitErrorException e)
      {
        GENTransport.LOGGER.log(Level.WARNING, "GEN Error occurred when decoding data : {0}", e);
        transport.communicationError(null, receptionHandler);
      }
    }
  }

  /**
   * This Runnable task is responsible for processing the already decoded message. It holds a queue of messages split on transaction
   * id so that messages with the same transaction id get processed in reception order.
   *
   */
  private final class GENIncomingMessageProcessor implements Runnable
  {
    private final Queue<GENIncomingMessageHolder> malMsgs = new ArrayDeque<GENIncomingMessageHolder>();
    private boolean finished = false;

    /**
     * Constructor
     *
     * @param malMsg The MAL message.
     */
    public GENIncomingMessageProcessor(final GENIncomingMessageHolder malMsg)
    {
      malMsgs.add(malMsg);
    }

    /**
     * Adds a message to the internal queue. If the thread associated with this executor has finished it resets the flag and returns
     * true to indicate that it should be resubmitted for more processing to the Executor pool.
     *
     * @param malMsg The decoded message.
     * @return True if this needs to be resubmitted to the processing executor pool.
     */
    public synchronized boolean addMessage(final GENIncomingMessageHolder malMsg)
    {
      malMsgs.add(malMsg);

      if (finished)
      {
        finished = false;

        // need to resubmit this to the processing threads
        return true;
      }

      return false;
    }

    /**
     * Returns true if this thread has finished processing its queue.
     *
     * @return True if finished processing queue.
     */
    public boolean isFinished()
    {
      return finished;
    }

    @Override
    public void run()
    {
      GENIncomingMessageHolder msg;

      synchronized (this)
      {
        msg = malMsgs.poll();
      }

      while (null != msg)
      {
        // send message for further processing and routing
        processIncomingMessage(msg.malMsg, msg.smsg);

        synchronized (this)
        {
          msg = malMsgs.poll();

          if (null == msg)
          {
            finished = true;
          }
        }
      }
    }
  }

  /**
   * Converts the packet to a string form for logging.
   *
   */
  public class PacketToString
  {
    private final byte[] data;
    private String str;

    /**
     * Constructor.
     *
     * @param data the packet.
     */
    public PacketToString(byte[] data)
    {
      this.data = data;
    }

    @Override
    public String toString()
    {
      if (null == str)
      {
        synchronized (this)
        {
          if (logFullDebug && null != data)
          {
            if (streamHasStrings)
            {
              str = new String(data, UTF8_CHARSET);
            }
            else
            {
              str = GENHelper.byteArrayToHexString(data);
            }
          }
          else
          {
            str = "";
          }
        }
      }

      return str;
    }
  }

  private static ExecutorService createThreadPoolExecutor(final java.util.Map properties)
  {
    boolean needsTuning = false;
    int lInputProcessorThreads = 100;
    int lMinInputProcessorThreads = lInputProcessorThreads;
    int lIdleTimeInSeconds = 0;

    if (null != properties)
    {
      // minium number of internal threads that process incoming MAL packets
      if (properties.containsKey(MIN_INPUT_PROCESSORS_PROPERTY))
      {
        needsTuning = true;
        lMinInputProcessorThreads = Integer.parseInt((String) properties.get(MIN_INPUT_PROCESSORS_PROPERTY));
      }

      // number of seconds for internal threads that process incoming MAL packets to be idle before being terminated
      if (properties.containsKey(IDLE_INPUT_PROCESSORS_PROPERTY))
      {
        needsTuning = true;
        lIdleTimeInSeconds = Integer.parseInt((String) properties.get(IDLE_INPUT_PROCESSORS_PROPERTY));
      }

      // number of internal threads that process incoming MAL packets
      if (properties.containsKey(INPUT_PROCESSORS_PROPERTY))
      {
        lInputProcessorThreads = Integer.parseInt((String) properties.get(INPUT_PROCESSORS_PROPERTY));
      }
    }

    ExecutorService rv = Executors.newFixedThreadPool(lInputProcessorThreads);

    // see if we can tune the thread pool
    if (needsTuning)
    {
      if (rv instanceof ThreadPoolExecutor)
      {
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) rv;

        tpe.setKeepAliveTime(lIdleTimeInSeconds, TimeUnit.SECONDS);
        tpe.setCorePoolSize(lMinInputProcessorThreads);
      }
    }

    return rv;
  }
}
