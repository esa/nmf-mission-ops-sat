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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Map;

import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALOperation;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.transport.MALEncodedBody;
import org.ccsds.moims.mo.mal.transport.MALMessage;
import org.ccsds.moims.mo.mal.transport.MALMessageBody;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacketHeader;

public class SPPMessage implements MALMessage {

    // Error string
    private static final String TRANSACTION_ID_IS_NULL = "'Transaction Id' may not be null.";
    // Member variables
    private final SPPMessageHeader header;
    private final SPPMessageBody body;
    private final Map qosProperties;
    private final Map endpointQosProperties;
    private final MALElementStreamFactory esf;

    public SPPMessage(final SPPMessageHeader header, final SPPMessageBody body, final Map qosProperties,
        final Map endpointQosProperties, final MALElementStreamFactory esf, final SPPTransport transport) {
        this.header = header;
        this.body = body;
        this.qosProperties = qosProperties;
        this.endpointQosProperties = endpointQosProperties;
        this.esf = esf;
    }

    public SPPMessage(final SPPMessageHeader header, final SpacePacket[] spacePackets, final Map qosProperties,
        final Map endpointQosProperties, final MALElementStreamFactory esf, final SPPTransport transport)
        throws MALException {
        this.qosProperties = qosProperties;
        this.endpointQosProperties = endpointQosProperties;
        this.esf = esf;
        this.header = header;

        // combine user (*not* packet) data fields of (segmented) Space Packets
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (final SpacePacket sp : spacePackets) {
            baos.write(sp.getBody(), header.getOffset(), sp.getBody().length - header.getOffset());
        }
        final byte[] userDataField = baos.toByteArray();

        final MALOperation op;
        try {
            op = MALContextFactory.lookupArea(header.getServiceArea(), header.getAreaVersion()).getServiceByNumber(
                header.getService()).getOperationByNumber(header.getOperation());
        } catch (final NullPointerException npe) {
            throw new MALException("Could not resolve operation. " + header);
        }
        final MALEncodingContext ctx = new MALEncodingContext(header, op, -1, null, this.qosProperties);
        final MALEncodedBody encodedBody = userDataField.length == 0 ? null : new MALEncodedBody(new Blob(
            userDataField));
        body = SPPEndpoint.createMessageBody(encodedBody, esf, ctx);
    }

    /**
     * Create Space Packet(s) from the MAL message according to CCSDS 524.1.
     *
     * @param sequenceCounter          The sequence counter to be used for creating
     *                                 the Space Packet.
     * @param segmentCounter           The segment counter to be used for creating
     *                                 the Space Packet.
     * @param packetDataFieldSizeLimit Limit of the packet data field size in
     *                                 octets. 0 for maximum.
     * @return An array of valid Space Packets encapsulating the MAL message.
     * @throws MALException
     */
    protected SpacePacket[] createSpacePackets(final SPPCounter sequenceCounter, final SPPCounter segmentCounter,
        final int packetDataFieldSizeLimit) throws MALException {
        // create secondary header in 2 parts: before and after the segment counter
        final ByteArrayOutputStream encSecondaryHeaderPart1 = new ByteArrayOutputStream();
        final ByteArrayOutputStream encSecondaryHeaderPart2 = new ByteArrayOutputStream();
        final boolean isTCpacket = new Configuration(qosProperties).isTCpacket();

        writeSecondaryHeader(encSecondaryHeaderPart1, encSecondaryHeaderPart2, isTCpacket);

        // create encoded MAL message body
        final byte[] encBody;
        final MALEncodedBody malEncodedBody = body.getEncodedBody();
        if (malEncodedBody == null || malEncodedBody.getEncodedBody() == null) {
            encBody = new byte[]{};
        } else {
            final Blob wholeBody = malEncodedBody.getEncodedBody();
            encBody = new byte[wholeBody.getLength()];
            System.arraycopy(wholeBody.getValue(), wholeBody.getOffset(), encBody, 0, wholeBody.getLength());
        }

        final SPPURI primarySPPURI = new SPPURI(isTCpacket ? header.getURITo() : header.getURIFrom());
        final int primaryApidQualifier = primarySPPURI.getQualifier();
        final int primaryApid = primarySPPURI.getAPID();

        // Create template Space Packet header. Sequence flags and packet sequence count
        // will be
        // handled and set correctly in SPPSegmenter.split().
        final SpacePacketHeader spHeader = new SpacePacketHeader(SPPTransport.SPP_VERSION, isTCpacket ? 1 : 0, 1,
            primaryApid, 0b11, 0);

        final SpacePacket[] spacePackets = SPPSegmenter.split(packetDataFieldSizeLimit, primaryApidQualifier, spHeader,
            encSecondaryHeaderPart1.toByteArray(), encSecondaryHeaderPart2.toByteArray(), encBody, sequenceCounter,
            segmentCounter);

        for (final SpacePacket sp : spacePackets) {
            // PENDING: Testbed assumes endpoint QoS properties to be delivered to the
            // TRANSMIT
            // request.
            sp.setQosProperties(endpointQosProperties);
        }
        return spacePackets;
    }

    /**
     * Encodes the secondary header of the Space Packet to two stream. The first
     * stream will contain all bytes up to (but excluding) the segment counter, the
     * second stream will contain all bytes starting from the segment counter
     * (excluding the counter) to the end of the header.
     *
     * @param os1        Output stream where the first part of the secondary Space
     *                   Packet header is written to.
     * @param os2        Output stream where the second part of the secondary Space
     *                   Packet header is written to.
     * @param isTCpacket True, if packet type is telecommand, false if telemetry.
     * @throws MALException
     */
    private void writeSecondaryHeader(final OutputStream os1, final OutputStream os2, final boolean isTCpacket)
        throws MALException {
        final MALElementOutputStream eos1 = esf.createOutputStream(os1);
        final byte sdu = header.getSDU();
        eos1.writeElement(new UOctet((short) (SPPTransport.MALSPP_VERSION << 5 | sdu)), null);
        writeUShort(header.getServiceArea(), eos1);
        writeUShort(header.getService(), eos1);
        writeUShort(header.getOperation(), eos1);
        eos1.writeElement(header.getAreaVersion(), null);
        final SPPURI sppURIFrom = new SPPURI(header.getURIFrom());
        final SPPURI sppURITo = new SPPURI(header.getURITo());
        final short secondaryAPID = isTCpacket ? sppURIFrom.getAPID() : sppURITo.getAPID();
        final int secondaryQualifier = isTCpacket ? sppURIFrom.getQualifier() : sppURITo.getQualifier();
        final int error_qos_session_scndapid = ((header.getIsErrorMessage() ? 1 : 0) << 15) | ((byte) (header
            .getQoSlevel().getNumericValue().getValue() - 1) << 13) | ((byte) (header.getSession().getNumericValue()
                .getValue() - 1) << 11) | (secondaryAPID);
        writeUShort(new UShort(error_qos_session_scndapid), eos1);
        writeUShort(new UShort(secondaryQualifier), eos1);
        // NOTE: According to 4.4.2 Transaction id is required to be provided by the MAL
        // also for
        // SEND interactions. This is a constraint imposed by the MAL/SPP binding on the
        // MAL (in its
        // most general form, the MAL does not require a Transaction id for SEND
        // interactions).
        final Long transactionId = header.getTransactionId();
        if (transactionId == null) {
            throw new MALException(TRANSACTION_ID_IS_NULL);
        }
        writeLong(transactionId, eos1);

        final Short sourceIdentifier = sppURIFrom.getIdentifier();
        final Short destinationIdentifier = sppURITo.getIdentifier();

        final Configuration config = new Configuration(qosProperties);
        final boolean priorityFlag = config.priorityFlag();
        final boolean timestampFlag = config.timestampFlag();
        final boolean networkZoneFlag = config.networkZoneFlag();
        final boolean sessionNameFlag = config.sessionNameFlag();
        final boolean domainFlag = config.domainFlag();
        final boolean authenticationIdFlag = config.authenticationIdFlag();

        final BitSet bs = new BitSet(8);
        // Bits in BitSet are numbered right to left.
        bs.set(7, null != sourceIdentifier);
        bs.set(6, null != destinationIdentifier);
        bs.set(5, priorityFlag);
        bs.set(4, timestampFlag);
        bs.set(3, networkZoneFlag);
        bs.set(2, sessionNameFlag);
        bs.set(1, domainFlag);
        bs.set(0, authenticationIdFlag);
        eos1.writeElement(new UOctet((short) bs.toLongArray()[0]), null);

        if (null != sourceIdentifier) {
            eos1.writeElement(new UOctet(sourceIdentifier), null);
        }
        if (null != destinationIdentifier) {
            eos1.writeElement(new UOctet(destinationIdentifier), null);
        }

        // Second stream starts here; segment counter is encoded separately in
        // SPPSegmenter.split().
        final MALElementOutputStream eos2 = esf.createOutputStream(os2);
        if (priorityFlag) {
            eos2.writeElement(header.getPriority(), null);
        }
        if (timestampFlag) {
            eos2.writeElement(header.getTimestamp(), null);
        }
        if (networkZoneFlag) {
            eos2.writeElement(header.getNetworkZone(), null);
        }
        if (sessionNameFlag) {
            eos2.writeElement(header.getSessionName(), null);
        }
        if (domainFlag) {
            eos2.writeElement(header.getDomain(), null);
        }
        if (authenticationIdFlag) {
            eos2.writeElement(header.getAuthenticationId(), null);
        }
    }

    /**
     * Helper method for writing an unsigned short value to a MALElementOutputStream
     * using a fixed encoding length of two bytes.
     *
     * @param value Unsigned short value to be written.
     * @param os    Stream to write the value to.
     * @throws MALException
     */
    private static void writeUShort(final UShort value, final MALElementOutputStream os) throws MALException {
        final int v = value.getValue();
        for (int i = 1; i >= 0; i--) {
            final short b = (short) ((v >>> (i * 8)) & 255);
            os.writeElement(new UOctet(b), null);
        }
    }

    /**
     * Helper method for writing a signed long value to a MALElementOutputStream
     * using a fixed encoding length of eight bytes.
     *
     * @param value Signed long value to be written.
     * @param os    Stream to write the value to.
     * @throws MALException
     */
    private static void writeLong(final Long value, final MALElementOutputStream os) throws MALException {
        for (int i = 7; i >= 0; i--) {
            final short b = (short) ((value >>> (i * 8)) & 255);
            os.writeElement(new UOctet(b), null);
        }
    }

    @Override
    public MALMessageHeader getHeader() {
        return header;
    }

    @Override
    public MALMessageBody getBody() {
        return body;
    }

    @Override
    public Map getQoSProperties() {
        return qosProperties;
    }

    @Override
    public void free() throws MALException {
        // nothing to do
    }
}
