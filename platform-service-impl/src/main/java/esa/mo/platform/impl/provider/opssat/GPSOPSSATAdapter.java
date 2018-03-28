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
import esa.mo.platform.impl.provider.gen.GPSNMEAonlyAdapter;

/** 
 *
 * @author Cesar Coelho
 */
public class GPSOPSSATAdapter extends GPSNMEAonlyAdapter {
    
    private final GMVServicesConsumer gmvServicesConsumer;
    
    public GPSOPSSATAdapter(GMVServicesConsumer gmvServicesConsumer){
        this.gmvServicesConsumer = gmvServicesConsumer;
    }

    @Override
    public String getNMEASentence(String identifier) {
        
//        gmvServicesConsumer.getGPSNanomindService().getGPSNanomindStub().getGPSData(identifier, adapter);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isUnitAvailable() {
        return false;
    }

}
