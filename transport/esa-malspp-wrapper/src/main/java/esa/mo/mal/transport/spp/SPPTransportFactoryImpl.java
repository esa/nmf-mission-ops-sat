/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO SPP Transport Framework
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
package esa.mo.mal.transport.spp;

import java.util.Map;
import org.ccsds.moims.mo.mal.MALContext;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.transport.MALTransport;
import org.ccsds.moims.mo.mal.transport.MALTransportFactory;

/**
 * Instance of the transport factory for a TCP/IP transport.
 */
public class SPPTransportFactoryImpl extends MALTransportFactory {
    private static final Object MUTEX = new Object();
    private SPPTransport transport;

    /**
     * Constructor.
     *
     * @param protocol The protocol string.
     */
    public SPPTransportFactoryImpl(final String protocol) {
        super(protocol);
    }

    @Override
    public MALTransport createTransport(final MALContext malContext, final Map properties) throws MALException {
        synchronized (MUTEX) {
            if (null == transport) {
                transport = new SPPTransport(getProtocol(), this, properties);
                transport.init();
            }

            return transport;
        }
    }
}
