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
package esa.mo.nanosatmoframework.apps;

import esa.mo.helpertools.helpers.HelperMisc;
import esa.mo.nanosatmoframework.MCRegistration;
import esa.mo.nanosatmoframework.MonitorAndControlNMFAdapter;
import esa.mo.transport.can.opssat.CANReceiveInterface;
import esa.mo.transport.can.opssat.CFPFrameHandler;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.provider.MALInteraction;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mc.structures.AttributeValueList;

/**
 * This class provides a blank template for starting the development of an app
 */
public class CanTestApp {

//    private final NanoSatMOFrameworkInterface nanoSatMOFramework = new NanoSatMOFrameworkMonolithicImpl(new MCAdapter());
    private static final boolean SEND_DATA_BOOL = true;
    private String parameterX = "";
    private final CFPFrameHandler handler;
    private final Timer timer;
    private int counter = 0;
    private String outString = "This is the last!";

    public CanTestApp() throws IOException {
        HelperMisc.loadPropertiesFile();

        handler = new CFPFrameHandler(new CANFrameAdapter());
        
        this.timer = new Timer();
        
        int init_delay = 3 * 1000;  // 3 seconds        
/*        
        for (int i = 1 ; i < 33 ; i++){
            String number = Long.toString(i);
            
            if (i<10){
                outString += number + "       ";
            }else{
                outString += number + "      ";
            }
        }
*/

        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    
//                    final byte[] array = outString.getBytes();
                    
                    if (SEND_DATA_BOOL){
                        handler.sendData(outString.getBytes());
                    }
                                        
                } catch (IOException ex) {
                    Logger.getLogger(CanTestApp.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(CanTestApp.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }, init_delay, 5000); // 25 milliseconds
                
    }

    /**
     * Main command line entry point.
     *
     * @param args the command line arguments
     * @throws java.lang.Exception If there is an error
     */
    public static void main(final String args[]) throws Exception {
        CanTestApp demo = new CanTestApp();
    }

    public class MCAdapter extends MonitorAndControlNMFAdapter {

        @Override
        public void initialRegistrations(MCRegistration registrationObject) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Attribute onGetValue(Identifier identifier, Byte rawType) {
            return null;
        }

        @Override
        public Boolean onSetValue(Identifier identifier, Attribute value) {
            
            if ("sendValue".equals(identifier.toString())){ try {
                // parameterX was called?
                handler.sendData(value.toString().getBytes());
                } catch (IOException ex) {
                    Logger.getLogger(CanTestApp.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(CanTestApp.class.getName()).log(Level.SEVERE, null, ex);
                }
            
                return true;
            }
            
            return false;  // to confirm that the variable was set
        }

        @Override
        public UInteger actionArrived(Identifier name, AttributeValueList attributeValues, Long actionInstanceObjId, boolean reportProgress, MALInteraction interaction) {
            return null;  // Action service not integrated
        }



    }
    
    public class CANFrameAdapter implements CANReceiveInterface{

        @Override
        public void receive(byte[] message) {
            try {
                String string = new String(message, "UTF-8");
                Logger.getLogger(CanTestApp.class.getName()).log(Level.INFO, string);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(CanTestApp.class.getName()).log(Level.SEVERE, null, ex);
            }
            
  //          nanoSatMOFramework.pushParameterValue("CAN received", message);
            
        }
        
    }
    

}
