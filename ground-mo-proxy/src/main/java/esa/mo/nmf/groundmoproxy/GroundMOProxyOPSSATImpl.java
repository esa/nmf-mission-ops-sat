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
package esa.mo.nmf.groundmoproxy;

import esa.mo.com.impl.consumer.ArchiveSyncConsumerServiceImpl;
import esa.mo.com.impl.util.COMObjectStructure;
import esa.mo.helpertools.connections.SingleConnectionDetails;
import esa.mo.helpertools.helpers.HelperTime;
import esa.mo.helpertools.misc.Const;
import esa.mo.mc.impl.consumer.ActionConsumerServiceImpl;
import esa.mo.mc.impl.proxy.ActionProxyServiceImpl;
import esa.mo.sm.impl.provider.AppsLauncherManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.com.COMHelper;
import org.ccsds.moims.mo.com.COMService;
import org.ccsds.moims.mo.com.archive.provider.QueryInteraction;
import org.ccsds.moims.mo.com.archive.structures.ArchiveDetails;
import org.ccsds.moims.mo.com.archive.structures.ArchiveDetailsList;
import org.ccsds.moims.mo.com.archive.structures.ArchiveQuery;
import org.ccsds.moims.mo.com.archive.structures.ArchiveQueryList;
import org.ccsds.moims.mo.com.archivesync.ArchiveSyncHelper;
import org.ccsds.moims.mo.com.archivesync.body.GetTimeResponse;
import org.ccsds.moims.mo.com.structures.ObjectType;
import org.ccsds.moims.mo.com.structures.ObjectTypeList;
import org.ccsds.moims.mo.common.directory.structures.ProviderSummaryList;
import org.ccsds.moims.mo.common.directory.structures.ServiceFilter;
import org.ccsds.moims.mo.common.structures.ServiceKey;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.structures.ElementList;
import org.ccsds.moims.mo.mal.structures.FineTime;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.UIntegerList;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.transport.MALMessage;
import org.ccsds.moims.mo.mc.action.ActionHelper;

/**
 * The Ground MO Proxy for OPS-SAT
 *
 * @author Cesar Coelho
 */
public class GroundMOProxyOPSSATImpl extends GroundMOProxy {
    private static final Logger LOGGER = Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName());

    private final ProtocolBridgeSPP protocolBridgeSPP = new ProtocolBridgeSPP();
    private final HashMap<IdentifierList, URI> actionURIs = new HashMap<>();

    /**
     * Ground MO Proxy for OPS-SAT
     *
     */
    public GroundMOProxyOPSSATImpl() {
        super();

        // Initialize the protocol bridge services and expose them using TCP/IP!
        final Map properties = System.getProperties();

        String protocol = System.getProperty("org.ccsds.moims.mo.mal.transport.default.protocol");

        // Default it to tcp if the property is not defined
        protocol = (protocol != null) ? protocol.split(":")[0] : "maltcp";

        // The range of APIDs below were formally requested 
        // And are uniquely assigned for the Ground MO Proxy of OPS-SAT
        properties.put(ProtocolBridgeSPP.PROPERTY_APID_RANGE_START, "1450");
        properties.put(ProtocolBridgeSPP.PROPERTY_APID_RANGE_END, "1499");

        // Initialize the SPP Protocol Bridge
        try {
            // TCP/IP is the selected transport binding for the bridge with SPP
            protocolBridgeSPP.init(protocol, properties);
            final URI routedURI = protocolBridgeSPP.getRoutingProtocol();

            // Initialize the pure protocol bridge for the services without extension
            final URI centralDirectoryServiceURI = new URI("malspp:247/100/5");
            super.init(centralDirectoryServiceURI, routedURI);

            final URI uri = super.getDirectoryServiceURI();
            LOGGER.log(Level.INFO,
                    "Ground MO Proxy initialized! URI: " + uri + "\n");
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE,
                    "The SPP Protocol Bridge could not be initialized!", ex);
        }
    }

    @Override
    public synchronized void additionalHandling() {
        final IdentifierList domain = new IdentifierList();
        domain.add(new Identifier("*"));

        // Add the Action service rerouting stuff
        COMService serviceType = ActionHelper.ACTION_SERVICE;
        ServiceKey serviceKey = new ServiceKey(serviceType.getArea().getNumber(),
                serviceType.getNumber(), serviceType.getArea().getVersion());
        ServiceFilter sf = new ServiceFilter(new Identifier("*"),
                domain, new Identifier("*"), null, new Identifier("*"),
                serviceKey, new UIntegerList());

        try {
            final ProviderSummaryList actionsCD = localDirectoryService.lookupProvider(sf, null);

            // Cycle through the NMF Apps and sync them!
            for (int i = 0; i < actionsCD.size(); i++) {
                final ProviderSummaryList psl = new ProviderSummaryList();
                psl.add(actionsCD.get(i));
                // Needs some work!

                try {
                    final SingleConnectionDetails connectionDetails = AppsLauncherManager.getSingleConnectionDetailsFromProviderSummaryList(psl);
                    try {
                        synchronized (actionURIs) {
                            URI localActionURI = actionURIs.get(connectionDetails.getDomain());

                            if (localActionURI == null) {
                                // This only needs to be done in case it still does not exist:
                                final ActionConsumerServiceImpl actionConsumer = new ActionConsumerServiceImpl(connectionDetails, null);
                                final ActionProxyServiceImpl actionProxyService = new ActionProxyServiceImpl();
                                actionProxyService.init(localCOMServices, actionConsumer);
                                localActionURI = actionProxyService.getConnectionProvider().getConnectionDetails().getProviderURI();
                                actionURIs.put(connectionDetails.getDomain(), localActionURI);
                            }

                            super.localDirectoryService.rerouteActionServiceURI(connectionDetails.getDomain(), localActionURI);
                        }
                    } catch (final MalformedURLException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                } catch (final IOException ex) {
                    // The Action service does not exist on this provider...
                    // Do nothing!
                }

            }
        } catch (final MALInteractionException | MALException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        // ---------------------
        // Sync the COM Archives
        // ---------------------
        serviceType = ArchiveSyncHelper.ARCHIVESYNC_SERVICE;
        serviceKey = new ServiceKey(serviceType.getArea().getNumber(),
                serviceType.getNumber(), serviceType.getArea().getVersion());
        sf = new ServiceFilter(new Identifier("*"),
                domain, new Identifier("*"), null, new Identifier("*"),
                serviceKey, new UIntegerList());

        try {
            final ProviderSummaryList archiveSyncsCD = localDirectoryService.lookupProvider(sf, null);
            final ArrayList<ArchiveSyncConsumerServiceImpl> archiveSyncs = new ArrayList<>();

            // Cycle through the NMF Apps and sync them!
            for (int i = 0; i < archiveSyncsCD.size(); i++) {
                if (archiveSyncsCD.get(i).getProviderName().getValue().contains(Const.NANOSAT_MO_SUPERVISOR_NAME)) {
                    LOGGER.fine("Skipping Supervisor in Archive Sync");
                    continue;
                }
                final ProviderSummaryList psl = new ProviderSummaryList();
                psl.add(archiveSyncsCD.get(i));

                try {
                    final SingleConnectionDetails connectionDetails = AppsLauncherManager.getSingleConnectionDetailsFromProviderSummaryList(psl);
                    try {
                        final ArchiveSyncConsumerServiceImpl archSync = new ArchiveSyncConsumerServiceImpl(connectionDetails);
                        archiveSyncs.add(archSync);
                    } catch (final MalformedURLException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                } catch (final IOException ex) {
                    // The ArchiveSync service does not exist on this provider...
                    // Do nothing!
                }
            }

            this.syncRemoteArchiveWithLocalArchive(archiveSyncs);
        } catch (final MALInteractionException | MALException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public final void syncRemoteArchiveWithLocalArchive(final ArrayList<ArchiveSyncConsumerServiceImpl> archiveSyncs) throws MALInteractionException, MALException {
        // Select Parameter Definitions by default
        final ObjectTypeList objTypes = new ObjectTypeList();
        final UShort shorty = new UShort((short) 0);
        objTypes.add(new ObjectType(shorty, shorty, new UOctet((short) 0), shorty));

        for (int i = 0; i < archiveSyncs.size(); i++) {
            final ArchiveSyncConsumerServiceImpl archiveSync = archiveSyncs.get(i);

            final GetTimeResponse response = archiveSync.getArchiveSyncStub().getTime();
            FineTime lastSyncTime = response.getBodyElement1();

            if (lastSyncTime.getValue() == 0) {
                lastSyncTime = latestTimestampForProvider(archiveSync);
            }

            final FineTime until = response.getBodyElement0();

            LOGGER.log(
                    Level.INFO,
                    "Synchronizing provider: {0}, From: {1}, Until: {2}",
                    new Object[] {archiveSync.getConnectionDetails().getDomain(), lastSyncTime, until});
            // This value should be obtained from the getCurrent timestamp!
            final ArrayList<COMObjectStructure> comObjects = archiveSync.retrieveCOMObjects(lastSyncTime, until, objTypes);

            for (final COMObjectStructure comObject : comObjects) {
                final ArchiveDetailsList detailsList = new ArchiveDetailsList();
                detailsList.add(comObject.getArchiveDetails());

                try {
                    super.localCOMServices.getArchiveService().store(
                            false,
                            comObject.getObjType(),
                            comObject.getDomain(),
                            detailsList,
                            comObject.getObjects(),
                            null
                    );
                } catch (final MALException ex) {
                    LOGGER.log(
                            Level.SEVERE, null, ex);
                } catch (final MALInteractionException ex) {
                    if (COMHelper.DUPLICATE_ERROR_NUMBER.equals(ex.getStandardError().getErrorNumber())) {
                        LOGGER.log(
                                Level.SEVERE, "The object already exists!");
                    } else {
                        LOGGER.log(
                                Level.SEVERE, "Error!", ex);
                    }
                }
            }
            // Change the Archive URI to be the one of the local COM Archive service
            final IdentifierList providerDomain = archiveSync.getConnectionDetails().getDomain();
            final URI localCOMArchiveURI = super.getCOMArchiveServiceURI();
            //super.localDirectoryService.rerouteArchiveServiceURI(providerDomain, localCOMArchiveURI);
            LOGGER.log(
                    Level.INFO,
                    "Synchronizing provider {0} completed",
                    new Object[] {archiveSync.getConnectionDetails().getDomain()});
        }
    }

    /**
     * Main command line entry point.
     *
     * @param args the command line arguments
     * @throws java.lang.Exception If there is an error
     */
    public static void main(final String[] args) throws Exception {
        final GroundMOProxyOPSSATImpl proxy = new GroundMOProxyOPSSATImpl();
    }

    private FineTime latestTimestampForProvider(final ArchiveSyncConsumerServiceImpl archiveSync) {
        // We have to return the value as the most recent COM Object timestamp!

        // Do a query on the COM Objects for the latest one!
        final FineTime timeInFarFuture = new FineTime(Long.MAX_VALUE);
        final String text = HelperTime.time2readableString(timeInFarFuture);
        Logger.getLogger(GroundMOProxy.class.getName()).log(Level.FINE,
                "The time in the future is: " + text);

        final ArchiveQuery archiveQuery = new ArchiveQuery(
                archiveSync.getConnectionDetails().getDomain(),
                null,
                null,
                0L,
                null,
                null,
                timeInFarFuture,
                true,
                null
        );

        final ArchiveQueryList archiveQueryList = new ArchiveQueryList();
        archiveQueryList.add(archiveQuery);

        final Semaphore semaphore = new Semaphore(0);
        final ArchiveDetails arch = new ArchiveDetails();

        final ObjectType objType = new ObjectType(new UShort(0), new UShort(0), new UOctet((short) 0), new UShort(0));

        try {
            super.localCOMServices.getArchiveService().query(false, objType,
                    archiveQueryList, null, new QueryInteractionImpl(arch, semaphore));
        } catch (final MALException | MALInteractionException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        try {
            semaphore.acquire();
            return arch.getTimestamp();
        } catch (final InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        return new FineTime(0);
    }

    private static class QueryInteractionImpl extends QueryInteraction {

        private final ArchiveDetails arch;
        private final Semaphore semaphore;

        public QueryInteractionImpl(final ArchiveDetails arch, final Semaphore semaphore) {
            super(null);
            this.arch = arch;
            this.semaphore = semaphore;
        }

        @Override
        public MALMessage sendAcknowledgement() throws MALInteractionException, MALException {
            return null;
        }

        @Override
        public MALMessage sendUpdate(final ObjectType objType, final IdentifierList domain,
                                     final ArchiveDetailsList objDetails, final ElementList objBodies)
                throws MALInteractionException, MALException {
            // Should never reach this because we asked for one single object, the latest!
            Logger.getLogger(GroundMOProxy.class.getName()).log(Level.WARNING,
                    "Something is wrong! sendUpdate is not expected to be called!");
            return null;
        }

        @Override
        public MALMessage sendResponse(final ObjectType objType, final IdentifierList domain,
                                       final ArchiveDetailsList objDetails, final ElementList objBodies)
                throws MALInteractionException, MALException {
            if (objDetails != null && !objDetails.isEmpty()) {
                arch.setTimestamp(objDetails.get(0).getTimestamp());
            } else {
                arch.setTimestamp(new FineTime(0));
            }
            semaphore.release();
            return null;
        }

        @Override
        public MALMessage sendError(final MALStandardError error) throws MALInteractionException, MALException {
            Logger.getLogger(GroundMOProxy.class.getName()).log(Level.INFO, "Error! (1)");
            semaphore.release();
            return null;
        }

        @Override
        public MALMessage sendUpdateError(final MALStandardError error) throws MALInteractionException, MALException {
            Logger.getLogger(GroundMOProxy.class.getName()).log(Level.INFO, "Error! (2)");
            semaphore.release();
            return null;
        }

    }
}
