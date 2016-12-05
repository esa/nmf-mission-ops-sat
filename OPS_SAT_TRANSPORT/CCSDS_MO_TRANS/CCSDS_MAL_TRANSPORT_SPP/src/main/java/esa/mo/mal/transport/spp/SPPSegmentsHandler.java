/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO SPP Transport Framework
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
package esa.mo.mal.transport.spp;

import static esa.mo.mal.transport.spp.SPPBaseTransport.LOGGER;
import java.util.HashMap;
import java.util.logging.Level;
import org.ccsds.moims.mo.mal.MALException;

/**
 *
 * @author Cesar Coelho
 */
public class SPPSegmentsHandler {

    private final HashMap<Long, SPPSegmentsAssembler> segmentsAssemblerMap = new HashMap<Long, SPPSegmentsAssembler>(); // use TreeMap so that key is kept sorted
    private final int apidQualifier;
    private final int apid;
    private final SPPBaseTransport transport;

    SPPSegmentsHandler(SPPBaseTransport transport, int apidQualifier, int apid) 
    {
        this.transport = transport;
        this.apid = apid;
        this.apidQualifier = apidQualifier;
    }

    public void addSegment(int segmentFlags, byte[] packet) 
    {
        long localSSC = java.nio.ByteBuffer.wrap(packet).getShort(2) & 0x3FFF; // Mask to remove the sequence Flags
        
        int extra = (packet[26] & 0x80) != 0 ? 1 : 0; // Flags
        extra += (packet[26] & 0x40) != 0 ? 1 : 0; // Flags
        long segmentIndex = java.nio.ByteBuffer.wrap(packet).getInt(27 + extra);
        LOGGER.log(Level.FINE, "Segment index: " + segmentIndex + " - Local SSC: " + localSSC);

        long segAssemblerIndex = localSSC - segmentIndex;

        if (segAssemblerIndex < 0) 
        {  // Cope with transition zone
            segAssemblerIndex += 16384;
        }

        SPPSegmentsAssembler assembler;

        synchronized (segmentsAssemblerMap) 
        {
            assembler = segmentsAssemblerMap.get(segAssemblerIndex);

            if (assembler == null) 
            {
                assembler = new SPPSegmentsAssembler(segAssemblerIndex);
                segmentsAssemblerMap.put(segAssemblerIndex, assembler);
            }
        }

        SPPSegment segment = new SPPSegment(segmentIndex, segmentFlags, localSSC, packet);
        assembler.addSegment(segmentIndex, segment);
    }

    public byte[] getNextMessage() throws MALException 
    {
        synchronized (segmentsAssemblerMap) 
        {
            for (SPPSegmentsAssembler assembler : segmentsAssemblerMap.values()) 
            {
                if (assembler.isReady()) 
                {
                    byte[] out = assembler.getCompleteMessage(transport, apid, apidQualifier);
                    segmentsAssemblerMap.remove(assembler.getSequenceIndex());
                    return out;
                }
            }
        }

        return null;  // No messages found!
    }

    public boolean isEmpty() {
        return segmentsAssemblerMap.isEmpty();
    }
}
