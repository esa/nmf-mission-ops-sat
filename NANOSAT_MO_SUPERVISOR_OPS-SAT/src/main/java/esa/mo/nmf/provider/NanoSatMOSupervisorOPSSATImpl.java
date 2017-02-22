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
package esa.mo.nmf.provider;

import esa.mo.com.impl.util.COMServicesProvider;
import esa.mo.com.impl.util.GMVServicesConsumer;
import esa.mo.nmf.nanosatmosupervisor.NanoSatMOSupervisor;
import esa.mo.nmf.packager.PackageManagementBackendNMFPackage;
import esa.mo.platform.impl.util.PlatformServicesConsumer;
import esa.mo.platform.impl.util.PlatformServicesProviderOPSSAT;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;

/**
 * A Provider of MO services composed by COM, M&C and Platform services. Selects
 * the transport layer based on the selected values of the properties file and
 * initializes all services automatically. Provides configuration persistence,
 * therefore the last state of the configuration of the MO services will be kept
 * upon restart. Additionally, the NanoSat MO Framework implements an
 * abstraction layer over the Back-End of some MO services to facilitate the
 * monitoring of the business logic of the app using the NanoSat MO Framework.
 *
 * @author Cesar Coelho
 */
public class NanoSatMOSupervisorOPSSATImpl extends NanoSatMOSupervisor {

    private PlatformServicesProviderOPSSAT platformServicesOPSSAT;
    private GMVServicesConsumer gmvServicesConsumer;

    private final static int IMAGE_LENGTH = 2048;
    private final static int IMAGE_WIDTH = 1944;

    /**
     * NanoSat MO Supervisor for OPS-SAT
     *
     */
    public NanoSatMOSupervisorOPSSATImpl() {
        super(new MCOPSSATAdapter(),
                new PlatformServicesConsumer(),
                new PackageManagementBackendNMFPackage());
    }

    @Override
    public void initPlatformServices(COMServicesProvider comServices) {
        // Initialize the consumers to the Nanomind
        gmvServicesConsumer = new GMVServicesConsumer();
        gmvServicesConsumer.init();

        try {
            platformServicesOPSSAT = new PlatformServicesProviderOPSSAT();
            platformServicesOPSSAT.init(comServices, gmvServicesConsumer);
        } catch (MALException ex) {
            Logger.getLogger(NanoSatMOSupervisorOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    /**
     * Main command line entry point.
     *
     * @param args the command line arguments
     * @throws java.lang.Exception If there is an error
     */
    public static void main(final String args[]) throws Exception {
        NanoSatMOSupervisorOPSSATImpl supervisor = new NanoSatMOSupervisorOPSSATImpl();
    }

}
