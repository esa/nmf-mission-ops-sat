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

/**
 *
 * @author Cesar Coelho
 */
public class CFPFrameIdentifier {

    private static final int CFP_SIZE_SRC = 3;
    private static final int CFP_SIZE_DST = 6;
    private static final int CFP_SIZE_TYPE = 2;
    private static final int CFP_SIZE_REMAIN = 5;
    public static final int CFP_SIZE_TRANSACTION_ID = 13;

    private final int src;
    private final int dst;
    private final int type;
    private final int remain;
    private final int transactionId;
    
    private int frameIdentifier;
    
    /**
     * Constructor.
     *
     * @param frameIdentifier
     */
    public CFPFrameIdentifier(final int frameIdentifier) {
        this.frameIdentifier = frameIdentifier;
        
        int position = 0;
        this.transactionId = (int) ( ( frameIdentifier >> position ) & CFPFrameIdentifier.generateMask(CFP_SIZE_TRANSACTION_ID) );
        position += CFP_SIZE_TRANSACTION_ID;
        this.remain = (int) ( ( frameIdentifier >> position ) & CFPFrameIdentifier.generateMask(CFP_SIZE_REMAIN) );
        position += CFP_SIZE_REMAIN;
        this.type = (int) ( ( frameIdentifier >> position ) & CFPFrameIdentifier.generateMask(CFP_SIZE_TYPE) );
        position += CFP_SIZE_TYPE;
        this.dst = (int) ( ( frameIdentifier >> position ) & CFPFrameIdentifier.generateMask(CFP_SIZE_DST) );
        position += CFP_SIZE_DST;
        this.src = (int) ( ( frameIdentifier >> position ) & CFPFrameIdentifier.generateMask(CFP_SIZE_SRC) );
        position += CFP_SIZE_SRC;
    }
    
    /**
     * Constructor.
     *
     * @param src Source
     * @param dst Destination
     * @param type Type
     * @param remain Remain
     * @param transactionId Transaction Id
     */
    public CFPFrameIdentifier(final int src, final int dst, final int type, 
            final int remain, final int transactionId){
        this.src = src;
        this.dst = dst;
        this.type = type;
        this.remain = remain;
        this.transactionId = transactionId;
        
        int position = 0;
        this.frameIdentifier = 0;
        this.frameIdentifier |= ((new Long(this.transactionId)) << position);
        position += CFP_SIZE_TRANSACTION_ID;
        this.frameIdentifier |= ((new Long(this.remain)) << position);
        position += CFP_SIZE_REMAIN;
        this.frameIdentifier |= ((new Long(this.type)) << position);
        position += CFP_SIZE_TYPE;
        this.frameIdentifier |= ((new Long(this.dst)) << position);
        position += CFP_SIZE_DST;
        this.frameIdentifier |= ((new Long(this.src)) << position);

    }

    public int getFrameIdentifier(){
        return this.frameIdentifier;
    }
    
    public int getSrc(){
        return this.src;
    }

    public int getDst(){
        return this.dst;
    }

    public short getTransactionId(){
        return (short) this.transactionId;
    }

    public int getRemain(){
        return this.remain;
    }

    public int getType(){
        return this.type;
    }

    private static long generateMask(int size){
        return ((1 << size) - 1);
    }
    
    public boolean isRetransmissionRequest(){
        return (this.getType() == 0);
    }
    
}
