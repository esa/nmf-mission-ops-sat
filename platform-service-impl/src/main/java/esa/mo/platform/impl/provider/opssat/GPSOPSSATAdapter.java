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
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import esa.opssat.nanomind.opssat_pf.gps.consumer.GPSAdapter;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.structures.Blob;

/**
 *
 * @author Cesar Coelho
 */
public class GPSOPSSATAdapter extends GPSNMEAonlyAdapter {

    private final GMVServicesConsumer gmvServicesConsumer;

    public GPSOPSSATAdapter(final GMVServicesConsumer gmvServicesConsumer) {
        this.gmvServicesConsumer = gmvServicesConsumer;
    }

    @Override
    public String getNMEASentence(final String identifier) throws IOException {
        final GPSHandler gpsHandler = new GPSHandler();
        try {
            gmvServicesConsumer.getGPSNanomindService().getGPSNanomindStub().getGPSData(new Blob(identifier.getBytes()), gpsHandler);
        } catch (final IllegalArgumentException | MALException | MALInteractionException e) {
            throw new IOException("Error when retrieving GPS NMEA response from Nanomind", e);
        }
        return gpsHandler.response;
    }

    @Override
    public boolean isUnitAvailable() {
        return true;
    }

    private class GPSHandler extends GPSAdapter {
        String response = "";
        @Override
        public void getGPSDataResponseReceived(final org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader,
                                               final org.ccsds.moims.mo.mal.structures.Blob data, final java.util.Map qosProperties) {
            try {
                response = Arrays.toString(data.getValue());
            } catch (final MALException ex) {
                Logger.getLogger(GPSHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
