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
import esa.mo.helpertools.helpers.HelperMisc;
import esa.mo.helpertools.misc.Const;
import esa.mo.mc.impl.consumer.ActionConsumerServiceImpl;
import esa.mo.mc.impl.proxy.ActionProxyServiceImpl;
import esa.mo.nmf.groundmoproxy.entities.LastArchiveSync;
import esa.mo.nmf.groundmoproxy.helper.LastArchiveSyncHelper;
import esa.mo.sm.impl.provider.AppsLauncherManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.com.COMHelper;
import org.ccsds.moims.mo.com.COMService;
import org.ccsds.moims.mo.com.archive.provider.QueryInteraction;
import org.ccsds.moims.mo.com.archive.structures.ArchiveDetails;
import org.ccsds.moims.mo.com.archive.structures.ArchiveDetailsList;
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
import org.ccsds.moims.mo.mal.structures.UInteger;
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
public class GroundMOProxyOPSSATImpl extends GroundMOProxy
{

    // name of the Java property specifying the period of archives synchronizing (in seconds)
    private static final String ARCHIVE_SYNC_PERIOD = "esa.mo.nmf.groundmoproxy.archiveSyncPeriod";

    private static final Logger LOGGER = Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName());

    private final ProtocolBridgeSPP protocolBridgeSPP = new ProtocolBridgeSPP();

    private final HashMap<IdentifierList, URI> actionURIs = new HashMap<>();

    public int archiveSyncPeriod = 10000;

    private Timer archiveSyncTimer;

    private Future<?> currentSyncTask = null;

    private ExecutorService archiveSyncExecutor = Executors.newCachedThreadPool();

    private LastArchiveSyncHelper lastArchiveSyncHelper;

    /**
     * Ground MO Proxy for OPS-SAT
     */
    public GroundMOProxyOPSSATImpl()
    {
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
        try
        {
            // TCP/IP is the selected transport binding for the bridge with SPP
            protocolBridgeSPP.init(protocol, properties);
            final URI routedURI = protocolBridgeSPP.getRoutingProtocol();

            // Initialize the pure protocol bridge for the services without extension
            final URI centralDirectoryServiceURI = new URI("malspp:247/100/5");
            super.init(centralDirectoryServiceURI, routedURI);

            final URI uri = super.getDirectoryServiceURI();
            LOGGER.log(Level.INFO, "Ground MO Proxy initialized! URI: {0}\n", new Object[] { uri });
        }
        catch (final Exception ex)
        {
            LOGGER.log(Level.SEVERE, "The SPP Protocol Bridge could not be initialized!", ex);
        }

        lastArchiveSyncHelper =
                new LastArchiveSyncHelper(super.localCOMServices.getArchiveService().getArchiveManager().getDbBackend().getEM(),
                                          super.localCOMServices.getArchiveService().getArchiveManager().getDbBackend()
                                          .getEmAvailability());

        archiveSyncPeriod = Integer.parseInt(System.getProperty(ARCHIVE_SYNC_PERIOD, "10")) * 1000;

        archiveSyncTimer = new Timer("GMOArchiveSyncTimer");
        archiveSyncTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if (null == currentSyncTask || currentSyncTask.isDone())
                {
                    currentSyncTask = archiveSyncExecutor.submit(GroundMOProxyOPSSATImpl.this::synchronizeArchives);
                }
            }
        }, 0, archiveSyncPeriod);
    }

    private synchronized void synchronizeArchives()
    {
        final IdentifierList domain = new IdentifierList();
        domain.add(new Identifier("*"));

        COMService serviceType;
        ServiceKey serviceKey;
        ServiceFilter sf;
        // ---------------------
        // Sync the COM Archives
        // ---------------------
        serviceType = ArchiveSyncHelper.ARCHIVESYNC_SERVICE;
        serviceKey = new ServiceKey(serviceType.getArea().getNumber(), serviceType.getNumber(),
                                    serviceType.getArea().getVersion());
        sf = new ServiceFilter(new Identifier("*"), domain, new Identifier("*"), null, new Identifier("*"), serviceKey,
                               new UIntegerList());

        try
        {
            final ProviderSummaryList archiveSyncsCD = localDirectoryService.lookupProvider(sf, null);
            final ArrayList<ArchiveSyncConsumerServiceImpl> archiveSyncs = new ArrayList<>();

            // Cycle through the NMF Apps and sync them!
            for (int i = 0; i < archiveSyncsCD.size(); i++)
            {
                if (archiveSyncsCD.get(i).getProviderName().getValue().contains(Const.NANOSAT_MO_SUPERVISOR_NAME))
                {
                    LOGGER.fine("Skipping Supervisor in Archive Sync");
                    continue;
                }
                final ProviderSummaryList psl = new ProviderSummaryList();
                psl.add(archiveSyncsCD.get(i));

                try
                {
                    final SingleConnectionDetails connectionDetails =
                            AppsLauncherManager.getSingleConnectionDetailsFromProviderSummaryList(psl);
                    try
                    {
                        final ArchiveSyncConsumerServiceImpl archSync =
                                new ArchiveSyncConsumerServiceImpl(connectionDetails);
                        archiveSyncs.add(archSync);
                    }
                    catch (final MalformedURLException ex)
                    {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                }
                catch (final IOException ex)
                {
                    // The ArchiveSync service does not exist on this provider...
                    // Do nothing!
                }
            }

            this.syncRemoteArchiveWithLocalArchive(archiveSyncs);
        }
        catch (final MALInteractionException | MALException ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public final void syncRemoteArchiveWithLocalArchive(final ArrayList<ArchiveSyncConsumerServiceImpl> archiveSyncs)
            throws MALInteractionException, MALException
    {
        // Select Parameter Definitions by default
        final ObjectTypeList objTypes = new ObjectTypeList();
        final UShort shorty = new UShort((short) 0);
        objTypes.add(new ObjectType(shorty, shorty, new UOctet((short) 0), shorty));

        for (int i = 0; i < archiveSyncs.size(); i++)
        {
            final ArchiveSyncConsumerServiceImpl archiveSync = archiveSyncs.get(i);

            String domain = HelperMisc.domain2domainId(archiveSync.getConnectionDetails().getDomain());
            String providerUri = archiveSync.getConnectionDetails().getProviderURI().getValue();

            LastArchiveSync lastArchiveSync = lastArchiveSyncHelper.findLastArchiveSync(domain, providerUri);

            GetTimeResponse lastSyncTime = archiveSync.getArchiveSyncStub().getTime();

            if (null == lastSyncTime)
            {
                LOGGER.log(Level.WARNING,
                           "Archive sync completed for domain: {0} ! Can't get last sync time! NULL was returned -> error or no ArchiveSync service!",
                           new Object[] { domain });
                continue;
            }

            FineTime from = null;
            FineTime until = null;

            if (null == lastArchiveSync)
            {
                until = lastSyncTime.getBodyElement0();
                lastArchiveSync = new LastArchiveSync(providerUri, domain);
            }
            else
            {
                if (lastArchiveSync.getLastSync() <= lastSyncTime.getBodyElement0().getValue())
                {
                    from = new FineTime(lastArchiveSync.getLastSync());
                    until = lastSyncTime.getBodyElement0();
                }
                else
                {
                    LOGGER.log(Level.WARNING,
                               "Archive sync completed for domain: {0}! Sync not performed! Last sync time {1} is greater than provider current time {2}!",
                               new Object[] { domain, lastArchiveSync.getLastSync(),
                                              lastSyncTime.getBodyElement0().getValue() });
                    continue;

                }
            }

            LOGGER.log(Level.INFO, "Synchronizing provider: {0}, From: {1}, Until: {2}",
                       new Object[] { domain, from, until });
            // This value should be obtained from the getCurrent timestamp!
            final ArrayList<COMObjectStructure> comObjects = archiveSync.retrieveCOMObjects(from, until, objTypes);

            lastSyncTime = archiveSync.getArchiveSyncStub().getTime();

            FineTime timestamp = lastSyncTime.getBodyElement1();

            boolean success = true;

            for (final COMObjectStructure comObject : comObjects)
            {
                final ArchiveDetailsList detailsList = new ArchiveDetailsList();
                detailsList.add(comObject.getArchiveDetails());

                try
                {
                    super.localCOMServices.getArchiveService()
                            .store(false, comObject.getObjType(), comObject.getDomain(), detailsList,
                                   comObject.getObjects(), null);
                }
                catch (final MALException ex)
                {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    success = false;
                }
                catch (final MALInteractionException ex)
                {
                    if (COMHelper.DUPLICATE_ERROR_NUMBER.equals(ex.getStandardError().getErrorNumber()))
                    {
                        UIntegerList duplicatesList = (UIntegerList) ex.getStandardError().getExtraInformation();
                        if (!duplicatesList.isEmpty())
                        {
                            try
                            {
                                ArchiveDetailsList tempDetailsList = new ArchiveDetailsList();
                                ElementList tempElementsList = null;
                                for (UInteger duplicate : duplicatesList)
                                {
                                    tempDetailsList.add(detailsList.get((int) duplicate.getValue()));

                                    if (comObject.getObjects() != null)
                                    {
                                        if (tempElementsList == null)
                                        {
                                            tempElementsList = HelperMisc.element2elementList(
                                                    comObject.getObjects().get((int) duplicate.getValue()));
                                        }
                                        tempElementsList.add(comObject.getObjects().get((int) duplicate.getValue()));
                                    }
                                }
                                super.localCOMServices.getArchiveService()
                                        .update(comObject.getObjType(), comObject.getDomain(), tempDetailsList,
                                                tempElementsList, null);

                                for (UInteger duplicate : duplicatesList)
                                {
                                    detailsList.set((int) duplicate.getValue(), null);
                                    if (comObject.getObjects() != null)
                                    {
                                        comObject.getObjects().set((int) duplicate.getValue(), null);
                                    }
                                }
                                detailsList.removeIf(Objects::isNull);
                                if (comObject.getObjects() != null)
                                {
                                    comObject.getObjects().removeIf(Objects::isNull);
                                }

                                super.localCOMServices.getArchiveService()
                                        .store(false, comObject.getObjType(), comObject.getDomain(), detailsList,
                                               comObject.getObjects(), null);
                            }
                            catch (MALException | MALInteractionException ex2)
                            {
                                LOGGER.log(Level.SEVERE, "Error!", ex);
                                success = false;
                            }
                        }
                    }
                    else
                    {
                        LOGGER.log(Level.SEVERE, "Error!", ex);
                        success = false;
                    }
                }
            }
            // Change the Archive URI to be the one of the local COM Archive service
            /*final IdentifierList providerDomain = archiveSync.getConnectionDetails().getDomain();
            final URI localCOMArchiveURI = super.getCOMArchiveServiceURI();
            super.localDirectoryService.rerouteArchiveServiceURI(providerDomain, localCOMArchiveURI);*/

            if (success)
            {

                lastArchiveSync.setLastSync(timestamp.getValue());

                lastArchiveSyncHelper.persistLastArchiveSync(lastArchiveSync);

                LOGGER.log(Level.INFO, "Synchronizing provider {0} completed", new Object[] { domain });
            }
            else
            {
                LOGGER.log(Level.WARNING, "Synchronizing provider {0} completed with some problems!",
                           new Object[] { domain });
            }
        }
    }

    /**
     * Main command line entry point.
     *
     * @param args the command line arguments
     * @throws java.lang.Exception If there is an error
     */
    public static void main(final String[] args) throws Exception
    {
        final GroundMOProxyOPSSATImpl proxy = new GroundMOProxyOPSSATImpl();
    }

    @Override
    public synchronized void additionalHandling()
    {
        final IdentifierList domain = new IdentifierList();
        domain.add(new Identifier("*"));

        // Add the Action service rerouting stuff
        COMService serviceType = ActionHelper.ACTION_SERVICE;
        ServiceKey serviceKey = new ServiceKey(serviceType.getArea().getNumber(), serviceType.getNumber(),
                                               serviceType.getArea().getVersion());
        ServiceFilter sf =
                new ServiceFilter(new Identifier("*"), domain, new Identifier("*"), null, new Identifier("*"),
                                  serviceKey, new UIntegerList());

        try
        {
            final ProviderSummaryList actionsCD = localDirectoryService.lookupProvider(sf, null);

            // Cycle through the NMF Apps and sync them!
            for (int i = 0; i < actionsCD.size(); i++)
            {
                final ProviderSummaryList psl = new ProviderSummaryList();
                psl.add(actionsCD.get(i));
                // Needs some work!

                try
                {
                    final SingleConnectionDetails connectionDetails =
                            AppsLauncherManager.getSingleConnectionDetailsFromProviderSummaryList(psl);
                    try
                    {
                        synchronized (actionURIs)
                        {
                            URI localActionURI = actionURIs.get(connectionDetails.getDomain());

                            if (localActionURI == null)
                            {
                                // This only needs to be done in case it still does not exist:
                                final ActionConsumerServiceImpl actionConsumer =
                                        new ActionConsumerServiceImpl(connectionDetails, null);
                                final ActionProxyServiceImpl actionProxyService = new ActionProxyServiceImpl();
                                actionProxyService.init(localCOMServices, actionConsumer);
                                localActionURI = actionProxyService.getConnectionProvider().getConnectionDetails()
                                        .getProviderURI();
                                actionURIs.put(connectionDetails.getDomain(), localActionURI);
                            }

                            super.localDirectoryService.rerouteActionServiceURI(connectionDetails.getDomain(),
                                                                                localActionURI);
                        }
                    }
                    catch (final MalformedURLException ex)
                    {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                }
                catch (final IOException ex)
                {
                    // The Action service does not exist on this provider...
                    // Do nothing!
                }

            }
        }
        catch (final MALInteractionException | MALException ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private static class QueryInteractionImpl extends QueryInteraction
    {

        private final ArchiveDetails arch;

        private final Semaphore semaphore;

        public QueryInteractionImpl(final ArchiveDetails arch, final Semaphore semaphore)
        {
            super(null);
            this.arch = arch;
            this.semaphore = semaphore;
        }

        @Override
        public MALMessage sendAcknowledgement() throws MALInteractionException, MALException
        {
            return null;
        }

        @Override
        public MALMessage sendUpdate(final ObjectType objType, final IdentifierList domain,
                                     final ArchiveDetailsList objDetails, final ElementList objBodies)
                throws MALInteractionException, MALException
        {
            // Should never reach this because we asked for one single object, the latest!
            Logger.getLogger(GroundMOProxy.class.getName())
                    .log(Level.WARNING, "Something is wrong! sendUpdate is not expected to be called!");
            return null;
        }

        @Override
        public MALMessage sendResponse(final ObjectType objType, final IdentifierList domain,
                                       final ArchiveDetailsList objDetails, final ElementList objBodies)
                throws MALInteractionException, MALException
        {
            if (objDetails != null && !objDetails.isEmpty())
            {
                arch.setTimestamp(objDetails.get(0).getTimestamp());
            }
            else
            {
                arch.setTimestamp(new FineTime(0));
            }
            semaphore.release();
            return null;
        }

        @Override
        public MALMessage sendError(final MALStandardError error) throws MALInteractionException, MALException
        {
            Logger.getLogger(GroundMOProxy.class.getName()).log(Level.INFO, "Error! (1)");
            semaphore.release();
            return null;
        }

        @Override
        public MALMessage sendUpdateError(final MALStandardError error) throws MALInteractionException, MALException
        {
            Logger.getLogger(GroundMOProxy.class.getName()).log(Level.INFO, "Error! (2)");
            semaphore.release();
            return null;
        }

    }
}
