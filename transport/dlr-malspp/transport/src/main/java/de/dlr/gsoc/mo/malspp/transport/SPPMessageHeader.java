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

import java.io.ByteArrayInputStream;
import java.util.BitSet;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInvokeOperation;
import org.ccsds.moims.mo.mal.MALProgressOperation;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.MALRequestOperation;
import org.ccsds.moims.mo.mal.MALSubmitOperation;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
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
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacketHeader;

public class SPPMessageHeader implements MALMessageHeader {

    private static final String ILLEGAL_NULL_ARGUMENT = "Argument may not be null.";
    private static final String MALFORMED_SPACE_PACKET = "Malformed Space Packet received.";
    private static final String VERSION_NOT_SUPPORTED = "Received Space Packet with unsupported MAL/SPP version number.";
    private URI uriFrom;
    private Blob authenticationId;
    private URI uriTo;
    private Time timestamp;
    private QoSLevel qosLevel;
    private UInteger priority;
    private IdentifierList domain;
    private Identifier networkZone;
    private SessionType session;
    private Identifier sessionName;
    private InteractionType interactionType;
    private UOctet interactionStage;
    private Long transactionId;
    private UShort serviceArea;
    private UShort service;
    private UShort operation;
    private UOctet areaVersion;
    private Boolean isErrorMessage;
    private final Integer offset; // only used when constructing header from Space Packet; denotes body data
                                  // offset

    private SPPURI sppURIFrom;
    private SPPURI sppURITo;

    public SPPMessageHeader(final URI uriFrom, final Blob authenticationId, final URI uriTo, final Time timestamp,
            final QoSLevel qosLevel, final UInteger priority, final IdentifierList domain, final Identifier networkZone,
            final SessionType session, final Identifier sessionName, final InteractionType interactionType,
            final UOctet interactionStage, final Long transactionId, final UShort serviceArea, final UShort service,
            final UShort operation, final UOctet areaVersion, final Boolean isErrorMessage)
            throws IllegalArgumentException {
        if (uriFrom == null || authenticationId == null || uriTo == null || timestamp == null || qosLevel == null
                || priority == null || domain == null || networkZone == null || session == null || sessionName == null
                || interactionType == null
                // PENDING: Testbed expects no exception if interactionStage == null, although
                // this
                // is demanded by 5.2.4.4.4 MAL Java API. Here: If interactionStage == null set
                // it
                // to UOcctet(0).
                // || interactionStage == null
                || serviceArea == null || service == null || operation == null || areaVersion == null
                || isErrorMessage == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        this.uriFrom = uriFrom;
        this.authenticationId = authenticationId;
        this.uriTo = uriTo;
        this.timestamp = timestamp;
        this.qosLevel = qosLevel;
        this.priority = priority;
        this.domain = domain;
        this.networkZone = networkZone;
        this.session = session;
        this.sessionName = sessionName;
        this.interactionType = interactionType;
        // PENDING: Testbed expects no exception if interactionStage == null, although
        // this
        // is demanded by 5.2.4.4.4 MAL Java API. Here: If interactionStage == null set
        // it
        // to UOcctet(0).
        this.interactionStage = interactionStage == null ? new UOctet((short) 0) : interactionStage;
        this.transactionId = transactionId;
        this.serviceArea = serviceArea;
        this.service = service;
        this.operation = operation;
        this.areaVersion = areaVersion;
        this.isErrorMessage = isErrorMessage;
        this.offset = null;
    }

    public SPPMessageHeader(final SpacePacket spacePacket, final MALElementStreamFactory esf,
            final Map msgQosProperties) throws MALException {
        final SpacePacketHeader sppHeader = spacePacket.getHeader();
        if (sppHeader.getPacketVersionNumber() != SPPTransport.SPP_VERSION || sppHeader.getSecondaryHeaderFlag() != 1) {
            throw new MALException(MALFORMED_SPACE_PACKET);
        }
        final short primaryApid = (short) sppHeader.getApid();
        final int primaryApidQualifier = spacePacket.getApidQualifier();
        final boolean isTCpacket = sppHeader.getPacketType() == 1;

        final ByteArrayInputStream is = new ByteArrayInputStream(spacePacket.getBody());
        offset = initMessageHeader(is, isTCpacket, sppHeader.getSequenceFlags(), primaryApid, primaryApidQualifier, esf,
                msgQosProperties);
    }

    private int initMessageHeader(final ByteArrayInputStream is, final boolean isTCpacket, final int sequenceFlag,
            final short primaryApid, final int primaryApidQualifier, final MALElementStreamFactory esf,
            final Map msgQosProperties) throws MALException {
        final int size = is.available();
        final MALElementInputStream eis = esf.createInputStream(is);

        final short version_sdu = ((UOctet) eis.readElement(new UOctet(), null)).getValue();
        final int version = version_sdu >>> 5;
        final byte sdu = (byte) (version_sdu & 0x1F);
        if (version != SPPTransport.MALSPP_VERSION) {
            throw new MALException(VERSION_NOT_SUPPORTED);
        }
        setInteraction(sdu);
        setServiceArea(readUShort(eis));
        setService(readUShort(eis));
        setOperation(readUShort(eis));
        setAreaVersion((UOctet) eis.readElement(new UOctet(), null));

        final int error_qos_session_scndapid = readUShort(eis).getValue();
        final int isErrorMessage = error_qos_session_scndapid >>> 15;
        final int qosLevel = (error_qos_session_scndapid >>> 13) & 0b11;
        final int session = (error_qos_session_scndapid >>> 11) & 0b11;
        final short secondaryApid = (short) (error_qos_session_scndapid & 0b0000011111111111);
        setIsErrorMessage(isErrorMessage == 1);
        setQoSlevel(QoSLevel.fromOrdinal(qosLevel));
        setSession(SessionType.fromOrdinal(session));
        final int secondaryApidQualifier = readUShort(eis).getValue();
        // MAL/SPP always specifies a transaction id, also for SEND interactions.
        setTransactionId(readLong(eis));

        final byte flags = (byte) ((UOctet) eis.readElement(new UOctet(), null)).getValue();
        final BitSet bs = BitSet.valueOf(new byte[] { flags });

        // Bits in BitSet are numbered right to left.
        final Short sourceIdentifier = bs.get(7) ? ((UOctet) eis.readElement(new UOctet(), null)).getValue() : null;

        final Short destinationIdentifier = bs.get(6) ? ((UOctet) eis.readElement(new UOctet(), null)).getValue() : null;

        if (isTCpacket) {
            sppURIFrom = new SPPURI(secondaryApidQualifier, secondaryApid, sourceIdentifier);
            sppURITo = new SPPURI(primaryApidQualifier, primaryApid, destinationIdentifier);
        } else {
            sppURIFrom = new SPPURI(primaryApidQualifier, primaryApid, sourceIdentifier);
            sppURITo = new SPPURI(secondaryApidQualifier, secondaryApid, destinationIdentifier);
        }
        setURIFrom(sppURIFrom.getURI());
        setURITo(sppURITo.getURI());

        // Read segment counter only if it was encoded, but ignore its value.
        // SPPSegmenter will
        // read this field again and uses it for packet ordering. This could be solved
        // in a more
        // elegant way without reading the same field twice, but it's ok fo a prototype.
        if (sequenceFlag != 0b11) { // packet is segmented and has segment counter (length: 4 bytes)
            for (int i = 0; i < 4; i++) {
                eis.readElement(new UOctet(), null); // ignore return value
            }
        }

        final Configuration config = new Configuration(msgQosProperties);
        final UInteger priority = bs.get(5) ? (UInteger) eis.readElement(new UInteger(), null) : config.priority();
        setPriority(priority);

        final Time timestamp = bs.get(4) ? (Time) eis.readElement(new Time(), null) : Configuration.DEFAULT_TIMESTAMP;
        setTimestamp(timestamp);

        final Identifier networkZone = bs.get(3) ? (Identifier) eis.readElement(new Identifier(), null)
                : config.networkZone();
        setNetworkZone(networkZone);

        final Identifier sessionName = bs.get(2) ? (Identifier) eis.readElement(new Identifier(), null)
                : config.sessionName();
        setSessionName(sessionName);

        final IdentifierList domain = bs.get(1) ? (IdentifierList) eis.readElement(new IdentifierList(), null)
                : config.domain();
        setDomain(domain);

        final Blob authenticationId = bs.get(0) ? (Blob) eis.readElement(new Blob(), null) : config.authenticationId();
        setAuthenticationId(authenticationId);

        // return offset
        return size - is.available();
    }

    /**
     * Gets the SDU type of the Space Packet according to Table 3-6 of the MAL/SPP
     * Book.
     *
     * @return The SDU type, between 0 and 21.
     * @throws MALException
     */
    protected byte getSDU() throws MALException {
        final short stage = interactionStage.getValue();
        switch (interactionType.getOrdinal()) {
            case InteractionType._SEND_INDEX:
                return 0;
            case InteractionType._SUBMIT_INDEX:
                return (byte) stage;
            case InteractionType._REQUEST_INDEX:
                return (byte) (stage + 2);
            case InteractionType._INVOKE_INDEX:
                return (byte) (stage + 4);
            case InteractionType._PROGRESS_INDEX:
                return (byte) (stage + 7);
            case InteractionType._PUBSUB_INDEX:
                return (byte) (stage + 11);
        }
        throw new MALException();
    }

    public SPPURI getSPPURIFrom() {
        return sppURIFrom;
    }

    public SPPURI getSPPURITo() {
        return sppURITo;
    }

    // Array for mapping SDU type to interaction stages.
    private static final UOctet[] SDU_STAGES = new UOctet[] { new UOctet((short) 0), // interaction stage for SEND not
                                                                                     // specified, set to 0
            MALSubmitOperation.SUBMIT_STAGE, MALSubmitOperation.SUBMIT_ACK_STAGE, MALRequestOperation.REQUEST_STAGE,
            MALRequestOperation.REQUEST_RESPONSE_STAGE, MALInvokeOperation.INVOKE_STAGE,
            MALInvokeOperation.INVOKE_ACK_STAGE, MALInvokeOperation.INVOKE_RESPONSE_STAGE,
            MALProgressOperation.PROGRESS_STAGE, MALProgressOperation.PROGRESS_ACK_STAGE,
            MALProgressOperation.PROGRESS_UPDATE_STAGE, MALProgressOperation.PROGRESS_RESPONSE_STAGE,
            MALPubSubOperation.REGISTER_STAGE, MALPubSubOperation.REGISTER_ACK_STAGE,
            MALPubSubOperation.PUBLISH_REGISTER_STAGE, MALPubSubOperation.PUBLISH_REGISTER_ACK_STAGE,
            MALPubSubOperation.PUBLISH_STAGE, MALPubSubOperation.NOTIFY_STAGE, MALPubSubOperation.DEREGISTER_STAGE,
            MALPubSubOperation.DEREGISTER_ACK_STAGE, MALPubSubOperation.PUBLISH_DEREGISTER_STAGE,
            MALPubSubOperation.PUBLISH_DEREGISTER_ACK_STAGE };

    /**
     * Sets the interactionType and interactionStage according to the SDU Type.
     *
     * @param sdu SDU Type encoded in the Space Packet.
     * @throws MALException
     */
    protected void setInteraction(final byte sdu) throws MALException {
        if (sdu == 0) {
            interactionType = InteractionType.SEND;
        } else if (sdu <= 2) {
            interactionType = InteractionType.SUBMIT;
        } else if (sdu <= 4) {
            interactionType = InteractionType.REQUEST;
        } else if (sdu <= 7) {
            interactionType = InteractionType.INVOKE;
        } else if (sdu <= 11) {
            interactionType = InteractionType.PROGRESS;
        } else if (sdu <= 21) {
            interactionType = InteractionType.PUBSUB;
        }
        interactionStage = SDU_STAGES[sdu];
    }

    /**
     * Helper method for reading an unsigned short value from a
     * MALElementInputStream using a fixed encoding length of two bytes.
     *
     * @param is Stream to read value from.
     * @return Unsigned short value read from stream.
     * @throws MALException
     */
    private static UShort readUShort(final MALElementInputStream is) throws MALException {
        int v = 0;
        for (int i = 0; i < 2; i++) {
            final short b = ((UOctet) is.readElement(new UOctet(), null)).getValue();
            v <<= 8;
            v |= b;
        }
        return new UShort(v);
    }

    /**
     * Helper method for reading a signed long value from a MALElementInputStream
     * using a fixed encoding length of eight bytes.
     *
     * @param is Stream to read value from.
     * @return Signed long value read from stream.
     * @throws MALException
     */
    private static Long readLong(final MALElementInputStream is) throws MALException {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            final short b = ((UOctet) is.readElement(new UOctet(), null)).getValue();
            v <<= 8;
            v |= b;
        }
        return v;
    }

    protected Integer getOffset() {
        return offset;
    }

    @Override
    public URI getURIFrom() {
        return uriFrom;
    }

    @Override
    public void setURIFrom(final URI newValue) {
        uriFrom = newValue;
    }

    @Override
    public Blob getAuthenticationId() {
        return authenticationId;
    }

    @Override
    public void setAuthenticationId(final Blob newValue) {
        authenticationId = newValue;
    }

    @Override
    public URI getURITo() {
        return uriTo;
    }

    @Override
    public void setURITo(final URI newValue) {
        uriTo = newValue;
    }

    @Override
    public Time getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(final Time newValue) {
        timestamp = newValue;
    }

    @Override
    public QoSLevel getQoSlevel() {
        return qosLevel;
    }

    @Override
    public void setQoSlevel(final QoSLevel newValue) {
        qosLevel = newValue;
    }

    @Override
    public UInteger getPriority() {
        return priority;
    }

    @Override
    public void setPriority(final UInteger newValue) {
        priority = newValue;
    }

    @Override
    public IdentifierList getDomain() {
        return domain;
    }

    @Override
    public void setDomain(final IdentifierList newValue) {
        domain = newValue;
    }

    @Override
    public Identifier getNetworkZone() {
        return networkZone;
    }

    @Override
    public void setNetworkZone(final Identifier newValue) {
        networkZone = newValue;
    }

    @Override
    public SessionType getSession() {
        return session;
    }

    @Override
    public void setSession(final SessionType newValue) {
        session = newValue;
    }

    @Override
    public Identifier getSessionName() {
        return sessionName;
    }

    @Override
    public void setSessionName(final Identifier newValue) {
        sessionName = newValue;
    }

    @Override
    public InteractionType getInteractionType() {
        return interactionType;
    }

    @Override
    public void setInteractionType(final InteractionType newValue) {
        interactionType = newValue;
    }

    @Override
    public UOctet getInteractionStage() {
        return interactionStage;
    }

    @Override
    public void setInteractionStage(final UOctet newValue) {
        interactionStage = newValue;
    }

    @Override
    public Long getTransactionId() {
        return transactionId;
    }

    @Override
    public void setTransactionId(final Long newValue) {
        transactionId = newValue;
    }

    @Override
    public UShort getServiceArea() {
        return serviceArea;
    }

    @Override
    public void setServiceArea(final UShort newValue) {
        serviceArea = newValue;
    }

    @Override
    public UShort getService() {
        return service;
    }

    @Override
    public void setService(final UShort newValue) {
        service = newValue;
    }

    @Override
    public UShort getOperation() {
        return operation;
    }

    @Override
    public void setOperation(final UShort newValue) {
        operation = newValue;
    }

    @Override
    public UOctet getAreaVersion() {
        return areaVersion;
    }

    @Override
    public void setAreaVersion(final UOctet newValue) {
        areaVersion = newValue;
    }

    @Override
    public Boolean getIsErrorMessage() {
        return isErrorMessage;
    }

    @Override
    public void setIsErrorMessage(final Boolean newValue) {
        isErrorMessage = newValue;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("SPPMEssageHeader(");
        buf.append("uriFrom = " + uriFrom);
        buf.append(",  authenticationId = " + authenticationId);
        buf.append(",  uriTo = " + uriTo);
        buf.append(",  timestamp = " + timestamp);
        buf.append(",  qosLevel = " + qosLevel);
        buf.append(",  priority = " + priority);
        buf.append(",  domain = " + domain);
        buf.append(",  networkZone = " + networkZone);
        buf.append(",  session = " + session);
        buf.append(",  sessionName = " + sessionName);
        buf.append(",  interactionType = " + interactionType);
        buf.append(",  interactionStage = " + interactionStage);
        buf.append(",  transactionId = " + transactionId);
        buf.append(",  serviceArea = " + serviceArea);
        buf.append(",  service = " + service);
        buf.append(",  operation = " + operation);
        buf.append(",  areaVersion = " + areaVersion);
        buf.append(",  isErrorMessage = " + isErrorMessage);
        buf.append(")");
        return buf.toString();
    }
}
