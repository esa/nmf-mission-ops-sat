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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.structures.URI;

/**
 * The Ground MO Proxy for OPS-SAT
 *
 * @author Cesar Coelho
 */
public class GroundMOProxyOPSSATImpl extends GroundMOProxy {

    private final ProtocolBridgeSPP protocolBridge = new ProtocolBridgeSPP();

    /**
     * Ground MO Proxy for OPS-SAT
     *
     */
    public GroundMOProxyOPSSATImpl() {
        super();

        try {
            // Initialize the protocol bridge services and expose them using TCP/IP!
            Map properties = System.getProperties();

            // Initialize the Protocol Bridge
            protocolBridge.init("rmi", properties);
            final URI routedURI = protocolBridge.getRoutingProtocol();

            // Initialize the pure protocol bridge for the services without extension
            final URI centralDirectoryServiceURI = new URI("malspp:247/100/5");
            super.init(centralDirectoryServiceURI, routedURI);

            final URI uri = super.getDirectoryServiceURI();
            Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName()).log(Level.INFO,
                    "Groud MO Proxy initialized! URI: " + uri + "\n");
        } catch (Exception ex) {
            Logger.getLogger(GroundMOProxyOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
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
