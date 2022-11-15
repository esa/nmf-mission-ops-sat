/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under European Space Agency Public License (ESA-PL) Weak Copyleft – v2.4
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
package esa.mo.platform.impl.util;

import esa.mo.com.impl.util.COMServicesProvider;
import esa.mo.helpertools.connections.ConnectionConsumer;
import esa.mo.platform.impl.provider.gen.PowerControlProviderServiceImpl;
import esa.mo.platform.impl.provider.gen.CameraProviderServiceImpl;
import esa.mo.platform.impl.provider.gen.GPSProviderServiceImpl;
import esa.mo.platform.impl.provider.gen.AutonomousADCSProviderServiceImpl;
import esa.mo.platform.impl.provider.gen.OpticalDataReceiverProviderServiceImpl;
import esa.mo.platform.impl.provider.gen.SoftwareDefinedRadioProviderServiceImpl;
import esa.mo.platform.impl.provider.opssat.CameraOPSSATAdapter;
import esa.mo.platform.impl.provider.opssat.GPSOPSSATAdapter;
import esa.mo.platform.impl.provider.opssat.AutonomousADCSOPSSATAdapter;
import esa.mo.platform.impl.provider.opssat.OpticalRxOPSSATAdapter;
import esa.mo.platform.impl.provider.opssat.PowerControlOPSSATAdapter;
import esa.mo.platform.impl.provider.opssat.SDROPSSATAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;

/**
 *
 *
 */
public class PlatformServicesProviderOPSSAT implements PlatformServicesProviderInterface {

    private final static Logger LOGGER = Logger.getLogger(PlatformServicesProviderOPSSAT.class.getName());
    private final AutonomousADCSProviderServiceImpl adcsService = new AutonomousADCSProviderServiceImpl();
    private final CameraProviderServiceImpl cameraService = new CameraProviderServiceImpl();
    private final GPSProviderServiceImpl gpsService = new GPSProviderServiceImpl();
    private final OpticalDataReceiverProviderServiceImpl optrxService = new OpticalDataReceiverProviderServiceImpl();
    private final PowerControlProviderServiceImpl powerService = new PowerControlProviderServiceImpl();
    private final SoftwareDefinedRadioProviderServiceImpl sdrService = new SoftwareDefinedRadioProviderServiceImpl();
    private PowerControlOPSSATAdapter pcAdapter = null;

    @Override
    public void init(final COMServicesProvider comServices) throws MALException {
        try {
            pcAdapter = new PowerControlOPSSATAdapter();
            powerService.init(pcAdapter);
            gpsService.init(comServices, new GPSOPSSATAdapter(pcAdapter));
            adcsService.init(comServices, new AutonomousADCSOPSSATAdapter(pcAdapter));
            cameraService.init(comServices, new CameraOPSSATAdapter(pcAdapter));
            optrxService.init(new OpticalRxOPSSATAdapter(pcAdapter));
            sdrService.init(new SDROPSSATAdapter(pcAdapter));
        } catch (UnsatisfiedLinkError | NoClassDefFoundError | NoSuchMethodError error) {
            LOGGER.log(Level.SEVERE, "Could not load platform adapters (check for missing JARs and libraries)", error);
        }
    }

    @Override
    public void startStatusTracking(ConnectionConsumer connection) {
        pcAdapter.startStatusTracking(connection);
    }

    @Override
    public GPSProviderServiceImpl getGPSService() {
        return this.gpsService;
    }

    @Override
    public CameraProviderServiceImpl getCameraService() {
        return this.cameraService;
    }

    @Override
    public AutonomousADCSProviderServiceImpl getAutonomousADCSService() {
        return this.adcsService;
    }

    @Override
    public OpticalDataReceiverProviderServiceImpl getOpticalDataReceiverService() {
        return this.optrxService;
    }

    @Override
    public SoftwareDefinedRadioProviderServiceImpl getSoftwareDefinedRadioService() {
        return this.sdrService;
    }

}
