/* 
 * MAL/SPP Binding for CCSDS Mission Operations Framework
 * Copyright (C) 2015 Deutsches Zentrum für Luft- und Raumfahrt e.V. (DLR).
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.dlr.gsoc.mo.malspp.transport;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.broker.MALBrokerBinding;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.SessionType;
import org.ccsds.moims.mo.mal.structures.ShortList;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.transport.MALEndpoint;
import org.ccsds.moims.mo.mal.transport.MALMessage;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import org.ccsds.moims.mo.mal.transport.MALMessageListener;
import org.ccsds.moims.mo.mal.transport.MALTransmitErrorException;
import org.ccsds.moims.mo.mal.transport.MALTransport;
import org.ccsds.moims.mo.testbed.util.spp.SPPSocket;
import org.ccsds.moims.mo.testbed.util.spp.SPPSocketFactory;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;

public class SPPTransport implements MALTransport {

    private static final Logger LOGGER = Logger.getLogger(SPPTransport.class.getName());
    // Error strings
    private static final String ILLEGAL_NULL_ARGUMENT = "Argument may not be null.";
    private static final String IDENTIFIER_UNAVAILABLE = "Source or destination identifier not available.";
    private static final String TRANSPORT_CLOSED = "Trying to interact with a closed transport.";
    private static final String SPP_ERROR = "Error in Space Packet Protocol library.";
    private static final String SOCKET_ERROR = "Space Packet Socket error (ignore if socket closed on purpose).";
    private static final String THREAD_INTERRUPTED = "The Space Packet receive thread has been interrupted.";
    // Numeric constants
    private static final short SEQUENCE_COUNTER_WRAP = 16384;
    private static final long SEGMENT_COUNTER_WRAP = 4294967296L;
    protected static final byte MALSPP_VERSION = 0;
    protected static final byte SPP_VERSION = 0;
    protected static final int MAX_SPACE_PACKET_SIZE = 65536;
    // Member variables
    private final String protocol;
    private final Map properties;
    private final SPPSocket sppSocket;
    private final Map<String, SPPEndpoint> endpointsByName = new HashMap<>();
    private final Map<URI, SPPEndpoint> endpointsByURI = new HashMap<>();
    private final ShortList apids = new ShortList();
    private boolean isClosed;
    private Thread receiveThread; // is assigned on first endpoint creation
    private Thread messageHandlerThread; // is assigned on first endpoint creation

    // We need to set the initial capacity to have the MAL mixing messages from
    // different sources.
    // For example, if I do a heavy query, I don't want to have the queue full of
    // those messages,
    // but instead, a mix of those combined with other messages. This let's them
    // work in parallel.
    private final LinkedBlockingQueue<MALMessage> receivedMessages = new LinkedBlockingQueue<>(15);
    // private LinkedBlockingQueue<MALMessage> receivedMessages = new
    // LinkedBlockingQueue<>();

    private final HashMap<Long, LinkedBlockingQueue<MALMessage>> transMap = new HashMap<>();

    private final Map<SequenceCounterId, SPPCounter> sequenceCounters = new HashMap<>();
    private final Map<SequenceCounterId, Queue<Short>> identifiers = new HashMap<>();
    private final Map<SegmentCounterId, SPPCounter> segmentCounters = new HashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(6);
    private final Object MUTEX = new Object();

    public SPPTransport(final String protocol, final Map properties) throws MALException {
        try {
            sppSocket = SPPSocketFactory.newInstance().createSocket(properties);
        } catch (final Exception ex) {
            LOGGER.log(Level.WARNING, SPP_ERROR + " " + ex.getMessage(), ex);
            throw new MALException(SPP_ERROR + " " + ex.getMessage(), ex);
        }
        this.protocol = protocol;
        this.properties = properties;
        this.isClosed = false;
    }

    @Override
    public MALEndpoint createEndpoint(final String localName, final Map qosProperties) throws MALException {
        if (isClosed) {
            throw new MALException(TRANSPORT_CLOSED);
        }
        if (endpointsByName.containsKey(localName)) {
            final SPPEndpoint ep = endpointsByName.get(localName);
            if (ep.isClosed()) {
                ep.reopen();
            }
            return ep;
        }

        // PENDING: Not all needed endpoint QoS properties seem to be present in
        // qosProperties. As
        // it seems all properties read from the configuration file are put into the
        // transport QoS
        // property map. Passing of per-message QoS properties is unclear. Here: Merge
        // transport and
        // endpoint QoS properties for creation of new endpoint and message receiving
        // and handling.
        // Per-message QoS properties are defined in the per-transport properties as
        // well and
        // therefore merged as well. If a property is present in the per-transport and
        // in the
        // per-endpoint map, the latter will override the former.
        final Map props = Configuration.mix(properties, qosProperties);
        final Configuration config = new Configuration(props);

        // PENDING: appendIdToUri property not yet in specification.
        final Short identifier = config.appendIdToUri() ? claimIdentifier(config.qualifier(), config.apid(), config
            .numIdentifiers(), config.startIdentifier()) : null;

        SPPURI uri = null;

        if (localName != null) {
            try {
                uri = new SPPURI(new URI(localName));
            } catch (final java.lang.IllegalArgumentException ex) {
                // Do nothing!
            }
        }

        if (uri == null) {
            uri = new SPPURI(config.qualifier(), config.apid(), identifier);
        }

        synchronized (apids) {
            apids.add(config.apid());
        }

        // SPPURI uri = new SPPURI(config.qualifier(), config.apid(), identifier);

        final SPPEndpoint endpoint = new SPPEndpoint(protocol, this, localName, uri.getURI(), qosProperties, sppSocket);

        SPPEndpoint oldEndpoint = null;

        if (localName != null) {
            oldEndpoint = endpointsByName.put(localName, endpoint);

            if (null != oldEndpoint) {
                oldEndpoint.close();
                oldEndpoint = null;
            }
        }

        oldEndpoint = endpointsByURI.put(endpoint.getURI(), endpoint);

        if (null != oldEndpoint) {
            oldEndpoint.close();
            oldEndpoint = null;
        }

        synchronized (this) { // No need to make this more efficient; createEndpoint() is not called often.
            if (null == messageHandlerThread) {
                messageHandlerThread = constructMessageHandlerThread(props);
                messageHandlerThread.start();
            }
            if (null == receiveThread) {
                receiveThread = constructReceiveThread(sppSocket, props);
                receiveThread.start();
            }
        }
        return endpoint;
    }

    @Override
    public MALEndpoint getEndpoint(final String localName) throws IllegalArgumentException, MALException {
        if (isClosed) {
            throw new MALException(TRANSPORT_CLOSED);
        }
        if (null == localName) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        return endpointsByName.get(localName);
    }

    @Override
    public MALEndpoint getEndpoint(final URI uri) throws IllegalArgumentException, MALException {
        if (isClosed) {
            throw new MALException(TRANSPORT_CLOSED);
        }
        if (null == uri) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        return endpointsByURI.get(uri);
    }

    @Override
    public void deleteEndpoint(final String localName) throws IllegalArgumentException, MALException {
        if (isClosed) {
            throw new MALException(TRANSPORT_CLOSED);
        }
        if (null == localName) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        final SPPEndpoint endpoint = endpointsByName.get(localName);
        if (null != endpoint) {
            endpointsByName.remove(localName);
            endpoint.close();
            invalidateURI(endpoint.getURI());
        }
    }

    /**
     * Invalidates a URI by deleting references to an endpoint and freeing up its
     * identifier. References by its local name won't be deleted. If this is desired
     * it should be done before calling this method.
     *
     * @param uri The URI to be invalidated.
     * @throws MALException
     */
    protected void invalidateURI(final URI uri) throws MALException {
        endpointsByURI.remove(uri);
        final SPPURI sppURI = new SPPURI(uri);
        freeIdentifier(sppURI.getAPID(), sppURI.getQualifier(), sppURI.getIdentifier());
    }

    @Override
    public MALBrokerBinding createBroker(final String localName, final Blob authenticationId,
        final QoSLevel[] expectedQos, final UInteger priorityLevelNumber, final Map defaultQosProperties)
        throws IllegalArgumentException, MALException {
        if (isClosed) {
            throw new MALException(TRANSPORT_CLOSED);
        }
        // Tansport level broker is not supoorted.
        return null;
    }

    @Override
    public MALBrokerBinding createBroker(final MALEndpoint endpoint, final Blob authenticationId,
        final QoSLevel[] expectedQos, final UInteger priorityLevelNumber, final Map defaultQosProperties)
        throws IllegalArgumentException, MALException {
        if (isClosed) {
            throw new MALException(TRANSPORT_CLOSED);
        }
        // Tansport level broker is not supoorted.
        return null;
    }

    @Override
    public boolean isSupportedQoSLevel(final QoSLevel qos) {
        // PENDING: SPP implementation: Get the supported QoS levels from the underlying
        // transport
        // layer. This should be implemented in the SPP socket layer. Here: Return true
        // for all
        // QoS levels.
        return true;
    }

    @Override
    public boolean isSupportedInteractionType(final InteractionType type) {
        return type.getOrdinal() != InteractionType._PUBSUB_INDEX;
    }

    @Override
    synchronized public void close() throws MALException {
        for (final SPPEndpoint endpoint : new ArrayList<>(endpointsByURI.values())) {
            final String localName = endpoint.getLocalName();
            if (localName != null) {
                deleteEndpoint(localName);
            } else {
                endpoint.close();
            }
        }
        isClosed = true;
        endpointsByName.clear();
        endpointsByURI.clear();
        if (null != receiveThread) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        if (null != messageHandlerThread) {
            messageHandlerThread.interrupt();
            messageHandlerThread = null;
        }
        try {
            sppSocket.close();
        } catch (final Exception ex) {
            LOGGER.log(Level.WARNING, SOCKET_ERROR, ex);
            throw new MALException(SOCKET_ERROR, ex);
        }
    }

    protected boolean isClosed() {
        return isClosed;
    }

    protected Map getProperties() {
        return properties;
    }

    /**
     * Like claimIdentifier(apid, qualifier, id, numIdentifiers, startIdentifier)
     * with id == null, i.e. an arbitrary available identifier is returned.
     *
     * @param qualifier
     * @param apid
     * @param numIdentifiers
     * @param startIdentifier
     * @return
     * @throws org.ccsds.moims.mo.mal.MALException
     */
    private short claimIdentifier(final int qualifier, final short apid, final short numIdentifiers,
        final short startIdentifier) throws MALException {
        return claimIdentifier(qualifier, apid, null, numIdentifiers, startIdentifier);
    }

    /**
     * Claims an unused identifier for usage in URIs as source or destination
     * identifier.
     *
     * @param qualifier       APID qualifier for identification of the correct
     *                        identifier queue.
     * @param apid            APID for identification of the correct identifier
     *                        queue.
     * @param id              The specific identifier to claim. Pass null if an
     *                        aribtrary unused identifier between 0 and
     *                        numIdentifiers - 1 shall be claimed. For each
     *                        combination of APID and APID qualifier the identifier
     *                        to be returned when null is passed in is 0 upon first
     *                        invocation of this method.
     * @param numIdentifiers  Number of adjacent identifiers that will be put in the
     *                        identifier pool. This parameter is only used to fill
     *                        the identifier pool upon first use for each APID and
     *                        APID qualifier, it is ignored for subseqeuent calls.
     * @param startIdentifier The first identifier that is allowed to be claimed.
     *                        Like numbIdentifiers this parameter is used to fill
     *                        the pool upon first invocation for each APID and APID
     *                        qualifier.
     * @return The specified identifier or an arbitrary unused identifier between 0
     *         and numIdentifiers - 1 if none was specified. For each combination of
     *         APID and APID qualifier the identifier to be returned when none was
     *         specified is 0 upon first invocation of this method. If the specified
     *         one/none is available a MALException is thrown. The claimed
     *         identifier is not available for further claims except it is freed
     *         with freeIdentifier(). Each combination of APID and APID qualifier
     *         holds its own queue of possible identifiers.
     * @throws MALException
     */
    private short claimIdentifier(final int qualifier, final short apid, final Short id, final short numIdentifiers,
        final short startIdentifier) throws MALException {
        final SequenceCounterId counterId = new SequenceCounterId(qualifier, apid);
        Queue<Short> ids = identifiers.get(counterId);
        if (null == ids) {
            final Short[] pool = new Short[numIdentifiers];
            for (int i = 0; i < numIdentifiers; i++) { // create pool of valid identifiers
                pool[i] = (short) (i + startIdentifier);
            }
            ids = new ArrayBlockingQueue<>(numIdentifiers, false, Arrays.asList(pool));
            identifiers.put(counterId, ids);
        }
        if (null == id) {
            try {
                return ids.remove();
            } catch (final java.util.NoSuchElementException ex) {
                throw new MALException(IDENTIFIER_UNAVAILABLE, ex);
            }
        }
        if (!ids.remove(id)) {
            throw new MALException(IDENTIFIER_UNAVAILABLE);
        }
        return id;
    }

    /**
     * Frees a previously claimed identifier and makes it availabe for future
     * claims. If the id was not in use before, the method does nothing.
     *
     * @param apid      APID for identification of the correct identifier queue.
     * @param qualifier APID qualifier for identification of the correct identifier
     *                  queue.
     * @param id        The identifier to free.
     */
    private void freeIdentifier(final short apid, final Integer qualifier, final short id) {
        final SequenceCounterId counterId = new SequenceCounterId(qualifier, apid);
        final Queue<Short> ids = identifiers.get(counterId);
        if (null != ids && !ids.contains(id)) {
            ids.add(id);
        }
    }

    /**
     * Listens to an SPPSocket and delivers the received messages to the appropriate
     * endpoint's listener. This method is executed in loop for each receive thread.
     *
     * @param sppSocket     SPP socket to listen to.
     * @param qosProperties QoS properties.
     * @param segmenters    Segmenters responsible for reconstructing segmented
     *                      Space qosProperties.
     * @param currentThread Current thread, in which the receive() method is
     *                      executed.
     */
    private MALMessage receive(final SPPSocket sppSocket, final Map qosProperties,
        final Map<SegmentCounterId, SPPSegmenter> segmenters, final Thread currentThread) {
        // TODO: Queue received messages for stopped delivery and QoS level QUEUED.

        try {
            final SpacePacket spacePacket = sppSocket.receive(); // blocks until a space packet has been received
            if (spacePacket == null) {
                LOGGER.log(Level.FINE, "Discarding message as it is not inside the whitelist.");
                return null;
            }
            if (spacePacket.getHeader().getSecondaryHeaderFlag() != 1) {
                LOGGER.log(Level.FINE, "Discarding message as it has no secondary header.");
                return null;
            }
            // PENDING: SPP TCP implementation allocates a new Space Packet with a body size
            // of
            // 65536 bytes. If the received Space Packet is smaller, the body byte array is
            // not
            // trimmed to fit. Here: Create new byte array of right size, copy contents, and
            // set
            // array as new body of the Space Packet.

            /*
             * byte[] trimmedBody = new byte[spacePacket.getLength()];
             * System.arraycopy(spacePacket.getBody(), 0, trimmedBody, 0,
             * spacePacket.getLength()); spacePacket.setBody(trimmedBody);
             */

            // retrieve effective QoS properties resolving per-application parameters
            final Configuration config = new Configuration(qosProperties);
            // short apid = config.apid();

            short apid;

            final Map effectiveProperties = config.getEffectiveProperties(spacePacket.getApidQualifier(),
                (short) spacePacket.getHeader().getApid());
            // Selection of correct segment counter needs MAL header information.
            final MALElementStreamFactory esf = MALElementStreamFactory.newFactory(protocol, effectiveProperties);
            final SPPMessageHeader messageHeader = new SPPMessageHeader(spacePacket, esf, effectiveProperties);

            // We need to iterate between all the available endpoints before discarding
            // it...
            boolean discard = true;

            final short from = messageHeader.getSPPURIFrom().getAPID();
            final short to = messageHeader.getSPPURITo().getAPID();

            // Iterate through all the endpoints
            /*
             * for (Map.Entry<URI, SPPEndpoint> entry : endpointsByURI.entrySet()) { URI key
             * = entry.getKey(); // SPPEndpoint value = entry.getValue();
             * 
             * // Get the APID from the key: String[] strs = key.getValue().split("/"); apid
             * = Short.parseShort(strs[1]);
             * 
             * // Don't discard if one of the enpoint apids is the from or to apid if(from
             * == apid || to == apid){ discard = false; break; } }
             */
            // Iterate through all the apids
            synchronized (apids) {
                for (int i = 0; i < apids.size(); i++) {
                    apid = apids.get(i);

                    // Don't discard if one of the enpoint apids is the from or to apid
                    if (from == apid || to == apid) {
                        discard = false;
                        break;
                    }
                }
            }

            if (discard) {
                LOGGER.log(Level.FINE, "Discarding message...");
                return null;
            }

            final SegmentCounterId segmentCounterId = new SegmentCounterId(messageHeader);
            SPPSegmenter segmenter;

            segmenter = segmenters.get(segmentCounterId);
            if (null == segmenter) {
                segmenter = new SPPSegmenter(config.timeout());
                segmenters.put(segmentCounterId, segmenter);
                // TODO: Delete segmenter when it is no longer needed, otherwise memory runs
                // full.
            }

            segmenter.process(spacePacket);
            if (!segmenter.hasNext()) {
                return null;
            }

            return new SPPMessage(messageHeader, segmenter.next(), effectiveProperties, qosProperties, esf, this);
        } catch (final SocketException ex) {
            LOGGER.log(Level.SEVERE, SOCKET_ERROR, ex);
            currentThread.interrupt();
        } catch (final InterruptedException ex) {
            LOGGER.log(Level.INFO, THREAD_INTERRUPTED);
            currentThread.interrupt();
        } catch (final Exception ex) {
            // TODO: Is there any other way of handling reception exceptions?
            LOGGER.log(Level.WARNING, SPP_ERROR, ex);
        }
        return null;
    }

    private void handleReceivedMessage(final MALMessage msg, final Map qosProperties) {
        try {
            final URI uriTo = msg.getHeader().getURITo();
            final SPPEndpoint targetEndpoint = (SPPEndpoint) getEndpoint(uriTo);
            if (targetEndpoint == null) {
                // Endpoint referenced in message not known. Choose a different endpoint for the
                // sole purpose of returning an error message.
                final Collection<SPPEndpoint> possibleEPs = endpointsByURI.values();
                if (!possibleEPs.isEmpty()) {
                    // TODO: EP could be closed. Should we choose a different one?
                    final SPPEndpoint alternativeEndpoint = possibleEPs.iterator().next();
                    final MALStandardError error = new MALStandardError(MALHelper.DESTINATION_UNKNOWN_ERROR_NUMBER,
                        null);
                    sendErrorMessage(alternativeEndpoint, msg, error, uriTo);
                } else {
                    // No endpoint exists, thus error cannot be returned. Just log it instead.
                    // One could investigate the possibility to temporarily create an endpoint.
                    LOGGER.log(Level.WARNING, IDENTIFIER_UNAVAILABLE);
                }
                return;
            }

            final MALMessageListener listener = targetEndpoint.getMessageListener();
            if (listener == null) {
                final MALStandardError error = new MALStandardError(MALHelper.DELIVERY_FAILED_ERROR_NUMBER, null);
                sendErrorMessage(targetEndpoint, msg, error, null);
                return;
            }
            if (targetEndpoint.isDeliveryStopped()) {
                final MALStandardError error = new MALStandardError(MALHelper.DELIVERY_FAILED_ERROR_NUMBER, null);
                sendErrorMessage(targetEndpoint, msg, error, null);
            } else {
                /*
                 * LOGGER.log(Level.INFO, "\n" + "Local Name: " + targetEndpoint.getLocalName()
                 * + "\nURI: " + targetEndpoint.getURI() + "\n\nInteractionType: " +
                 * msg.getHeader().getInteractionType().toString() + "\nDomain: " +
                 * msg.getHeader().getDomain() + "\nInteractionStage: " +
                 * msg.getHeader().getInteractionStage().toString() + "\nIsErrorMessage: " +
                 * msg.getHeader().getIsErrorMessage() + "\nNetworkZone: " +
                 * msg.getHeader().getNetworkZone() + "\nTransactionId: " +
                 * msg.getHeader().getTransactionId() + "\nTimestamp: " +
                 * msg.getHeader().getTimestamp() + "\n");
                 */

                /*
                 * Thread thread = new Thread() {
                 * 
                 * @Override public void run() { this.setName("ListenerThread_malspp");
                 * listener.onMessage(targetEndpoint, msg); } }; thread.start();
                 */

                /*
                 * Logger.getLogger(SPPTransport.class.getName()).log(Level.INFO, "Body:\n" +
                 * Arrays.toString(msg.getBody().getEncodedBody().getEncodedBody().getValue()));
                 */

                // targetEndpoint.shipMessage(msg);
                listener.onMessage(targetEndpoint, msg);
                /*
                 * executor.submit(new Runnable(){
                 * 
                 * @Override public void run() { targetEndpoint.shipMessage(msg); } });
                 */
            }
        } catch (final Exception ex) {
            // TODO: Is there any other way of handling reception exceptions?
            LOGGER.log(Level.WARNING, SPP_ERROR, ex);
        }
    }

    /**
     * Creates and send an error message in reply to another MAL message. If
     * returning an error message is not allowed by the interaction type and stage
     * of the original MAL message, then no message is sent and a warning is written
     * to the log.
     *
     * @param targetEndpoint The endpoint to use for sending the error message.
     * @param replyToMsg     The original MAL message, on which the error message is
     *                       a reply. The sender of this message is the destination
     *                       of the error message.
     * @param error          The error object with the extraInformation field being
     *                       of one of the MAL element types.
     * @param uriFrom        The URI used for the sender. If null, the URI of the
     *                       endpoint is used, otherwise the endpoint URI won't be
     *                       considered for this error message and is overwritten.
     * @throws MALException
     * @throws MALTransmitErrorException
     */
    private void sendErrorMessage(final SPPEndpoint targetEndpoint, final MALMessage replyToMsg,
        final MALStandardError error, final URI uriFrom) throws MALException, MALTransmitErrorException {
        final MALMessage errMsg = targetEndpoint.createErrorMessage(replyToMsg, error, uriFrom);
        if (errMsg != null) {
            targetEndpoint.sendMessage(errMsg);
        } else {
            LOGGER.log(Level.WARNING, "Error: {0} could not answer to {1}", new Object[]{error.toString(), replyToMsg
                .getHeader()});
        }
    }

    /**
     * Constructs a receive thread associated with the socket, when a new socket
     * needs to be created. The thread is not started.
     *
     * @param socket        The SPP socket the receive thread shall listen to.
     * @param qosProperties QoS properties.
     * @return The newly created receive thread.
     */
    private Thread constructReceiveThread(final SPPSocket socket, final Map qosProperties) throws MALException {
        return new Thread() {
            private final Map<SegmentCounterId, SPPSegmenter> segmenters = new HashMap<>();

            @Override
            public void run() {
                this.setName("ReceiveThread_malspp");
                while (!isInterrupted()) {
                    final MALMessage msg = receive(socket, qosProperties, segmenters, this);
                    if (null != msg) {
                        try {
                            receivedMessages.put(msg);
                        } catch (final InterruptedException ex) {
                            LOGGER.log(Level.INFO, THREAD_INTERRUPTED, ex);
                            break;
                        }
                    }
                }
            }
        };
    }

    /**
     * Constructs a message handler thread working through the list of received and
     * injected messages. The thread is not started.
     *
     * @param qosProperties QoS properties.
     * @return The newly created message handler thread.
     * @throws MALException
     */
    private Thread constructMessageHandlerThread(final Map qosProperties) throws MALException {
        return new Thread() {
            @Override
            public void run() {
                this.setName("MessageHandlerThread_malspp");
                while (!isInterrupted()) {
                    try {
                        final MALMessage msg = receivedMessages.take();

                        final Long transId = msg.getHeader().getTransactionId();

                        LinkedBlockingQueue<MALMessage> msgs = null;

                        synchronized (MUTEX) {
                            msgs = transMap.get(transId);

                            if (msgs != null) {
                                msgs.add(msg);
                                continue;
                            }
                        }

                        msgs = new LinkedBlockingQueue<>();
                        msgs.add(msg);
                        transMap.put(transId, msgs);

                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                final LinkedBlockingQueue<MALMessage> msgsIn = transMap.get(transId);

                                MALMessage msg = msgsIn.poll();
                                while (msg != null) {
                                    handleReceivedMessage(msg, qosProperties);

                                    synchronized (MUTEX) {
                                        msg = msgsIn.poll();

                                        if (msg == null) {
                                            transMap.remove(transId);
                                        }
                                    }
                                }
                            }
                        });

                    } catch (final InterruptedException ex) {
                        LOGGER.log(Level.INFO, THREAD_INTERRUPTED);
                        break;
                    }
                }
            }
        };
    }

    /**
     * Injects a message in the list of received messages. This is useful if a
     * message is not received on the SPP socket but shall be put in the message
     * queue by some other means (e.g. because it has been dispatched in the same
     * process). You need to make sure the message is usable in the same way as if
     * it was received on the socket.
     *
     * @param msg The MAL message to be injected.
     * @throws InterruptedException
     */
    protected void injectReceivedMessage(final MALMessage msg) throws InterruptedException {
        receivedMessages.put(msg);
    }

    /**
     * Finds the sequence counter belonging to a specific APID/APID qualifier
     * combination.
     *
     * Each APID for each each APID qualifier has its own sequence counter
     * associated. If the counter does not exist, it will be created, starting from
     * 0. Once a counter is created, it won't be deleted unless the transport is
     * deleted. This should not pose a memory issue as the number of these counters
     * is expected to be low and stay rather constant during the transport
     * lifecycle.
     *
     * @param qualifier The API qualifier which together with the APID uniquely
     *                  identifies the counter.
     * @param apid      The APID which together with the qualifier will uniquely
     *                  identify the counter.
     * @return The sequence counter associated with the APID/APID qualifier
     *         combination. A new sequence counter starting from 0 is created, if
     *         none exists.
     */
    protected SPPCounter getSequenceCounter(final int qualifier, final short apid) {
        final SequenceCounterId counterId = new SequenceCounterId(qualifier, apid);
        SPPCounter counter;
        synchronized (sequenceCounters) {
            counter = sequenceCounters.get(counterId);
            if (null == counter) {
                counter = new SPPCounter(SEQUENCE_COUNTER_WRAP);
                sequenceCounters.put(counterId, counter);
            }
        }
        return counter;
    }

    /**
     * Finds the segment counter belonging to a specific combination of MAL message
     * header fields.
     *
     * Each combination of 'Interaction Type', 'Transaction Id', 'URI From', 'URI
     * To', 'Session', 'Session Name', 'Domain', 'Network', 'Service Area',
     * 'Service' and 'Operation' has its own segment counter associated. If the
     * counter does not exist, it will be created, starting from 0. The counter is
     * only incremented for messages that need to be split because they are too
     * large for a single Space Packet.
     *
     * This means for each new interaction a new counter will be created, possibly
     * leading to running out of memory. One way to fix this would be to delete the
     * counter after completion of the interaction. However, this is not implemented
     * at the moment.
     *
     * @param header The MAL message header used to derive a unique identifier for
     *               the segment counter from.
     * @return The segment counter associated with the unique header fields
     *         combination. A new segment counter starting from 0 is created, if
     *         none exists.
     */
    protected SPPCounter getSegmentCounter(final MALMessageHeader header) {
        // TODO: Delete counter after completion of the interaction to prevent high
        // memory usage.
        final SegmentCounterId counterId = new SegmentCounterId(header);
        SPPCounter counter;
        synchronized (segmentCounters) {
            counter = segmentCounters.get(counterId);
            if (null == counter) {
                counter = new SPPCounter(SEGMENT_COUNTER_WRAP);
                segmentCounters.put(counterId, counter);
            }
        }
        return counter;
    }

    /**
     * Class for uniquely identifying a sequence counter through a URI.
     *
     * The policy is to have one sequence counter for each APID for each APID
     * qualifier.
     */
    protected static class SequenceCounterId {

        private final short apid;
        private final int qualifier;

        SequenceCounterId(final SPPURI uri) {
            this.apid = uri.getAPID();
            this.qualifier = uri.getQualifier();
        }

        SequenceCounterId(final int qualifier, final short apid) {
            this.apid = apid;
            this.qualifier = qualifier;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + this.apid;
            hash = 89 * hash + this.qualifier;
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SequenceCounterId other = (SequenceCounterId) obj;
            if (this.apid != other.apid) {
                return false;
            }
            return this.qualifier == other.qualifier;
        }

    }

    /**
     * Class for uniquely identifying a segment counter through a URI.
     *
     * The policy is to have one segment counter for each combination of Interaction
     * Type, Transaction Id, URI From, URI To, Session, Session Name, Domain,
     * Network, Service Area, Service, Operation.
     */
    protected static class SegmentCounterId {

        private final InteractionType interactionType;
        private final Long transactionId;
        private final URI uriFrom;
        private final URI uriTo;
        private final SessionType session;
        private final Identifier sessionName;
        private final IdentifierList domain;
        private final Identifier network;
        private final UShort serviceArea;
        private final UShort service;
        private final UShort operation;

        SegmentCounterId(final MALMessageHeader header) {
            interactionType = header.getInteractionType();
            transactionId = header.getTransactionId();
            uriFrom = header.getURIFrom();
            uriTo = header.getURITo();
            session = header.getSession();
            sessionName = header.getSessionName();
            domain = header.getDomain();
            network = header.getNetworkZone();
            serviceArea = header.getServiceArea();
            service = header.getService();
            operation = header.getOperation();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.interactionType);
            hash = 53 * hash + Objects.hashCode(this.transactionId);
            hash = 53 * hash + Objects.hashCode(this.uriFrom);
            hash = 53 * hash + Objects.hashCode(this.uriTo);
            hash = 53 * hash + Objects.hashCode(this.session);
            hash = 53 * hash + Objects.hashCode(this.sessionName);
            hash = 53 * hash + Objects.hashCode(this.domain);
            hash = 53 * hash + Objects.hashCode(this.network);
            hash = 53 * hash + Objects.hashCode(this.serviceArea);
            hash = 53 * hash + Objects.hashCode(this.service);
            hash = 53 * hash + Objects.hashCode(this.operation);
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SegmentCounterId other = (SegmentCounterId) obj;
            if (!Objects.equals(this.interactionType, other.interactionType)) {
                return false;
            }
            if (!Objects.equals(this.transactionId, other.transactionId)) {
                return false;
            }
            if (!Objects.equals(this.uriFrom, other.uriFrom)) {
                return false;
            }
            if (!Objects.equals(this.uriTo, other.uriTo)) {
                return false;
            }
            if (!Objects.equals(this.session, other.session)) {
                return false;
            }
            if (!Objects.equals(this.sessionName, other.sessionName)) {
                return false;
            }
            if (!Objects.equals(this.domain, other.domain)) {
                return false;
            }
            if (!Objects.equals(this.network, other.network)) {
                return false;
            }
            if (!Objects.equals(this.serviceArea, other.serviceArea)) {
                return false;
            }
            if (!Objects.equals(this.service, other.service)) {
                return false;
            }
            return Objects.equals(this.operation, other.operation);
        }

    }
}
