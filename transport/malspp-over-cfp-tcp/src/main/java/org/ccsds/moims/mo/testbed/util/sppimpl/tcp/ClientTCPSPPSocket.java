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
  public static final int DEFAULT_RETRY_TIME = 5000;
  public static final String RETRYTIME = "org.ccsds.moims.mo.malspp.test.sppimpl.tcp.retrytime";
  public static final String HOSTNAME = "org.ccsds.moims.mo.malspp.test.sppimpl.tcp.hostname";
  public static final String PORT = "org.ccsds.moims.mo.malspp.test.sppimpl.tcp.port";

  private String host;
  private int port;
  private int retryTime;
  private SPPChannel channel;
  private boolean exiting;

  private final HashMap<Integer, Integer> lastSPPsMap = new HashMap<>();

  public ClientTCPSPPSocket()
  {
    super();
  }

  public void init(final Map properties) throws Exception
  {
    LOGGER.log(Level.FINE, "ClientTCPSPPSocket.init({0})", properties);
    host = (String) properties.get(HOSTNAME);
    final String portS = (String) properties.get(PORT);
    port = Integer.parseInt(portS);
    final String retryTimeS = (String) properties.get(RETRYTIME);
    if (retryTimeS != null) {
      retryTime = Integer.parseInt(retryTimeS);
    } else {
      retryTime = DEFAULT_RETRY_TIME;
    }
  }

  public void connect(final String host, final int port) throws IOException
  {
    LOGGER.log(Level.FINE, "ClientTCPSPPSocket.connect({0},{1})", new Object[]{host, port});
    final Socket socket = new Socket(host, port);
    channel = new SPPChannel(socket);
  }

  @Override
  public void close() throws Exception
  {
    this.exiting = true;
    channel.close();
  }

  @Override
  public SpacePacket receive() throws Exception
  {
    while (true) {
      if (channel != null) {
        try {
          final SpacePacket packet = channel.receive();

          if(packet == null){ // return null if packet is not NMF relevant
            return packet;
          }

          final int packetAPID = packet.getHeader().getApid();
          final int sequenceCount = packet.getHeader().getSequenceCount();
          final int previous = (lastSPPsMap.get(packetAPID) != null) ? lastSPPsMap.get(packetAPID)
              : -1;

          if (previous != -1 && previous != sequenceCount - 1
              && previous != 16383
              && sequenceCount != 0) { // Exclude also the transition zone
            LOGGER.log(Level.FINE,
                "Out-of-order detected! Sequence count: {0} - Last: {1} (For APID:{2})",
                new Object[]{
                  sequenceCount,
                  previous, packetAPID});
          }

          lastSPPsMap.put(packetAPID, sequenceCount);

          LOGGER.log(Level.FINE, "Received: {0}", packet);
          return packet;
        } catch (final IOException ex) {
          if (exiting) {
            return null;
          }
          LOGGER.log(Level.WARNING, "Failed socket receive - restarting the channel...", ex);
          channel.close();
          try {
            connect(host, port);
          } catch (final IOException ex2) {
            LOGGER.log(Level.WARNING, "Couldn't connect - sleeping for " + retryTime + " ms", ex2);
            Thread.sleep(DEFAULT_RETRY_TIME);
          }
        }
      } else {
        try {
          connect(host, port);
        } catch (final IOException ex2) {
          LOGGER.log(Level.WARNING, "Couldn't connect - sleeping for " + retryTime + " ms", ex2);
          Thread.sleep(DEFAULT_RETRY_TIME);
        }
      }
    }
  }

  @Override
  public void send(final SpacePacket packet) throws Exception
  {
    LOGGER.log(Level.FINE, "send({0})", packet);

    while (true) {
      if (channel != null) {
        try {
          channel.send(packet);
          return;
        } catch (final IOException ex) {
          LOGGER.log(Level.WARNING, "Failed socket send - restarting the channel...", ex);
          channel.close();
          try {
            connect(host, port);
          } catch (final IOException ex2) {
            LOGGER.log(Level.WARNING, "Couldn't connect - sleeping for " + retryTime + " ms", ex2);
            Thread.sleep(DEFAULT_RETRY_TIME);
          }
        }
      } else {
        try {
          connect(host, port);
        } catch (final IOException ex) {
          LOGGER.log(Level.WARNING, "Couldn't connect - sleeping for " + retryTime + " ms", ex);
          Thread.sleep(DEFAULT_RETRY_TIME);
        }
      }
    }
  }

  @Override
  public String getDescription()
  {
    return host + '-' + port;
  }
}
