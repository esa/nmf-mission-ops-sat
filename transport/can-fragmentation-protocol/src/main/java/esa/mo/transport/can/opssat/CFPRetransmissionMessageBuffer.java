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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * CFP Frame Processing
 */
public class CFPRetransmissionMessageBuffer {

    private final byte[] buffer; // Retransmission Buffer
    private final AtomicInteger counter = new AtomicInteger(0); // Retransmission counter
    private final long timestamp;
    private final int destinationNode;
    private final int virtualChannel;

    /**
     * Constructor.
     *
     * @param data
     * @param destinationNode
     * @param virtualChannel
     */
    public CFPRetransmissionMessageBuffer(final byte[] data, 
            final int destinationNode, final int virtualChannel) {
        this.buffer = data;
        this.destinationNode = destinationNode;
        this.virtualChannel = virtualChannel;
        this.timestamp = System.currentTimeMillis();
    }

    public int getCounter(){
        return counter.get();
    }

    public int getDestinationNode(){
        return this.destinationNode;
    }

    public int getVirtualChannel(){
        return this.virtualChannel;
    }

    public byte[] getBuffer(){
        return buffer;
    }

    public int incrementCounter(){
        return counter.incrementAndGet();
    }
    
    public long getTimestamp(){
        return timestamp;
    }

}
