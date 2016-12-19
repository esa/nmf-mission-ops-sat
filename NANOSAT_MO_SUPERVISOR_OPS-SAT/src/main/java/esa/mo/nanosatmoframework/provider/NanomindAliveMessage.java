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
package esa.mo.nanosatmoframework.provider;

import esa.mo.com.impl.util.GMVServicesConsumer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;

/**
 *
 * @author Cesar Coelho
 */
public class NanomindAliveMessage {

    private final GMVServicesConsumer gmvServicesConsumer;
    private final Timer timer;
    private static final int PERIOD = 10000;  // 10 seconds
    private boolean active = false;
    private final int apid;

    public NanomindAliveMessage(final GMVServicesConsumer gmvServicesConsumer, final int apid) {
        this.apid = apid;
        this.timer = new Timer();
        this.gmvServicesConsumer = gmvServicesConsumer;
    }

    public void init() {
        
        // To do: Spoofing the APID is still not being done...

        // Start the periodic reporting here!
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (active) {
                    try {
                        gmvServicesConsumer.getExperimentWDNanomindService().getExperimentWDNanomindStub().alive();
                    } catch (MALInteractionException ex) {
                        Logger.getLogger(NanomindAliveMessage.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (MALException ex) {
                        Logger.getLogger(NanomindAliveMessage.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }, PERIOD, PERIOD); // the time has to be converted to milliseconds by multiplying by 1000

    }
    
    public void setActive(boolean active){
        this.active = active;
    }
    

}
