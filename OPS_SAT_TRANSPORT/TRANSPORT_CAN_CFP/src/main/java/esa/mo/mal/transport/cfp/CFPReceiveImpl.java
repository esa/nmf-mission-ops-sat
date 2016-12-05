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
package esa.mo.mal.transport.cfp;

import esa.mo.mal.transport.gen.receivers.GENIncomingByteMessageDecoderFactory.GENIncomingByteMessageDecoder;
import esa.mo.transport.can.opssat.CANReceiveInterface;

/**
 * The implementation of the CANReceiveInterface interface. Holds a reference to
 * the transport instance that created it.
 */
public class CFPReceiveImpl implements CANReceiveInterface {

    private final transient CFPTransport transport;

    /**
     * Creates a new instance of CANReceiveImpl
     *
     * @param transport The transport instance to pass received messages to.
     */
    public CFPReceiveImpl(final CFPTransport transport) {
        this.transport = transport;
    }

    @Override
    public void receive(final byte[] packet) {
        transport.receive(null, new GENIncomingByteMessageDecoder(transport, packet));
    }

}
