/**
 * *****************************************************************************
 * Copyright or © or Copr. CNES
 *
 * This software is a computer program whose purpose is to provide a
 * framework for the CCSDS Mission Operations services.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 ******************************************************************************
 */
package org.ccsds.moims.mo.testbed.util.sppimpl.tcp;

import java.util.Map;

import org.ccsds.moims.mo.testbed.util.spp.SPPSocket;
import org.ccsds.moims.mo.testbed.util.spp.SPPSocketFactory;

public class TCPSPPSocketFactory extends SPPSocketFactory {

    public static final String IS_SERVER = "org.ccsds.moims.mo.malspp.test.sppimpl.tcp.isServer";
    public static final String APID_FILTER_PROPERTY = "org.ccsds.moims.mo.malspp.apidFilterPath";
    public static final String APID_FILTER_DEFAULT = "processed_apids.txt";

    @Override
    public SPPSocket createSocket(final Map properties) throws Exception {
        String apidFilterFile = (String) properties.get(APID_FILTER_PROPERTY);
        if (apidFilterFile == null) {
            apidFilterFile = APID_FILTER_DEFAULT;
        }
        final String isServerS = (String) properties.get(IS_SERVER);
        final boolean isServer = Boolean.parseBoolean(isServerS);
        if (isServer) {
            final ServerTCPSPPSocket socket = new ServerTCPSPPSocket(apidFilterFile);
            socket.init(properties);
            return socket;
        } else {
            final ClientTCPSPPSocket socket = new ClientTCPSPPSocket(apidFilterFile);
            socket.init(properties);
            return socket;
        }
    }
}
