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

import com.github.kayak.core.Bus;
import com.github.kayak.core.BusURL;
import com.github.kayak.core.Frame;
import com.github.kayak.core.FrameListener;
import com.github.kayak.core.Subscription;
import com.github.kayak.core.TimeSource;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CANBusConnector - Connects to socketcand via a socket (the kayak library
 * does that) for sending and receiving CAN messages on a bus.
 *
 */
public class CANBusConnector {
    
    // CAN Nodes DST
    public static final int CAN_NODE_NR_DST_SEPP = 8;
    public static final int CAN_NODE_NR_DST_CCSDS = 16;
    public static final int CAN_NODE_NR_DST_NANOMIND = 32;

    // CAN Nodes SRC
    public static final int CAN_NODE_NR_SRC_ABORT = 0;
    public static final int CAN_NODE_NR_SRC_WAIT = 1;
    public static final int CAN_NODE_NR_SRC_RESUME = 2;
    public static final int CAN_NODE_NR_SRC_CCSDS_ENGINE = 4;
    public static final int CAN_NODE_NR_SRC_NANOMIND = 5;
    public static final int CAN_NODE_NR_SRC_SEPP = 6;
    
    /* Configuration settings */
    private static final String DEFAULT_BUS = "can0";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 29536; // Default port number of socketcand

    private static final String PROPERTY_BUS = "esa.mo.transport.can.opssat.bus";
    private static final String PROPERTY_HOST = "esa.mo.transport.can.opssat.host";
    private static final String PROPERTY_PORT = "esa.mo.transport.can.opssat.port";
    private static final String PROPERTY_N_MESSAGES = "esa.mo.transport.can.opssat.nMessages";
    private static final String PROPERTY_INTERVAL = "esa.mo.transport.can.opssat.interval";

    // The 10 is there to avoid flooding the queue. In multi-threading, this means
    // that other threads will still be able to send messages in parallel
    private final LinkedBlockingQueue<Frame> queue = new LinkedBlockingQueue<Frame>(10);
    private Thread sendingThread = null;
    private CountDownLatch countDown = new CountDownLatch(0);

    // The lowest maximum that CAN can support (1M/128 = 7'812 msgs/sec)
    // N_MESSAGES / N_MILLISECONDS * 1000 < 7812!
    private int nMessages = 1; // Keep as 1 for now, we can try to speed it up later
    private int nInterval = 1;
    
    private final Bus bus = new Bus();

    public CANBusConnector(final FrameListener receiver) throws IOException {

        String bus_string = (System.getProperty(PROPERTY_BUS) != null) ? System.getProperty(PROPERTY_BUS) : DEFAULT_BUS;
        String host_string = (System.getProperty(PROPERTY_HOST) != null) ? System.getProperty(PROPERTY_HOST) : DEFAULT_HOST;
        int port = (System.getProperty(PROPERTY_PORT) != null) ? Integer.parseInt(System.getProperty(PROPERTY_PORT)) : DEFAULT_PORT;
        nMessages = (System.getProperty(PROPERTY_N_MESSAGES) != null) ? Integer.parseInt(System.getProperty(PROPERTY_N_MESSAGES)) : nMessages;
        nInterval = (System.getProperty(PROPERTY_INTERVAL) != null) ? Integer.parseInt(System.getProperty(PROPERTY_INTERVAL)) : nInterval;

        final BusURL url = new BusURL(host_string, port, bus_string);

        if (!url.checkConnection()) {
            throw new IOException();
        }

        TimeSource ts = new TimeSource();
        bus.setConnection(url);
        bus.setTimeSource(ts);

        Logger.getLogger(CANBusConnector.class.getName()).log(Level.INFO,
                "Connected to socketcand (host: " + host_string + "; port: "
                + port + "; bus: " + bus_string + ")");

        /* Subscribe to all frames */
        Subscription s = new Subscription(receiver, bus);
        s.setSubscribeAll(true);
        ts.play();
    }
    
    public void init(){
        sendingThread = new Thread() {
            @Override
            public void run() {
                this.setName("CANBusConnector_startSendingThread");
                Frame canFrame;
                int counter = 0;
                while (true) {
                    try {
                        // This code is absolutely ugly however this is a workaround
                        // for the problem on the CAN bus. The CAN bus will add a
                        // couple of zeros randomly in the middle of the body of
                        // the CAN frame. This code below can be replaced after 
                        // the problem with the CAN bus has been fixed.
                        
                        for (int i = 0; i < nMessages; i++) {
                            canFrame = queue.take();
                            
                            countDown.await();

                            bus.sendFrame(canFrame);
/*
                            if (counter < 1000){
                                bus.sendFrame(canFrame);
                                counter++;
                            }else{
                                counter = 0;
                            }
*/
                        }

                        if(nInterval != 0){
                            Thread.sleep(nInterval); // Every N_MESSAGES messages, wait N_SECONDS ms
                        }
                    } catch (InterruptedException e) {
                        Logger.getLogger(CANBusConnector.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            }
        };

        sendingThread.start();
    }

    /**
     * Sends a CAN Frame to Kayak. The method includes queueing of messages to
     * avoid crashing the linux CAN driver with high data rates.
     *
     * @param canFrame The CAN Frame
     */
    public void sendData2Kayak(final Frame canFrame) {
        try {
            queue.put(canFrame);
        } catch (InterruptedException e) {
            Logger.getLogger(CANBusConnector.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void close() {
        bus.destroy();
    }

    public synchronized void continueBusActivity() {
        countDown.countDown();
    }

    public synchronized void pauseBusActivity() {
        // Create a new one only if the counter is down to zero
        if (countDown.getCount() == 0){ 
            countDown = new CountDownLatch(1);
        }
    }
    
}
