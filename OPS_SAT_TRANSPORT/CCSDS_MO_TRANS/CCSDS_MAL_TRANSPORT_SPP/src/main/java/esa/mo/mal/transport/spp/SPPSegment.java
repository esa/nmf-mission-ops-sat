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

/**
 *
 * @author Cesar Coelho
 */
public class SPPSegment 
{
    private final long segmentIndex;
    private final byte[] packet;
    private final long ssc;
    private final boolean isFirst;
    private final boolean isLast;

    SPPSegment(long segmentIndex, int segmentFlags, long ssc, byte[] packet) 
    {
        this.segmentIndex = segmentIndex;
        this.ssc = ssc;
        this.packet = packet;
        this.isFirst = (1 == segmentFlags);
        this.isLast = (2 == segmentFlags);
    }

    public boolean isFirst() 
    {
        return this.isFirst;
    }

    public boolean isLast() 
    {
        return this.isLast;
    }

    public byte[] getPacket()
    {
        return packet;
    }
    
    public long getSegmentIndex()
    {
        return segmentIndex;
    }
    
    public long getSSC()
    {
        return ssc;
    }
}
