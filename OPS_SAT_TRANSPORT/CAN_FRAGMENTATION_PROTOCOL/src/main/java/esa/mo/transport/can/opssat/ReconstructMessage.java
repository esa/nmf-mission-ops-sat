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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cesar Coelho
 */
public final class ReconstructMessage {

    // Max number of retransmissions
    private static final int MAX_NUMBER_OF_SEGMENTS = 32;
    private static final int TIMEOUT = 400;  // ms

    private final AtomicInteger numberRetrans = new AtomicInteger(0);

    private final short transactionId;
    private final HashMap<Integer, byte[]> segments;  // They are sorted by the remain field
    private final HashMap<Integer, Boolean> remainings;
    private int totalNumberOfSegments;
    private final CFPFrameHandler handler;
    private final AtomicInteger receivedMsgCounter;
    private boolean passedUpwards = false;
    private final int src;
    private long timestamp;
    private boolean isReconstructed = false;

    public ReconstructMessage(Frame newFrame, CFPFrameHandler handler) {

        this.handler = handler;
        this.segments = new HashMap<Integer, byte[]>(MAX_NUMBER_OF_SEGMENTS);
        this.remainings = new HashMap<Integer, Boolean>(MAX_NUMBER_OF_SEGMENTS);
        this.receivedMsgCounter = new AtomicInteger(1); // 1 because we received the first one in this method!
        this.timestamp = System.currentTimeMillis();

        // Initialize all as false
        for (int i = 0; i < MAX_NUMBER_OF_SEGMENTS; i++) {
            this.remainings.put(i, false);
        }

        CFPFrameIdentifier frameIdentifier = new CFPFrameIdentifier(newFrame.getIdentifier());
        this.remainings.put(frameIdentifier.getRemain(), true);
        this.segments.put(frameIdentifier.getRemain(), newFrame.getData());
        this.src = frameIdentifier.getSrc();

        if (frameIdentifier.getType() == 1) { // Starting Frame... perfect case
            this.totalNumberOfSegments = frameIdentifier.getRemain() + 1;
        } else {
            this.totalNumberOfSegments = -1; // Something else
        }

        this.transactionId = frameIdentifier.getTransactionId();

    }

    public int getSrc(){
        return this.src;
    }
    
    public synchronized void addFrame(final Frame newFrame) throws IOException {
        if (this.isRecontructed()) {
            return;  // It is done... ignore the received frame
        }

        this.receivedMsgCounter.incrementAndGet();
        CFPFrameIdentifier frameIdentifier = new CFPFrameIdentifier(newFrame.getIdentifier());

        // Did we already receive this frame before?
        if (this.remainings.get(frameIdentifier.getRemain())) {
            return; // We received it before... ignore it
        }

        if (transactionId != frameIdentifier.getTransactionId()) {
            throw new IOException("Wrong Frame transaction Id");
        }

        if (frameIdentifier.getType() == 1) { // Starting Frame
            this.totalNumberOfSegments = frameIdentifier.getRemain() + 1;
        }

        this.remainings.put(frameIdentifier.getRemain(), true);
        this.segments.put(frameIdentifier.getRemain(), newFrame.getData());
    }
    
    public short getTransactionId() {
        return this.transactionId;
    }

    public void setPassedUpwards(boolean passedUpwards) {
        this.passedUpwards = passedUpwards;
    }

    public boolean wasPassedUpwards() {
        return this.passedUpwards;
    }

    public synchronized boolean isRecontructed() {
        if(isReconstructed){ // To avoid recalculating it every single time...
            return true;
        }
        
        if (totalNumberOfSegments == -1) {
            // We don't even know how many segments there are yet...
            return false;
        }

        // Check here how many times we have received it...
        if (this.receivedMsgCounter.get() < totalNumberOfSegments) {
            return false;
        }

        int counter = 0;

        // Check all the remainings
        for (int i = 0; i < MAX_NUMBER_OF_SEGMENTS; i++) {
            if (this.remainings.get(i)) {
                counter++;
            }
        }
        
        isReconstructed = (counter == totalNumberOfSegments);
        return isReconstructed;
    }

    public synchronized byte[] reconstructData() throws IOException {

        if (!this.isRecontructed()) {
            throw new IOException("Unable to reconstruct the message because there are still missing parts!");
        }

        final int lastSegmentIndex = this.totalNumberOfSegments - 1;
        // Index 0 is actually the last segment...
        int bufferSize = (this.totalNumberOfSegments - 1) * 8 + this.segments.get(0).length;
        byte[] concatenated = new byte[bufferSize];

        // Recreate the output
        for (int i = 0; i < this.totalNumberOfSegments; i++) {
            final byte[] data = this.segments.get(i);
            Logger.getLogger(ReconstructMessage.class.getName()).log(Level.FINEST, "Segment(" + i + "/" + lastSegmentIndex + "): " + Arrays.toString(data));
//            Logger.getLogger(ReconstructMessage.class.getName()).log(Level.INFO, "Segment(" + i + "/" + lastSegmentIndex +"): " + Arrays.toString(data));
            System.arraycopy(data, 0, concatenated, (lastSegmentIndex - i) * 8, data.length);
        }

        Logger.getLogger(ReconstructMessage.class.getName()).log(Level.FINER, "Reconstructed data: " + Arrays.toString(concatenated));

        return concatenated;
    }

    private synchronized int findMissingSegment() throws IOException {

        // Do we have the total number of segments? If not, we have to sum +1 to the number of the highest returned remain
        if (totalNumberOfSegments != -1) {
            for (int i = totalNumberOfSegments - 1; i >= 0; i--) {
                if (!this.remainings.get(i)) {  // Didn't we receive this segment?
                    return i;  // Then it is the missing segment
                }
            }
        } else { // We don't know the total number of segments?
            // Then we have to find the highest remain received and sum 1:
            for (int i = MAX_NUMBER_OF_SEGMENTS - 1; i >= 0; i--) {
                if (this.remainings.get(i)) {  // Did we receive this segment?
                    return i + 1;  // Then sum 1 to have the segment before it
                }
            }
        }

        throw new IOException("Something went wrong! this.totalNumberOfSegments: " + this.totalNumberOfSegments + " - MAX_NUMBER_OF_SEGMENTS: " + MAX_NUMBER_OF_SEGMENTS);
    }

    public void requestRetransmissionForThis() throws IOException {
        try {
            if (numberRetrans.get() > 2) { // 3 strikes, you're out!
                throw new IOException("The retransmission timer has reached its maximum number of requests (3)! The timer will now stop requesting retransmissions for transactionId: " + transactionId);
            }

            numberRetrans.incrementAndGet();

            // Ask for a retransmission
            int missingSegment = findMissingSegment();
            Logger.getLogger(ReconstructMessage.class.getName()).log(Level.WARNING,
                    "Requesting a retransmission for the missingSegment '" + missingSegment
                    + "' out of a total of '" + totalNumberOfSegments + "' segments (transactionId: " + transactionId + ")");
            handler.requestRetransmission(transactionId, missingSegment, CFPFrameHandler.convertSrcToDestinationNode(this.src));
            this.timestamp = System.currentTimeMillis();
        } catch (IOException ex) {
            Logger.getLogger(ReconstructMessage.class.getName()).log(Level.SEVERE, "Retransmission request failed! Maybe this is the wrong handler...", ex);
        }
    }

}
