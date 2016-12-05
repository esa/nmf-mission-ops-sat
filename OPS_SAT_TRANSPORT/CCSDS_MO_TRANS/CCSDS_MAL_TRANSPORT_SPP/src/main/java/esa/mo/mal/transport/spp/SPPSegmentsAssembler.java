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

import java.util.TreeMap;
import org.ccsds.moims.mo.mal.MALException;

/**
 *
 * @author Cesar Coelho
 */
public class SPPSegmentsAssembler 
{

    private final TreeMap<Long, SPPSegment> segmentsMap = new TreeMap<Long, SPPSegment>(); // use TreeMap so that key is kept sorted
    private final long sequenceIndex;
    private int totalSize = 0;
    private boolean receivedFirst = false;
    private boolean receivedLast = false;

    SPPSegmentsAssembler(long sequenceIndex) 
    {
        this.sequenceIndex = sequenceIndex;
    }

    public long getSequenceIndex() 
    {
        return sequenceIndex;
    }

    public boolean isReady() 
    {
        if (!receivedLast) 
        { // Most likely to occur
            return false;
        }

        if (!receivedFirst) 
        {
            return false;
        }

        int segChecker = 0;

        // check if we indeed have all of them without gaps...
        for (SPPSegment segment : segmentsMap.values()) 
        {
            if (segment.getSegmentIndex() != segChecker++) 
            {
                return false;
            }
        }

        return true;
    }

    public void addSegment(long segmentIndex, SPPSegment segment) 
    {
        segmentsMap.put(segmentIndex, segment);

        if (segment.isFirst()) 
        {
            receivedFirst = true;
        }

        if (segment.isLast()) 
        {
            receivedLast = true;
        }

        totalSize += segment.getPacket().length;
    }

    public byte[] getCompleteMessage(final SPPBaseTransport transport,
            final int apid, final int apidQualifier) throws MALException 
    {
        byte[] concatenated = new byte[totalSize];
        byte[] tmpPacket;
        int index = 0;

        for (SPPSegment segment : segmentsMap.values()) 
        { // Tree garantees order
            if (segment.isFirst())
            {
                System.arraycopy(segment.getPacket(), 0, concatenated, 0, segment.getPacket().length);
                index = segment.getPacket().length;
                continue;
            }

            SPPMessage msg = transport.internalDecodeMessageHeader(apidQualifier, apid, segment.getPacket());
            tmpPacket = msg.getBody().getEncodedBody().getEncodedBody().getValue();

            System.arraycopy(tmpPacket, 0, concatenated, index, tmpPacket.length);
            index += tmpPacket.length;

            if (segment.isLast()) 
            {
                byte[] out = new byte[index]; // Trim to fit!
                System.arraycopy(concatenated, 0, out, 0, index);
                return out;
            }
        }
        
        return null;
    }

}
