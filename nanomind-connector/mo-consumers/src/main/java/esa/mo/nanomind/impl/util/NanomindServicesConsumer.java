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
package esa.mo.nanomind.impl.util;

import esa.mo.nanomind.impl.consumer.AggregationNanomindConsumerServiceImpl;
import esa.mo.nanomind.impl.consumer.ExperimentWDNanomindConsumerServiceImpl;
import esa.mo.nanomind.impl.consumer.GPSNanomindConsumerServiceImpl;
import esa.mo.nanomind.impl.consumer.PowerNanomindConsumerServiceImpl;
import esa.mo.helpertools.connections.SingleConnectionDetails;
import esa.mo.nanomind.impl.consumer.EventNanomindConsumerServiceImpl;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.URI;

/**
 *
 *
 */
public class NanomindServicesConsumer {
    private static final Logger LOGGER = Logger.getLogger(NanomindServicesConsumer.class.getName());

    private static volatile NanomindServicesConsumer instance;

    private GPSNanomindConsumerServiceImpl gpsNanomindService;
    private PowerNanomindConsumerServiceImpl powerNanomindService;
    private ExperimentWDNanomindConsumerServiceImpl experimentWDNanomindService;
    private AggregationNanomindConsumerServiceImpl aggregationService;
    private EventNanomindConsumerServiceImpl eventService;

    private static final String NANOMIND_APID = "10";  // Default Nanomind APID (on 13 June 2016)
    private static final String MAL_SPP_BINDINDING = "malspp"; // Use the SPP Implementation
    private static final String SOURCE_ID = "0"; // OBSW supports any value. By default it is set to 0

    private NanomindServicesConsumer() {

    }

    public static NanomindServicesConsumer getInstance() {
        if (instance == null) {
            synchronized (NanomindServicesConsumer.class) {
                if (instance == null) {
                    instance = new NanomindServicesConsumer();
                    instance.init();
                }
            }
        }
        return instance;
    }

    private void init() {
        // Enforce DLR's SPP
        System.setProperty("org.ccsds.moims.mo.mal.transport.protocol.malspp",
            "de.dlr.gsoc.mo.malspp.transport.SPPTransportFactory");
        System.setProperty("org.ccsds.moims.mo.mal.encoding.protocol.malspp",
            "de.dlr.gsoc.mo.malspp.encoding.SPPElementStreamFactory");
        System.setProperty("org.ccsds.moims.mo.malspp.test.spp.factory.class",
            "org.ccsds.moims.mo.testbed.util.sppimpl.tcp.TCPSPPSocketFactory");
        //        System.setProperty("helpertools.configurations.ground.Network", "SEPP");

        // Disable some flags
        System.setProperty("org.ccsds.moims.mo.malspp.authenticationIdFlag", "false");
        System.setProperty("org.ccsds.moims.mo.malspp.domainFlag", "false");
        System.setProperty("org.ccsds.moims.mo.malspp.networkZoneFlag", "false");
        System.setProperty("org.ccsds.moims.mo.malspp.sessionNameFlag", "false");
        System.setProperty("org.ccsds.moims.mo.malspp.timestampFlag", "false");

        final SingleConnectionDetails details;

        try {
            // Connection settings to the GPS Nanomind service
            details = new SingleConnectionDetails();
            final IdentifierList domain = new IdentifierList();
            domain.add(new Identifier("OPSSAT"));
            details.setBrokerURI((URI) null);
            details.setProviderURI(MAL_SPP_BINDINDING + ":247/" + NANOMIND_APID + "/" + SOURCE_ID);
            details.setDomain(domain);

            gpsNanomindService = new GPSNanomindConsumerServiceImpl(details);
            powerNanomindService = new PowerNanomindConsumerServiceImpl(details);
            experimentWDNanomindService = new ExperimentWDNanomindConsumerServiceImpl(details);
            aggregationService = new AggregationNanomindConsumerServiceImpl(details);
            eventService = new EventNanomindConsumerServiceImpl(details);
            eventService.registerDefaultEventHandler();

            // Add the other services!!

        } catch (final MALException | MalformedURLException | MALInteractionException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public GPSNanomindConsumerServiceImpl getGPSNanomindService() {
        return this.gpsNanomindService;
    }

    public PowerNanomindConsumerServiceImpl getPowerNanomindService() {
        return this.powerNanomindService;
    }

    public ExperimentWDNanomindConsumerServiceImpl getExperimentWDNanomindService() {
        return this.experimentWDNanomindService;
    }

    public AggregationNanomindConsumerServiceImpl getAggregationNanomindService() {
        return this.aggregationService;
    }

    public void setServices(final GPSNanomindConsumerServiceImpl gpsNanomindService,
        final PowerNanomindConsumerServiceImpl powerNanomindService,
        final ExperimentWDNanomindConsumerServiceImpl experimentWDNanomindService) {
        this.gpsNanomindService = gpsNanomindService;
        this.powerNanomindService = powerNanomindService;
        this.experimentWDNanomindService = experimentWDNanomindService;
    }

    public void setGPSNanomindService(final GPSNanomindConsumerServiceImpl gpsNanomindService) {
        this.gpsNanomindService = gpsNanomindService;
    }

    public void setPowerNanomindService(final PowerNanomindConsumerServiceImpl powerNanomindService) {
        this.powerNanomindService = powerNanomindService;
    }

    public void setExperimentWDNanomindService(
        final ExperimentWDNanomindConsumerServiceImpl experimentWDNanomindService) {
        this.experimentWDNanomindService = experimentWDNanomindService;
    }

    /**
     * Closes the service consumer connections
     *
     */
    public void closeConnections() {
        if (this.gpsNanomindService != null) {
            this.gpsNanomindService.close();
        }

        if (this.powerNanomindService != null) {
            this.powerNanomindService.close();
        }

        if (this.experimentWDNanomindService != null) {
            this.experimentWDNanomindService.close();
        }
    }

}
