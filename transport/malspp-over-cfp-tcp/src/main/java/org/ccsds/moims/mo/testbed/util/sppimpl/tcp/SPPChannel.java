/** *****************************************************************************
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
 ****************************************************************************** */
package org.ccsds.moims.mo.testbed.util.sppimpl.tcp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;
import org.ccsds.moims.mo.testbed.util.sppimpl.util.SPPReader;
import org.ccsds.moims.mo.testbed.util.sppimpl.util.SPPWriter;

public class SPPChannel {

    private final Socket socket;

    private final InputStream is;

    private final OutputStream os;

    private final SPPReader reader;

    private final SPPWriter writer;

    public SPPChannel(final Socket socket) throws IOException {
        this.socket = socket;
        is = new BufferedInputStream(socket.getInputStream());
        os = new BufferedOutputStream(socket.getOutputStream());
        reader = new SPPReader(is);
        writer = new SPPWriter(os);
    }

    public SpacePacket receive() throws IOException {
        return reader.receive();
    }

    public void send(final SpacePacket packet) throws IOException {
        writer.send(packet);
    }

    public void close() {
        try {
            if (is != null) {
                is.close();
            }
        } catch (final IOException exc) {
        }

        try {
            if (os != null) {
                os.close();
            }
        } catch (final IOException exc) {
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (final IOException exc) {
        }
    }
}
