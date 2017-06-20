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
        this.transactionId = (int) ( ( frameIdentifier >> position ) & CFPFrameIdentifier.generateMask(CFPFrameHandler.CFP_SIZE_TRANSACTION_ID) );
        position += CFPFrameHandler.CFP_SIZE_TRANSACTION_ID;
        this.remain = (int) ( ( frameIdentifier >> position ) & CFPFrameIdentifier.generateMask(CFPFrameHandler.CFP_SIZE_REMAIN) );
        position += CFPFrameHandler.CFP_SIZE_REMAIN;
        this.type = (int) ( ( frameIdentifier >> position ) & CFPFrameIdentifier.generateMask(CFPFrameHandler.CFP_SIZE_TYPE) );
        position += CFPFrameHandler.CFP_SIZE_TYPE;
        this.dst = (int) ( ( frameIdentifier >> position ) & CFPFrameIdentifier.generateMask(CFPFrameHandler.CFP_SIZE_DST) );
        position += CFPFrameHandler.CFP_SIZE_DST;
        this.src = (int) ( ( frameIdentifier >> position ) & CFPFrameIdentifier.generateMask(CFPFrameHandler.CFP_SIZE_SRC) );
        position += CFPFrameHandler.CFP_SIZE_SRC;
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
        position += CFPFrameHandler.CFP_SIZE_TRANSACTION_ID;
        this.frameIdentifier |= ((new Long(this.remain)) << position);
        position += CFPFrameHandler.CFP_SIZE_REMAIN;
        this.frameIdentifier |= ((new Long(this.type)) << position);
        position += CFPFrameHandler.CFP_SIZE_TYPE;
        this.frameIdentifier |= ((new Long(this.dst)) << position);
        position += CFPFrameHandler.CFP_SIZE_DST;
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

    public boolean isSTART(){
        return (this.type == 1);
    }

    public boolean isEND(){
        return (this.type == 2);
    }

    public boolean isCONTINUE(){
        return (this.type == 3);
    }
    
    private static long generateMask(int size){
        return ((1 << size) - 1);
    }
    
    public boolean isRetransmissionRequest(){
        return (this.getType() == 0);
    }
    
    @Override
    public String toString(){
        return "Frame Identifier - src: " + this.src +  " dst: " + this.dst +  
                " type: " + this.type +  " remain: " + this.remain +  " transactionId: " + this.transactionId;
    }
   
}
