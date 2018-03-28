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
package esa.mo.platform.impl.provider.opssat;

import esa.mo.com.impl.util.GMVServicesConsumer;
import esa.mo.platform.impl.provider.gen.AutonomousADCSAdapterInterface;
import java.io.IOException;
import org.ccsds.moims.mo.platform.autonomousadcs.structures.AttitudeDefinition;
import org.ccsds.moims.mo.platform.autonomousadcs.structures.AttitudeDefinitionBDot;
import org.ccsds.moims.mo.platform.autonomousadcs.structures.AttitudeInstance;

/**
 *
 * @author Cesar Coelho
 */
public class AutonomousADCSOPSSATAdapter implements AutonomousADCSAdapterInterface {

    private final GMVServicesConsumer gmvServicesConsumer;
    
    public AutonomousADCSOPSSATAdapter(GMVServicesConsumer gmvServicesConsumer){
        this.gmvServicesConsumer = gmvServicesConsumer;
    }
    
    @Override
    public void setDesiredAttitude(AttitudeDefinition attitude) throws IOException {

        if(attitude instanceof AttitudeDefinitionBDot){
            // Do something
            
        }

        
            // objInstId is of a definition that has pointing mode to NADIR
            
            
            
            /*------------------SIM------------------*/
            
            // instrumentSimulator.getFineADCSDevice.SetMode(byte[]);
            
            
            
            
            /*------------------Real OPS-SAT Implementation------------------*/
            
            // sendCommand(0x09, params);
            
            // sendToi2c(0x09 + params);
            
            // getFineADCSDevice.SetMode(byte[]);
            
            // convert 
        

    }

    @Override
    public void unset() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public AttitudeInstance getAttitudeInstance() throws IOException {

        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.


    }

    @Override
    public boolean isUnitAvailable() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
