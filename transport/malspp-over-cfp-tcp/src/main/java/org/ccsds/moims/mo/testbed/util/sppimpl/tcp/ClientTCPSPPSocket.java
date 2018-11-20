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

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.testbed.util.spp.SPPSocket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;

public class ClientTCPSPPSocket implements SPPSocket
{

  private static final Logger LOGGER
      = java.util.logging.Logger.getLogger(ClientTCPSPPSocket.class.getName());
  public static final String HOSTNAME = "org.ccsds.moims.mo.malspp.test.sppimpl.tcp.hostname";
  public static final String PORT = "org.ccsds.moims.mo.malspp.test.sppimpl.tcp.port";

  private String host;
  private int port;
  private SPPChannel channel;

  private final HashMap<Integer, Integer> lastSPPsMap = new HashMap<>();

  public ClientTCPSPPSocket()
  {
    super();
  }

  public void init(Map properties) throws Exception
  {
    LOGGER.log(Level.FINE, "ClientTCPSPPSocket.init({0})", properties);
    host = (String) properties.get(HOSTNAME);
    String portS = (String) properties.get(PORT);
    port = Integer.parseInt(portS);
    connect(host, port);
  }

  public void connect(String host, int port) throws Exception
  {
    LOGGER.log(Level.FINE, "ClientTCPSPPSocket.connect({0},{1})", new Object[]{host, port});
    Socket socket = new Socket(host, port);
    channel = new SPPChannel(socket);
  }

  @Override
  public void close() throws Exception
  {
    channel.close();
  }

  @Override
  public SpacePacket receive() throws Exception
  {
    try {
      SpacePacket packet = channel.receive();

      int packetAPID = packet.getHeader().getApid();
      final int sequenceCount = packet.getHeader().getSequenceCount();
      final int previous = (lastSPPsMap.get(packetAPID) != null) ? lastSPPsMap.get(packetAPID) : -1;

      if (previous != sequenceCount - 1
          && previous != 16383
          && sequenceCount != 0) { // Exclude also the transition zone
        LOGGER.log(Level.WARNING,
            "Out-of-order detected! Sequence count: {0} - Last: {1} (For APID:{2})", new Object[]{
              sequenceCount,
              previous, packetAPID});
      }

      lastSPPsMap.put(packetAPID, sequenceCount);

      LOGGER.log(Level.FINE, "Received: {0}", packet);
      return packet;
    } catch (IOException ex) {
      LOGGER.log(Level.WARNING, "Interrupted socket receive - restarting the channel...");
      channel.close();
      connect(host, port);
      throw ex;
    }
  }

  @Override
  public void send(SpacePacket packet) throws Exception
  {
    LOGGER.log(Level.FINE, "send({0})", packet);

    if (channel != null) {
      channel.send(packet);
    } else {
      throw new IOException("SPP send called, but no connection established!");
    }
  }

  @Override
  public String getDescription()
  {
    return host + '-' + port;
  }
}
