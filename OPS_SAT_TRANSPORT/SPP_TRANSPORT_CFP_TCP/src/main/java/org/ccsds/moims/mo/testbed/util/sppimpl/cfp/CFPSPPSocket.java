/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
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
package org.ccsds.moims.mo.testbed.util.sppimpl.cfp;

import esa.mo.transport.can.opssat.CANBusConnector;
import esa.mo.transport.can.opssat.CANReceiveInterface;
import esa.mo.transport.can.opssat.CFPFrameHandler;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import org.ccsds.moims.mo.testbed.util.spp.SPPSocket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.testbed.util.sppimpl.util.SPPReader;
import org.ccsds.moims.mo.testbed.util.sppimpl.util.SPPWriter;

public class CFPSPPSocket implements SPPSocket {

    private final LinkedBlockingQueue<SpacePacket> input;
    private final String BUS_NAME = "can0";
    private CFPFrameHandler handler;
    private final SPPWriter writer = new SPPWriter(new SenderImpl());
    private final Object MUTEX = new Object();
    private int destinationNode;
    private int virtualChannel;

    public CFPSPPSocket() {
        super();
        input = new LinkedBlockingQueue<SpacePacket>();
    }

    public void init(Map properties) throws Exception {
        try {
            handler = new CFPFrameHandler(new ReceiverImpl());
        } catch (IOException ex) {
            throw new MALException("Error initializing connection to socketcand!", ex);
        }
                    
        handler.init();
    }

    @Override
    public void close() throws Exception {
        handler.close();
    }

    @Override
    public SpacePacket receive() throws Exception {
        SpacePacket packet = input.take();

        java.util.logging.Logger.getLogger(CFPSPPSocket.class.getName()).log(Level.FINE,
                "A Space Packet was received"
                + "\ndata: " + packet.toString());

        return packet;
    }

    @Override
    public void send(SpacePacket packet) throws Exception {
        synchronized (MUTEX) {
            Logger.getLogger(CFPSPPSocket.class.getName()).log(Level.FINE,
                    "Sequence count: " + packet.getHeader().getSequenceCount()
                    + " - " + Arrays.toString(packet.getBody()));
            
            // Change the destination node and virtual channel based on the selected location
            String propNodeDest = (String) packet.getQosProperties().get("esa.mo.transport.can.opssat.nodeDestination");
            String propVirtualC = (String) (String) packet.getQosProperties().get("esa.mo.transport.can.opssat.virtualChannel");

            // Defaults
            this.destinationNode = (propNodeDest != null) ? Integer.parseInt(propNodeDest) : CANBusConnector.CAN_NODE_NR_DST_CCSDS; // Default is CCSDS Engine
            this.virtualChannel = (propVirtualC != null) ? Integer.parseInt(propVirtualC) : 2; // Default is VC 2
        
            java.util.logging.Logger.getLogger(CFPSPPSocket.class.getName()).log(Level.FINE, 
                "destinationNode: " + this.destinationNode + " - virtualChannel: " + this.virtualChannel);

            writer.send(packet);
        }
    }

    @Override
    public String getDescription() {
        return "-" + BUS_NAME;
    }

    public class ReceiverImpl implements CANReceiveInterface {

        /**
         * Creates a new instance of ReceiverImpl
         *
         */
        public ReceiverImpl() {
        }

        @Override
        public void receive(final byte[] array) {

            java.util.logging.Logger.getLogger(CFPSPPSocket.class.getName()).log(Level.FINEST,
                    "Data Received on the glue code..."
                    + "\ndata: " + Arrays.toString(array));

            SPPReader reader = new SPPReader(new ByteArrayInputStream(array));

            try {
                SpacePacket packet = reader.receive();
                input.offer(packet);
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(CFPSPPSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public class SenderImpl extends ByteArrayOutputStream {

        public SenderImpl() {
            super(256);
        }

        @Override
        public void flush() throws IOException {
            try {
                handler.sendData(super.toByteArray(), destinationNode, virtualChannel);
                super.reset();
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(CFPSPPSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
