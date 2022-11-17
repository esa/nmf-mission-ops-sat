/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO SPP Transport Framework
 * ----------------------------------------------------------------------------
 * Licensed under European Space Agency Public License (ESA-PL) Weak Copyleft – v2.4
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
package esa.mo.mal.transport.spp;

import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.util.GENMessagePoller;

import java.util.logging.Level;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.transport.MALTransmitErrorException;
import org.ccsds.moims.mo.mal.transport.MALTransportFactory;
import org.ccsds.moims.mo.testbed.util.spp.SPPSocket;
import org.ccsds.moims.mo.testbed.util.spp.SPPSocketFactory;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacketHeader;

public class SPPTransport extends SPPBaseTransport<SpacePacket> {
    private final SPPSocket sppSocket;

    /*
     * Constructor.
     *
     * @param protocol The protocol string.
     * @param factory The factory that created us.
     * @param properties The QoS properties.
     * @throws MALException On error.
     */
    public SPPTransport(final String protocol, final MALTransportFactory factory, final java.util.Map properties)
        throws MALException {
        super(new SPPConfiguration(true, 65530, true, true, true, true, true, true, true, true),
            new SPPURIRepresentationSimple(), new SPPSourceSequenceCounterSimple(), protocol, ":", '/', '@', false,
            false, factory, properties);

        try {
            sppSocket = SPPSocketFactory.newInstance().createSocket(properties);
        } catch (final Exception ex) {
            LOGGER.log(Level.WARNING, "Error in Space Packet Protocol library: " + ex.getMessage(), ex);
            throw new MALException("Error in Space Packet Protocol library: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void init() throws MALException {
        super.init();

        uriBase = "malspp:";

        final GENMessagePoller rcvr = new GENMessagePoller<>(this, new SPPMessageSender(sppSocket),
            new SPPMessageReceiver(sppSocket), new SPPMessageDecoderFactory<>());
        rcvr.start();
    }

    @Override
    public void close() throws MALException {
        super.close();

        synchronized (this) {
            try {
                sppSocket.close();
            } catch (final Exception ex) {
                LOGGER.log(Level.WARNING, "Space Packet Socket error (ignore if socket closed on purpose).", ex);
                throw new MALException("Space Packet Socket error (ignore if socket closed on purpose).", ex);
            }
        }
    }

    @Override
    protected String createTransportAddress() throws MALException {
        return "";
    }

    @Override
    protected GENMessageSender createMessageSender(final GENMessage msg, final String remoteRootURI)
        throws MALException, MALTransmitErrorException {
        //create a message sender and receiver for the socket

        return new SPPMessageSender(sppSocket);
    }

    @Override
    public GENMessage createMessage(final SpacePacket packet) throws MALException {
        final byte[] a = new byte[6];
        final SpacePacketHeader sph = packet.getHeader();
        final int vers_nb = sph.getPacketVersionNumber();
        final int pkt_type = sph.getPacketType();
        final int sec_head_flag = sph.getSecondaryHeaderFlag();
        final int TCPacket_apid = sph.getApid();
        final int segt_flag = sph.getSequenceFlags();
        final int pkt_ident = (vers_nb << 13) | (pkt_type << 12) | (sec_head_flag << 11) | (TCPacket_apid);

        final int pkt_seq_ctrl = (segt_flag << 14) | (packet.getHeader().getSequenceCount());

        // Remove 1 byte as specified by the specification.
        final int pkt_length_value = packet.getLength() - 1;

        a[0] = (byte) (pkt_ident >> 8);
        a[1] = (byte) (pkt_ident & 0xFF);
        a[2] = (byte) (pkt_seq_ctrl >> 8);
        a[3] = (byte) (pkt_seq_ctrl & 0xFF);
        a[4] = (byte) (pkt_length_value >> 8);
        a[5] = (byte) (pkt_length_value & 0xFF);

        final byte[] b = packet.getBody();

        final byte[] c = new byte[a.length + packet.getLength()];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, packet.getLength());

        //    System.out.println("RCV: " + GENHelper.byteArrayToHexString(c));

        return internalCreateMessage(packet.getApidQualifier(), sph.getApid(), segt_flag, c);
    }
}
