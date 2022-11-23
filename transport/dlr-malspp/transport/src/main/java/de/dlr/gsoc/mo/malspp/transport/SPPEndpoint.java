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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALInvokeOperation;
import org.ccsds.moims.mo.mal.MALOperation;
import org.ccsds.moims.mo.mal.MALProgressOperation;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.MALRequestOperation;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.MALSubmitOperation;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.SessionType;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.transport.MALEncodedBody;
import org.ccsds.moims.mo.mal.transport.MALEndpoint;
import org.ccsds.moims.mo.mal.transport.MALMessage;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import org.ccsds.moims.mo.mal.transport.MALMessageListener;
import org.ccsds.moims.mo.mal.transport.MALTransmitErrorException;
import org.ccsds.moims.mo.mal.transport.MALTransmitMultipleErrorException;
import org.ccsds.moims.mo.testbed.util.spp.SPPSocket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;

public class SPPEndpoint implements MALEndpoint {

    private static final String ILLEGAL_NULL_ARGUMENT = "Argument may not be null.";
    private static final String ENDPOINT_CLOSED = "Trying to interact with a closed endpoint.";
    private static final String HEADER_FIELD_IS_NULL = "At least one non-nullable MAL header field is null.";
    private static final String INVALID_URI = "Invalid URI format.";
    private final URI uri;
    private final String protocol;
    private final SPPTransport transport;
    private final String localName;
    private final Map qosProperties;
    private final Map effectiveQosProperties;
    private final SPPSocket sppSocket;
    private MALMessageListener listener;
    private boolean isClosed;
    private boolean isDeliveryStopped;
    // private final LinkedBlockingQueue<SpacePacket[]> outgoingQueue;
    // private final Thread outgoingThread;

    public SPPEndpoint(final String protocol, final SPPTransport transport, final String localName, final URI uri,
        final Map qosProperties, final SPPSocket sppSocket) throws MALException {
        this.protocol = protocol;
        this.transport = transport;
        // PENDING: Testbed throws NullpointerExceptions, if an endpoint, that is
        // created with
        // null as local name actually returns null when asked for the name. Here: Set
        // the hash code
        // as local name.
        this.localName = localName == null ? Integer.toString(hashCode()) : localName;
        this.qosProperties = qosProperties;
        this.effectiveQosProperties = Configuration.mix(transport.getProperties(), qosProperties);

        this.sppSocket = sppSocket;
        this.uri = uri;
        this.isClosed = false;
        this.isDeliveryStopped = true;
        // this.outgoingQueue = new LinkedBlockingQueue<SpacePacket[]>(5);
        // this.outgoingThread = this.constructOutgoingThread();
        // this.outgoingThread.start();
    }

    @Override
    public void startMessageDelivery() throws MALException {
        if (isClosed) {
            throw new MALException(ENDPOINT_CLOSED);
        }
        isDeliveryStopped = false;
    }

    @Override
    public void stopMessageDelivery() throws MALException {
        if (isClosed) {
            return;
            // throw new MALException(ENDPOINT_CLOSED);
        }
        isDeliveryStopped = true;
    }

    /*
     * private Thread constructOutgoingThread() { return new Thread() {
     * 
     * @Override public void run() { this.setName("OutgoingThread_malspp");
     * 
     * while (!isInterrupted()) { final SpacePacket[] spacePackets;
     * 
     * try { spacePackets = outgoingQueue.take();
     * 
     * if (null != spacePackets) { for (SpacePacket sp : spacePackets) { try {
     * sppSocket.send(sp); } catch (Exception ex) {
     * Logger.getLogger(SPPEndpoint.class.getName()).log(Level.SEVERE, null, ex); }
     * } } } catch (InterruptedException ex) {
     * Logger.getLogger(SPPEndpoint.class.getName()).log(Level.SEVERE, null, ex); }
     * } } }; }
     */

    // <editor-fold defaultstate="collapsed" desc="public MALMessage
    // createMessage(...) - 4x + 2x">
    @Override
    public MALMessage createMessage(final Blob authenticationId, final URI uriTo, final Time timestamp,
        final QoSLevel qosLevel, final UInteger priority, final IdentifierList domain, final Identifier networkZone,
        final SessionType session, final Identifier sessionName, final InteractionType interactionType,
        final UOctet interactionStage, final Long transactionId, final UShort serviceArea, final UShort service,
        final UShort operation, final UOctet areaVersion, final Boolean isErrorMessage, final Map qosProperties,
        final Object... body) throws IllegalArgumentException, MALException {
        if (isClosed) {
            throw new MALException(ENDPOINT_CLOSED);
        }
        final SPPMessageHeader msgHeader = new SPPMessageHeader(uri, authenticationId, uriTo, timestamp, qosLevel,
            priority, domain, networkZone, session, sessionName, interactionType, interactionStage, transactionId,
            serviceArea, service, operation, areaVersion, isErrorMessage);
        final MALOperation op = MALContextFactory.lookupArea(serviceArea, areaVersion).getServiceByNumber(service)
            .getOperationByNumber(operation);
        return createMessage(uri, uriTo, msgHeader, op, body, null, false, qosProperties);
    }

    @Override
    public MALMessage createMessage(final Blob authenticationId, final URI uriTo, final Time timestamp,
        final QoSLevel qosLevel, final UInteger priority, final IdentifierList domain, final Identifier networkZone,
        final SessionType session, final Identifier sessionName, final InteractionType interactionType,
        final UOctet interactionStage, final Long transactionId, final UShort serviceArea, final UShort service,
        final UShort operation, final UOctet areaVersion, final Boolean isErrorMessage, final Map qosProperties,
        final MALEncodedBody encodedBody) throws IllegalArgumentException, MALException {
        if (isClosed) {
            throw new MALException(ENDPOINT_CLOSED);
        }
        final SPPMessageHeader msgHeader = new SPPMessageHeader(uri, authenticationId, uriTo, timestamp, qosLevel,
            priority, domain, networkZone, session, sessionName, interactionType, interactionStage, transactionId,
            serviceArea, service, operation, areaVersion, isErrorMessage);
        final MALOperation op = MALContextFactory.lookupArea(serviceArea, areaVersion).getServiceByNumber(service)
            .getOperationByNumber(operation);
        return createMessage(uri, uriTo, msgHeader, op, null, encodedBody, true, qosProperties);
    }

    @Override
    public MALMessage createMessage(final Blob authenticationId, final URI uriTo, final Time timestamp,
        final QoSLevel qosLevel, final UInteger priority, final IdentifierList domain, final Identifier networkZone,
        final SessionType session, final Identifier sessionName, final Long transactionId, final Boolean isErrorMessage,
        final MALOperation op, final UOctet interactionStage, final Map qosProperties, final Object... body)
        throws IllegalArgumentException, MALException {
        if (op == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        if (isClosed) {
            throw new MALException(ENDPOINT_CLOSED);
        }
        final SPPMessageHeader msgHeader = new SPPMessageHeader(uri, authenticationId, uriTo, timestamp, qosLevel,
            priority, domain, networkZone, session, sessionName, op.getInteractionType(), interactionStage,
            transactionId, op.getService().getArea().getNumber(), op.getService().getNumber(), op.getNumber(), op
                .getService().getArea().getVersion(), isErrorMessage);
        return createMessage(uri, uriTo, msgHeader, op, body, null, false, qosProperties);
    }

    @Override
    public MALMessage createMessage(final Blob authenticationId, final URI uriTo, final Time timestamp,
        final QoSLevel qosLevel, final UInteger priority, final IdentifierList domain, final Identifier networkZone,
        final SessionType session, final Identifier sessionName, final Long transactionId, final Boolean isErrorMessage,
        final MALOperation op, final UOctet interactionStage, final Map qosProperties, final MALEncodedBody encodedBody)
        throws IllegalArgumentException, MALException {
        if (op == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        if (isClosed) {
            throw new MALException(ENDPOINT_CLOSED);
        }
        final SPPMessageHeader msgHeader = new SPPMessageHeader(uri, authenticationId, uriTo, timestamp, qosLevel,
            priority, domain, networkZone, session, sessionName, op.getInteractionType(), interactionStage,
            transactionId, op.getService().getArea().getNumber(), op.getService().getNumber(), op.getNumber(), op
                .getService().getArea().getVersion(), isErrorMessage);
        return createMessage(uri, uriTo, msgHeader, op, null, encodedBody, true, qosProperties);
    }

    private MALMessage createMessage(final URI uriFrom, final URI uriTo, final SPPMessageHeader msgHeader,
        final MALOperation op, final Object[] body, final MALEncodedBody encBody, final boolean isEncoded,
        final Map qosProperties) throws IllegalArgumentException, MALException {
        // get effective properties, resolving per-application parameters
        final Configuration config = new Configuration(Configuration.mix(this.effectiveQosProperties, qosProperties));
        final SPPURI primarySPPURI = new SPPURI(config.isTCpacket() ? uriTo : uriFrom);
        final Map props = config.getEffectiveProperties(primarySPPURI.getQualifier(), primarySPPURI.getAPID());
        final MALEncodingContext ctx = new MALEncodingContext(msgHeader, op, -1, this.effectiveQosProperties, props);
        final MALElementStreamFactory esf = MALElementStreamFactory.newFactory(protocol, props);
        final SPPMessageBody msgBody = isEncoded ? createMessageBody(encBody, esf, ctx) : createMessageBody(body, esf,
            ctx);
        return new SPPMessage(msgHeader, msgBody, props, this.qosProperties, esf, transport);
    }
    // </editor-fold>

    /**
     * Creates an error message in reply to another MAL message. If returning an
     * error message is not allowed by the interaction type and stage of the
     * original MAL message, then null is returned.
     *
     * @param replyToMsg The original MAL message, on which the error message is a
     *                   reply. The sender of this message is the destination of the
     *                   error message.
     * @param error      The error object with the extraInformation field being of
     *                   one of the MAL element types.
     * @param uriFrom    The URI used for the sender. If null, the URI of this
     *                   endpoint is used, otherwise the endpoint URI won't be
     *                   considered for this error message and is overwritten.
     * @return A MALMessage representing the error, with the isErrorMessage flag
     *         set, the timestamp of the error generation and the correct
     *         interaction stage for returning an error. Null, if it is not allowed
     *         to return a message.
     * @throws MALException
     */
    protected MALMessage createErrorMessage(final MALMessage replyToMsg, final MALStandardError error,
        final URI uriFrom) throws MALException {
        final MALMessageHeader header = replyToMsg.getHeader();
        final int type = header.getInteractionType().getOrdinal();
        final short stage = header.getInteractionStage().getValue();

        // Find out if current interaction allows returning an error message.
        final boolean isErrorAllowed = ((type == InteractionType._SUBMIT_INDEX && stage ==
            MALSubmitOperation._SUBMIT_STAGE) || (type == InteractionType._REQUEST_INDEX && stage ==
                MALRequestOperation._REQUEST_STAGE) || (type == InteractionType._INVOKE_INDEX && stage ==
                    MALInvokeOperation._INVOKE_STAGE) || (type == InteractionType._PROGRESS_INDEX && stage ==
                        MALProgressOperation._PROGRESS_STAGE) || (type == InteractionType._PUBSUB_INDEX && stage ==
                            MALPubSubOperation._REGISTER_STAGE) || (type == InteractionType._PUBSUB_INDEX && stage ==
                                MALPubSubOperation._PUBLISH_REGISTER_STAGE) || (type == InteractionType._PUBSUB_INDEX &&
                                    stage == MALPubSubOperation._PUBLISH_DEREGISTER_STAGE) || (type ==
                                        InteractionType._PUBSUB_INDEX && stage ==
                                            MALPubSubOperation._DEREGISTER_STAGE));
        if (!isErrorAllowed) {
            return null;
        }

        final MALMessage errMsg = createMessage(header.getAuthenticationId(), header.getURIFrom(), // Reply to message sender.
            new Time(System.currentTimeMillis()), // PENDING: Epoch for Time in MAL Java API unclear. Here: Use Java
            // epoch.
            header.getQoSlevel(), header.getPriority(), header.getDomain(), header.getNetworkZone(), header
                .getSession(), header.getSessionName(), header.getInteractionType(), new UOctet((short) (stage + 1)), // An error always replaces the next stage.
            header.getTransactionId(), header.getServiceArea(), header.getService(), header.getOperation(), header
                .getAreaVersion(), Boolean.TRUE, // Yes, this is an error message.
            replyToMsg.getQoSProperties(), error.getErrorNumber(), error.getExtraInformation());
        if (uriFrom != null) {
            errMsg.getHeader().setURIFrom(uriFrom);
        }
        return errMsg;
    }

    /**
     * Creates a specialized message body according to some header information.
     */
    protected static SPPMessageBody createMessageBody(final MALEncodedBody encodedBody,
        final MALElementStreamFactory esf, final MALEncodingContext ctx) {
        final MALMessageHeader header = ctx.getHeader();
        if (header.getIsErrorMessage()) {
            return new SPPErrorBody(encodedBody, esf, ctx);
        }
        if (header.getInteractionType().getOrdinal() == InteractionType._PUBSUB_INDEX) {
            switch (header.getInteractionStage().getValue()) {
                case MALPubSubOperation._REGISTER_STAGE:
                    return new SPPRegisterBody(encodedBody, esf, ctx);
                case MALPubSubOperation._PUBLISH_REGISTER_STAGE:
                    return new SPPPublishRegisterBody(encodedBody, esf, ctx);
                case MALPubSubOperation._PUBLISH_STAGE:
                    return new SPPPublishBody(encodedBody, esf, ctx);
                case MALPubSubOperation._NOTIFY_STAGE:
                    return new SPPNotifyBody(encodedBody, esf, ctx);
                case MALPubSubOperation._DEREGISTER_STAGE:
                    return new SPPDeregisterBody(encodedBody, esf, ctx);
            }
        }
        return new SPPMessageBody(encodedBody, esf, ctx);
    }

    /**
     * Creates a specialized message body according to some header information.
     */
    protected static SPPMessageBody createMessageBody(final Object[] body, final MALElementStreamFactory esf,
        final MALEncodingContext ctx) {
        final MALMessageHeader header = ctx.getHeader();
        if (header.getIsErrorMessage()) {
            return new SPPErrorBody(body, esf, ctx);
        }
        if (header.getInteractionType().getOrdinal() == InteractionType._PUBSUB_INDEX) {
            switch (header.getInteractionStage().getValue()) {
                case MALPubSubOperation._REGISTER_STAGE:
                    return new SPPRegisterBody(body, esf, ctx);
                case MALPubSubOperation._PUBLISH_REGISTER_STAGE:
                    return new SPPPublishRegisterBody(body, esf, ctx);
                case MALPubSubOperation._PUBLISH_STAGE:
                    return new SPPPublishBody(body, esf, ctx);
                case MALPubSubOperation._NOTIFY_STAGE:
                    return new SPPNotifyBody(body, esf, ctx);
                case MALPubSubOperation._DEREGISTER_STAGE:
                    return new SPPDeregisterBody(body, esf, ctx);
            }
        }
        return new SPPMessageBody(body, esf, ctx);
    }

    @Override
    public void sendMessage(final MALMessage msg) throws IllegalArgumentException, MALTransmitErrorException,
        MALException {
        if (isClosed) {
            throw new MALException(ENDPOINT_CLOSED);
        }
        if (msg == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        final MALMessageHeader header = msg.getHeader();
        if (null == header.getURIFrom() || null == header.getURITo() || null == header.getQoSlevel() || null == header
            .getSession() || null == header.getInteractionType() || null == header.getInteractionStage() || null ==
                header.getTransactionId() || null == header.getServiceArea() || null == header.getService() || null ==
                    header.getOperation() || null == header.getAreaVersion() || null == header.getIsErrorMessage()) {
            final MALStandardError error = new MALStandardError(MALHelper.INTERNAL_ERROR_NUMBER, HEADER_FIELD_IS_NULL);
            throw new MALTransmitErrorException(header, error, msg.getQoSProperties());
        }

        final SPPURI sppURIFrom;
        final SPPURI sppURITo;
        try {
            // Check validity of URIs here by creating SPPURI objects.
            sppURIFrom = new SPPURI(header.getURIFrom());
            sppURITo = new SPPURI(header.getURITo());
        } catch (final IllegalArgumentException ex) {
            final MALStandardError error = new MALStandardError(MALHelper.INTERNAL_ERROR_NUMBER, INVALID_URI);
            throw new MALTransmitErrorException(header, error, msg.getQoSProperties());
        }

        final SPPURI epURI = new SPPURI(uri);
        // boolean isLocalDestination = (sppURITo.getQualifier() ==
        // epURI.getQualifier()) && (sppURITo.getAPID() == epURI.getAPID());
        final boolean isLocalDestination = null != transport.getEndpoint(header.getURITo());

        try {
            if (isLocalDestination) {
                try {
                    transport.injectReceivedMessage(msg);
                    return;
                } catch (final Exception ex) {
                    Logger.getLogger(SPPEndpoint.class.getName()).log(Level.SEVERE,
                        "Maybe the configuration file is not being read!", ex);
                    final MALStandardError error = new MALStandardError(MALHelper.INTERNAL_ERROR_NUMBER, ex
                        .getMessage());
                    throw new MALTransmitErrorException(msg.getHeader(), error, msg.getQoSProperties());
                }
            }

            final Configuration config = new Configuration(msg.getQoSProperties());
            final boolean isTCpacket = config.isTCpacket();
            final int primaryQualifier = isTCpacket ? sppURITo.getQualifier() : sppURIFrom.getQualifier();
            final short primaryApid = isTCpacket ? sppURITo.getAPID() : sppURIFrom.getAPID();

            // Needs to be synchronized to avoid getting packets out of order
            synchronized (sppSocket) {
                final SPPCounter sequenceCounter = transport.getSequenceCounter(primaryQualifier, primaryApid);
                final SPPCounter segmentCounter = transport.getSegmentCounter(header);
                final int packetDataFieldSizeLimit = config.packetDataFieldSizeLimit();

                // Line hitting the exception is below!!
                final SpacePacket[] spacePackets = ((SPPMessage) msg).createSpacePackets(sequenceCounter,
                    segmentCounter, packetDataFieldSizeLimit);

                // this.outgoingQueue.put(spacePackets);

                for (final SpacePacket sp : spacePackets) {
                    sppSocket.send(sp);
                }

            }
        } catch (final Exception ex) {
            final MALStandardError error = new MALStandardError(MALHelper.INTERNAL_ERROR_NUMBER, ex.getMessage());
            throw new MALTransmitErrorException(msg.getHeader(), error, msg.getQoSProperties());
        }
    }

    @Override
    public void sendMessages(final MALMessage[] msgList) throws IllegalArgumentException, MALException {
        if (isClosed) {
            throw new MALException(ENDPOINT_CLOSED);
        }
        if (msgList == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }

        final List<MALTransmitErrorException> transmitErrors = new LinkedList<>();
        for (final MALMessage msg : msgList) {
            try {
                sendMessage(msg);
            } catch (final MALTransmitErrorException ex) {
                transmitErrors.add(ex);
            }
        }
        if (!transmitErrors.isEmpty()) {
            throw new MALTransmitMultipleErrorException(transmitErrors.toArray(new MALTransmitErrorException[0]));
        }
    }

    @Override
    public void setMessageListener(final MALMessageListener listener) throws MALException {
        if (isClosed) {
            throw new MALException(ENDPOINT_CLOSED);
        }
        // PENDING: 5.2.4.7.4 MAL Java API only says 'listener' shall not be NULL, but
        // doesn't say
        // what to do if it is. Here: Throw IllegalArgumentException.
        if (listener == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        this.listener = listener;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public void close() throws MALException {
        if (isClosed) {
            return;
        }
        stopMessageDelivery();
        isClosed = true;
        if (getLocalName() == null) {
            transport.invalidateURI(getURI());
        }
    }

    /**
     * Reopens a (possibly previously closed) endpoint.
     */
    protected void reopen() {
        isClosed = false;
    }

    protected MALMessageListener getMessageListener() {
        return listener;
    }

    protected boolean isClosed() {
        return isClosed;
    }

    protected boolean isDeliveryStopped() {
        return isDeliveryStopped;
    }
}
