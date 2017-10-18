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
package esa.mo.nmf.groundmoproxy;

import esa.mo.com.impl.consumer.ArchiveSyncConsumerServiceImpl;
import esa.mo.com.impl.util.COMObjectStructure;
import esa.mo.helpertools.connections.SingleConnectionDetails;
import esa.mo.helpertools.helpers.HelperTime;
import esa.mo.sm.impl.provider.AppsLauncherManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.com.COMService;
import org.ccsds.moims.mo.com.archive.structures.ArchiveDetailsList;
import org.ccsds.moims.mo.com.archivesync.ArchiveSyncHelper;
import org.ccsds.moims.mo.com.structures.ObjectType;
import org.ccsds.moims.mo.com.structures.ObjectTypeList;
import org.ccsds.moims.mo.common.directory.structures.ProviderSummaryList;
import org.ccsds.moims.mo.common.directory.structures.ServiceFilter;
import org.ccsds.moims.mo.common.structures.ServiceKey;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.structures.FineTime;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.UIntegerList;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.UShort;

/**
 * The Ground MO Proxy for OPS-SAT
 *
 * @author Cesar Coelho
 */
public class GroundMOProxyOPSSATImpl extends GroundMOProxy {

    private final ProtocolBridgeSPP protocolBridgeSPP = new ProtocolBridgeSPP();
    private final ArrayList<ArchiveSyncConsumerServiceImpl> archiveSyncs = new ArrayList<ArchiveSyncConsumerServiceImpl>();

    /**
     * Ground MO Proxy for OPS-SAT
     *
     */
    public GroundMOProxyOPSSATImpl() {
        super();

        // Initialize the protocol bridge services and expose them using TCP/IP!
        final Map properties = System.getProperties();

        // The range of APIDs below were formally requested 
        // And are uniquely assigned for the Ground MO Proxy of OPS-SAT
        properties.put(ProtocolBridgeSPP.PROPERTY_APID_RANGE_START, "21");
        properties.put(ProtocolBridgeSPP.PROPERTY_APID_RANGE_END, "59");

        // Initialize the SPP Protocol Bridge
        try {
            // TCP/IP is the selected transport binding for the bridge with SPP
            protocolBridgeSPP.init("maltcp", properties);
            final URI routedURI = protocolBridgeSPP.getRoutingProtocol();

            // Initialize the pure protocol bridge for the services without extension
            final URI centralDirectoryServiceURI = new URI("malspp:247/100/5");
            super.init(centralDirectoryServiceURI, routedURI);

            final URI uri = super.getDirectoryServiceURI();
            Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName()).log(Level.INFO,
                    "Ground MO Proxy initialized! URI: " + uri + "\n");
        } catch (Exception ex) {
            Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName()).log(Level.SEVERE,
                    "The SPP Protocol Bridge could not be initialized!", ex);
        }

        IdentifierList domain = new IdentifierList();
        domain.add(new Identifier("*"));
        COMService serviceType = ArchiveSyncHelper.ARCHIVESYNC_SERVICE;
        final ServiceKey serviceKey = new ServiceKey(serviceType.getArea().getNumber(),
                serviceType.getNumber(), serviceType.getArea().getVersion());
        final ServiceFilter sf = new ServiceFilter(
                new Identifier("*"),
                domain, new Identifier("*"), null, new Identifier("*"),
                serviceKey, new UIntegerList());

        try {
            final ProviderSummaryList archiveSyncsCD = this.getLocalDirectoryService().lookupProvider(sf, null);

            // Cycle through the NMF Apps and sync them!
            for (int i = 0; i < archiveSyncsCD.size(); i++) {
                ProviderSummaryList psl = new ProviderSummaryList();
                psl.add(archiveSyncsCD.get(i));

                final SingleConnectionDetails connectionDetails = AppsLauncherManager.getSingleConnectionDetailsFromProviderSummaryList(psl);

                try {
                    ArchiveSyncConsumerServiceImpl archSync = new ArchiveSyncConsumerServiceImpl(connectionDetails);
                    archiveSyncs.add(archSync);
                } catch (MalformedURLException ex) {
                    Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (MALInteractionException ex) {
            Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MALException ex) {
            Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void syncRemoteArchiveWithLocalArchive() {
        FineTime from = new FineTime(0); // Should be the time of the last sync
        FineTime until = HelperTime.getTimestamp();

        // Select Parameter Definitions by default
        ObjectTypeList objTypes = new ObjectTypeList();
        UShort shorty = new UShort((short) 0);
        objTypes.add(new ObjectType(shorty, shorty, new UOctet((short) 0), shorty));

        for (int i = 0; i < archiveSyncs.size(); i++) {
            ArrayList<COMObjectStructure> comObjects = archiveSyncs.get(i).retrieveCOMObjects(from, until, objTypes);
            
            for (COMObjectStructure comObject : comObjects){
                ArchiveDetailsList detailsList = new ArchiveDetailsList();
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
                } catch (MALException ex) {
                    Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
                } catch (MALInteractionException ex) {
                    Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    /**
     * Main command line entry point.
     *
     * @param args the command line arguments
     * @throws java.lang.Exception If there is an error
     */
    public static void main(final String args[]) throws Exception {
        GroundMOProxyOPSSATImpl proxy = new GroundMOProxyOPSSATImpl();
    }

}
