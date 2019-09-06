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
import esa.mo.nanomind.impl.util.GMVServicesConsumer;
import esa.mo.nmf.MonitorAndControlNMFAdapter;
import esa.mo.nmf.nanosatmosupervisor.NanoSatMOSupervisor;
import esa.mo.nmf.nmfpackage.NMFPackagePMBackend;
import esa.mo.platform.impl.util.PlatformServicesConsumer;
import esa.mo.platform.impl.util.PlatformServicesProviderOPSSAT;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;

/**
 * The implementation of the NanoSat MO Supervisor for the OPS-SAT mission.
 *
 * @author Cesar Coelho
 */
public final class NanoSatMOSupervisorOPSSATImpl extends NanoSatMOSupervisor {

    private PlatformServicesProviderOPSSAT platformServicesOPSSAT;
    private GMVServicesConsumer gmvServicesConsumer;

    @Override
    public void init(final MonitorAndControlNMFAdapter mcAdapter) {
        init(mcAdapter, new PlatformServicesConsumer(), new NMFPackagePMBackend());
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
        supervisor.init(new MCOPSSATAdapter());
    }

}
