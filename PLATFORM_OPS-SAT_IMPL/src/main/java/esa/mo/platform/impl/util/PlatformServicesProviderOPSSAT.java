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
package esa.mo.platform.impl.util;

import esa.mo.com.impl.util.COMServicesProvider;
import esa.mo.com.impl.util.GMVServicesConsumer;
import esa.mo.platform.impl.provider.gen.CameraProviderServiceImpl;
import esa.mo.platform.impl.provider.gen.GPSProviderServiceImpl;
import esa.mo.platform.impl.provider.opssat.CameraOPSSATAdapter;
import esa.mo.platform.impl.provider.opssat.GPSOPSSATAdapter;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.platform.autonomousadcs.provider.AutonomousADCSInheritanceSkeleton;
import org.ccsds.moims.mo.platform.opticaldatareceiver.provider.OpticalDataReceiverInheritanceSkeleton;
import org.ccsds.moims.mo.platform.softwaredefinedradio.provider.SoftwareDefinedRadioInheritanceSkeleton;

/**
 *
 *
 */
public class PlatformServicesProviderOPSSAT implements PlatformServicesProviderInterface {

    private final CameraProviderServiceImpl cameraService = new CameraProviderServiceImpl();
    private final GPSProviderServiceImpl gpsService = new GPSProviderServiceImpl();

//    @Override
    public void init(COMServicesProvider comServices, GMVServicesConsumer gmvServicesConsumer) throws MALException {
        cameraService.init(comServices, new CameraOPSSATAdapter());
        gpsService.init(comServices, new GPSOPSSATAdapter(gmvServicesConsumer));
    }

    @Override
    public CameraProviderServiceImpl getCameraService() {
        return this.cameraService;
    }

    @Override
    public GPSProviderServiceImpl getGPSService() {
        return this.gpsService;
    }

    @Override
    public AutonomousADCSInheritanceSkeleton getAutonomousADCSService() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public OpticalDataReceiverInheritanceSkeleton getOpticalDataReceiverService() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SoftwareDefinedRadioInheritanceSkeleton getSoftwareDefinedRadioService() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
