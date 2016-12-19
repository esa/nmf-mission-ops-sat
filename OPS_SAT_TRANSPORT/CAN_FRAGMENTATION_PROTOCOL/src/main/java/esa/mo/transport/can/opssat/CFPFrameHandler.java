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
package esa.mo.transport.can.opssat;

import com.github.kayak.core.Frame;
import com.github.kayak.core.FrameListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CFP Frame Processing
 */
public class CFPFrameHandler implements FrameListener {

    /**
     * Maximum Transmission Unit for CSP over CAN 8 (size of CAN data) * 32
     * (number of "REMAINING"s) = 256
     */
    private static final int CSP_CAN_MTU = 256;

    // Max number of retransmissions
    private static final int MAX_NUMBER_OF_RETRANSMISSIONS = 3; // 3 Strikes and you're out!
    private static final int RETRANSMISSION_REQUEST_ID = 0;

    // TEMPORARY BUFFER LIFETIME (in case retransmission is necessary)
    private static final int DESTRUCTION_BUFFER_LIFETIME = 900; // milliseconds

    // The DESTRUCTION_ENABLED flag set as true was throwing a: java.lang.OutOfMemoryError: unable to create new native thread
    // There is a tradeoff between memory used vs. processing power. False will always use around 2 MB to store the buffers
    private static final boolean DESTRUCTION_ENABLED = false; // Do we want to destroy the buffer after a timeout?
    private static final boolean RETRANSMISSION_ENABLED = true;


    private static final String PROPERTY_VIRTUAL_CHANNEL = "esa.mo.transport.can.opssat.virtualChannel";
    public static final String PROPERTY_NODE_SOURCE = "esa.mo.transport.can.opssat.nodeSource";
    private static final String PROPERTY_NODE_DESTINATION = "esa.mo.transport.can.opssat.nodeDestination";
    private static final int DEFAULT_VIRTUAL_CHANNEL = 2;

    private final int node_source;
    private int node_destination;
    private int virtualChannel;

    private final HashMap<Short, CFPRetransmissionMessageBuffer> retransmissionBuffers; // Retransmission Buffers
    private final HashMap<Short, Long> passedUpwardsTimestamp;
    private final LinkedBlockingQueue<Short> queuedForDestruction;
    private final LinkedBlockingQueue<Short> queuedForRetransmission;

    private final LinkedBlockingQueue<ReconstructMessage> readyQueue;

    // Reconstruct the received messages
    private final HashMap<Short, ReconstructMessage> incomingMessages;

    private final CANBusConnector connector;
    private final CANReceiveInterface upperLayerReceiver;
    private short lastTransactionId = 0;

    private final Object MUTEX = new Object();
    private final AtomicInteger cfpUniqueId; // CFP unique identification number

    private final AtomicInteger messagesCounterStatic = new AtomicInteger(0);
    private final AtomicInteger messagesCounterDinamic = new AtomicInteger(0);
    private final int MESSAGES_COUNTER_INTERVAL = 4000; //  second

    /**
     * Constructor.
     *
     * @param upperLayerReceiver
     * @throws java.io.IOException
     */
    public CFPFrameHandler(final CANReceiveInterface upperLayerReceiver) throws IOException {
        Random rand = new Random();
        cfpUniqueId = new AtomicInteger(rand.nextInt() & ((1 << CFPFrameIdentifier.CFP_SIZE_TRANSACTION_ID) - 1));

        this.upperLayerReceiver = upperLayerReceiver;
        this.connector = new CANBusConnector(this);
        this.retransmissionBuffers = new HashMap<Short, CFPRetransmissionMessageBuffer>();
        this.passedUpwardsTimestamp = new HashMap<Short, Long>();
        this.incomingMessages = new HashMap<Short, ReconstructMessage>();
        this.queuedForDestruction = new LinkedBlockingQueue<Short>();
        this.queuedForRetransmission = new LinkedBlockingQueue<Short>();
        this.readyQueue = new LinkedBlockingQueue<ReconstructMessage>();

        this.node_source = (System.getProperty(PROPERTY_NODE_SOURCE) != null) ? Integer.parseInt(System.getProperty(PROPERTY_NODE_SOURCE)) : CANBusConnector.CAN_NODE_NR_SRC_SEPP;
        this.node_destination = (System.getProperty(PROPERTY_NODE_DESTINATION) != null) ? Integer.parseInt(System.getProperty(PROPERTY_NODE_DESTINATION)) : CANBusConnector.CAN_NODE_NR_DST_CCSDS;

        Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.INFO, "Node Destination: " + this.node_destination + "\nProperty: " + (System.getProperty(PROPERTY_NODE_DESTINATION) != null));

        this.virtualChannel = (System.getProperty(PROPERTY_VIRTUAL_CHANNEL) != null) ? Integer.parseInt(System.getProperty(PROPERTY_VIRTUAL_CHANNEL)) : DEFAULT_VIRTUAL_CHANNEL;
    }

    public void init() {
        this.connector.init();

        final Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Collect how many messages were exchanged in the bus...
                messagesCounterStatic.set(messagesCounterDinamic.get());
                messagesCounterDinamic.set(0);

                // An adaptive speed algorithm could go in here to minimize collisions
            }
        }, MESSAGES_COUNTER_INTERVAL, MESSAGES_COUNTER_INTERVAL);

        /*
        if (DESTRUCTION_ENABLED) {
            Thread buffersCleaner = new Thread() {
                @Override
                public void run() {
                    this.setName("CFPFrameHandler_buffersCleaner");
                    // Clean the retransmission buffers
                    while (true) {
                        try {
                            final Long currentTime = System.currentTimeMillis();
                            final Short transactionId = queuedForDestruction.take();

                            CFPRetransmissionMessageBuffer buffer;

                            synchronized (retransmissionBuffers) {
                                buffer = retransmissionBuffers.get(transactionId);
                            }

                            if (currentTime < buffer.getTimestamp() + DESTRUCTION_BUFFER_LIFETIME) {
                                Thread.sleep(buffer.getTimestamp() + DESTRUCTION_BUFFER_LIFETIME + currentTime);
                            }

                            synchronized (retransmissionBuffers) {
                                retransmissionBuffers.remove(transactionId);
                            }
                        } catch (InterruptedException ex) {
                            Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
            };

//            buffersCleaner.start();
        }
        
         */
        if (RETRANSMISSION_ENABLED) {
            Thread retransmissionThread = new Thread() {
                @Override
                public void run() {
                    this.setName("CFPFrameHandler_retransmissionThread");

                    while (true) {
                        try {
                            final Long currentTime = System.currentTimeMillis();
                            final Short transactionId = queuedForRetransmission.take();

                            // Needs time comparison here!
                            Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.WARNING,
                                    "Requesting retransmission for transactionId: " + transactionId);

                            requestRetransmission(transactionId, 0, CFPFrameHandler.convertSrcToDestinationNode(incomingMessages.get(transactionId).getSrc()));

                        } catch (InterruptedException ex) {
                            Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
            };

            retransmissionThread.start();
        }

        Thread passUpwardsThread = new Thread() {
            short counter;

            public void passUpwards(ReconstructMessage message) {
                try {
                    upperLayerReceiver.receive(message.reconstructData()); // Pass it upwards
                    short transactionId = message.getTransactionId();
                    incomingMessages.remove(transactionId);  // No need to keep it
                    passedUpwardsTimestamp.put(transactionId, System.currentTimeMillis());
                } catch (IOException ex) {
                    Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void run() {
                this.setName("CFPFrameHandler_passUpwardsThread");

                ReconstructMessage message = null;
                while (true) {
                    try {
                        message = readyQueue.take();

                        short transactionId = message.getTransactionId();

                        if (!message.wasPassedUpwards()) {  // To avoid passing it twice or more times
                            message.setPassedUpwards(true);

                            // We want them to be passed Upwards without losing messages!
                            if (lastTransactionId + 1 != transactionId && transactionId != 0 && lastTransactionId != 0) {
                                Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.WARNING,
                                        "Out-of-order detected! For transactionId: " + transactionId
                                        + ", the last transactionId was: " + lastTransactionId);

                                if (RETRANSMISSION_ENABLED) {
                                    if (lastTransactionId < transactionId) {
                                        for (int i = lastTransactionId + 1; i < transactionId; i++) {
                                            queuedForRetransmission.put((short) i);
                                        }
                                    }

                                    // Only go forward except when we jump from the last possible messsage (8k smth) to 0 
                                    if (transactionId > lastTransactionId || Math.abs(lastTransactionId - transactionId) > 8000) {
                                        lastTransactionId = transactionId;
                                    }
                                }
                            }

                            // Thread.sleep(10);
                            passUpwards(message);
                        }

                    } catch (InterruptedException ex) {
                        Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

            }

        };
        passUpwardsThread.start();

    }

    public void setVirtualChannel(int virtualChannel) {
        this.virtualChannel = virtualChannel;
    }

    public void setNodeDestination(int node_destination) {
        this.node_destination = node_destination;
    }

    /**
     * Returns the amount of messages that are being exchanged on the CAN bus
     * per second
     *
     * @return
     */
    public double getRate() {
        return (((double) messagesCounterStatic.get()) / ((double) MESSAGES_COUNTER_INTERVAL) * 1000);
    }

    public void requestRetransmission(int transactionId, int remaining, int destinationNode) {
        int dst = destinationNode + 0; // Set the virtualChannel as zero

        CFPFrameIdentifier frameIdentifier = new CFPFrameIdentifier(
                node_source, dst, RETRANSMISSION_REQUEST_ID,
                remaining, transactionId);

        // The data field should be zero
        byte[] dataField = " ".getBytes();

        // Send chunk to the CAN bus
        final Frame canFrame = new Frame(frameIdentifier.getFrameIdentifier(), true, dataField);
        this.connector.sendData2Kayak(canFrame);
    }

    public void sendData(final byte[] data, final int destinationNode,
            final int virtualChannel) throws IOException, InterruptedException {
        // Generate a transactionId
        final int transactionId = this.generateTransactionId();
        Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.INFO, "The transactionId is: " + transactionId);

        synchronized (retransmissionBuffers) {
            retransmissionBuffers.put((short) transactionId, new CFPRetransmissionMessageBuffer(data, destinationNode, virtualChannel));
        }

        this.sendData(data, 0, transactionId, false, destinationNode, virtualChannel);
    }

    public void sendData(final byte[] data) throws IOException, InterruptedException {
        this.sendData(data, this.node_destination, this.virtualChannel);
    }

    private void sendData(final byte[] data, int startingChunk, final int transactionId,
            final boolean isRetransmissionRequest, final int destinationNode, final int virtualChannel) throws IOException {
        Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.FINE,
                //        Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.INFO,
                "Send Data Request received\ndata: "
                + Arrays.toString(data));

        if (data.length > CSP_CAN_MTU) {
            Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.SEVERE, "The length of the data is bigger than 256: " + data.length);

            // ERROR!!! Not possible because CAN has a maximum of 256 bytes
            throw new IOException("The length of data cannot be greater than 256 bytes");
        }

        final int dst = destinationNode + virtualChannel;
        final int nChunks = (data.length - 1) / 8 + 1; // It's right... check for 8 bytes and 9 bytes

        for (int i = startingChunk; i < nChunks; i++) {
            final int type = (isRetransmissionRequest) ? 0 : this.generateTypeFromChunkNumber(i, nChunks);
            final int remain = nChunks - i - 1;

            CFPFrameIdentifier frameIdentifier = new CFPFrameIdentifier(
                    this.node_source, dst, type, remain, transactionId);

            final int length = (i + 1 - nChunks != 0) ? 8 + i * 8 : data.length;

            // Split the data variable into data chunks
            final byte[] dataChunk = Arrays.copyOfRange(data, i * 8, length);

            final Frame canFrame = new Frame(frameIdentifier.getFrameIdentifier(), true, dataChunk);

            Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.FINEST,
//                                Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.INFO,
                    "Outgoing\nFrameIdentifier: "
                    + Integer.toHexString(frameIdentifier.getFrameIdentifier())
                    + " (" + Integer.toBinaryString(frameIdentifier.getFrameIdentifier())
                    + " - dest: " + frameIdentifier.getDst()
                    + " - i: " + i
                    + " - nChunks: " + nChunks
                    + ")\n"
                    + "dataChunk: " + Arrays.toString(dataChunk));

            // Send chunk to the CAN bus
            this.connector.sendData2Kayak(canFrame);
        }

    }

    private void resendData(short transactionId, int remaining,
            int destinationNode, int virtualChannel) throws IOException {
        CFPRetransmissionMessageBuffer retransmissionBuffer;

        synchronized (retransmissionBuffers) {
            retransmissionBuffer = retransmissionBuffers.get(transactionId);
        }

        if (retransmissionBuffer == null) {
            throw new IOException("The retransmission buffer for transactionId: " + transactionId + " is no longer available!");
        }

        final Integer counter = retransmissionBuffer.getCounter();

        // Only allow MAX_NUMBER_OF_RETRANSMISSIONS attempts! After that, give up!
        if (counter > MAX_NUMBER_OF_RETRANSMISSIONS) {
            throw new IOException("The maximum number of retransmissions (" + MAX_NUMBER_OF_RETRANSMISSIONS + " times) was reached!");
        }

        // The CAN Frame was lost, resend!
        final byte[] data = retransmissionBuffer.getBuffer();

//        int nChunks = (data.length-1) / 8 + 1;
//        this.sendData(data, nChunks - remaining, transactionId, true);
//        this.sendData(data, nChunks - 1, transactionId, true);
        // All the segments are sent back again, that's why the line above is commented out
        this.sendData(data, 0, transactionId, false, destinationNode, virtualChannel);
        retransmissionBuffer.incrementCounter();
    }

    private int generateTypeFromChunkNumber(int chunkNumber, int chunksTotal) {
        // Start:    1
        // End:      2
        // Continue: 3
        return ((chunkNumber == 0) ? 1 : ((chunkNumber == chunksTotal - 1) ? 2 : 3));
    }

    private synchronized int generateTransactionId() throws InterruptedException {
        int id = cfpUniqueId.incrementAndGet() & ((1 << CFPFrameIdentifier.CFP_SIZE_TRANSACTION_ID) - 1);

        if (cfpUniqueId.get() != id) {
            cfpUniqueId.set(id);
        }

        return id;
    }

    public void close() {
        connector.close();
    }

    @Override
    public void newFrame(Frame frame) {
        // Add a counter to know how many messages are circulating on the bus
        messagesCounterDinamic.incrementAndGet();

        // New Frame received! :)
        Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.FINEST,
                //                        Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.INFO,
                "Incoming\nFrameIdentifier: "
                + Integer.toHexString(frame.getIdentifier())
                + " (" + Integer.toBinaryString(frame.getIdentifier())
                + ")\n"
                + "dataChunk: " + Arrays.toString(frame.getData()));

        // Translate the Frame Identifier to CFP Frame Identifier
        CFPFrameIdentifier frameIdentifier = new CFPFrameIdentifier(frame.getIdentifier());

        // Is it just an echo from socketcand ?
        if (frameIdentifier.getSrc() == node_source) {
            Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.FINEST,
                    "The chunk was not processed because it is an echo from socketcand!");

            return;
        }

        // Check if the destination is our node...  
        if ((frameIdentifier.getDst() & CFPFrameHandler.convertSrcToDestinationNode(this.node_source)) == 0) {
            Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.INFO,
                    "Rejecting because it is not for our node! Our Node: " + this.node_source
                    + " (when converted to dst: " + CFPFrameHandler.convertSrcToDestinationNode(this.node_source) + ")"
                    + " - received frame dst: " + frameIdentifier.getDst());

            return;
        }

        if (frame.getIdentifier() == 0 && frame.getLength() == 0) {
            Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.WARNING,
                    "The frame identifier is 0 and the frame data length is 0! Most likely the CAN interface is down.\n");
            
            return;
        }

        // Is it a request for retransmission?
        if (frameIdentifier.isRetransmissionRequest()) {
            Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.INFO,
                    "Retransmission request received for transactionId: " + frameIdentifier.getTransactionId());

            // The CCSDS Engine is the default destination
            int destinationNode = CFPFrameHandler.convertSrcToDestinationNode(frameIdentifier.getSrc());

            try {
                resendData(frameIdentifier.getTransactionId(), frameIdentifier.getRemain(), destinationNode, this.virtualChannel);
                Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.INFO,
                        "Retransmission success for transactionId: " + frameIdentifier.getTransactionId() + "! :)");
            } catch (IOException ex) {
//                Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.SEVERE, null, ex);
                return;
//                Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.SEVERE,
//                        "Retransmission failure for transactionId: " + frameIdentifier.getTransactionId() + "! :(", ex);
            }
            return;
        }

        // Is it a request to Wait?
        if (frameIdentifier.getSrc() == CANBusConnector.CAN_NODE_NR_SRC_WAIT) {
            this.connector.pauseBusActivity();
        }
        
        // Is it a request to Resume?
        if (frameIdentifier.getSrc() == CANBusConnector.CAN_NODE_NR_SRC_RESUME) {
            this.connector.continueBusActivity();
        }
        
        synchronized (MUTEX) {
            // Try to find the object to be reconstructed...
            ReconstructMessage message = incomingMessages.get(frameIdentifier.getTransactionId());

            if (message == null) {
                // Was this transactionId passed upwards on the last second?
                Long timestamp = passedUpwardsTimestamp.get(frameIdentifier.getTransactionId());

                if (timestamp != null) {
                    if (System.currentTimeMillis() - timestamp < 1000) {
                        // Then we don't need to create a new message.
                        // This must be done to avoid having the retransmissions constantly
                        // creating new messages here!
                        return;
                    }
                }

                // if it does not exist... then create new one
                message = new ReconstructMessage(frame, this);
                incomingMessages.put(frameIdentifier.getTransactionId(), message);
            } else {
                try {
                    message.addFrame(frame);
                } catch (IOException ex) {
                    Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (message.isRecontructed()) {
                this.readyQueue.offer(message);
                if (this.queuedForRetransmission.remove(message.getTransactionId())) {
                    Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.INFO,
                            "The message was successfully recovered! For transactionId: " + frameIdentifier.getTransactionId() + "! :)");

                }
            }
        }
    }

    public static int convertSrcToDestinationNode(final int src) {
        // The CCSDS Engine is the default destination
        int destinationNode = CANBusConnector.CAN_NODE_NR_DST_CCSDS;

        if (src == CANBusConnector.CAN_NODE_NR_SRC_SEPP) {
            destinationNode = CANBusConnector.CAN_NODE_NR_DST_SEPP;
        }

        if (src == CANBusConnector.CAN_NODE_NR_SRC_NANOMIND) {
            destinationNode = CANBusConnector.CAN_NODE_NR_DST_NANOMIND;
        }

        return destinationNode;
    }

}
