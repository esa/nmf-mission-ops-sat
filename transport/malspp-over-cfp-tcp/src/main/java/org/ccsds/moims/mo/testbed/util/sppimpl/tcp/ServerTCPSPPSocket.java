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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.ccsds.moims.mo.testbed.util.spp.SPPSocket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;

import fr.dyade.aaa.common.Daemon;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.objectweb.util.monolog.api.Logger;

public class ServerTCPSPPSocket implements SPPSocket
{

  private static final java.util.logging.Logger LOGGER
      = java.util.logging.Logger.getLogger(ServerTCPSPPSocket.class.getName());

  private static final int MAX_ERROR_COUNT = 10;

  public final static String PORT_PROP = "org.ccsds.moims.mo.malspp.test.sppimpl.tcp.port";
  private int port;
  private ServerSocket listenerSocket;
  private boolean tcpNoDelay;
  private final List<SPPChannel> channels = new ArrayList<>();
  private AcceptorDaemon readerDaemon;

  private final LinkedBlockingQueue<SpacePacket> input = new LinkedBlockingQueue<>();

  public ServerTCPSPPSocket()
  {
    super();
  }

  public void init(Map properties) throws Exception
  {
    String portS = (String) properties.get(PORT_PROP);
    port = Integer.parseInt(portS);
    listen(port);
  }

  private void listen(int port) throws Exception
  {
    LOGGER.log(Level.FINE, "listen({0})", new Object[]{port});
    listenerSocket = new ServerSocket(port);
    readerDaemon = new AcceptorDaemon();
    readerDaemon.start();
  }

  @Override
  public void close() throws Exception
  {
    closeClientChannels();
    if (readerDaemon != null) {
      readerDaemon.stop();
    }
  }

  private void closeClientChannels()
  {
    synchronized (channels) {
      for (SPPChannel channel : channels) {
        if (channel != null) {
          channel.close();
        }
      }
    }
  }

  @Override
  public SpacePacket receive() throws Exception
  {
    SpacePacket packet = input.take();
    LOGGER.log(Level.FINE, "Received: {0}", packet);
    return packet;
  }

  @Override
  public void send(SpacePacket packet) throws IOException
  {
    LOGGER.log(Level.FINE, "send({0})", packet);
    synchronized (channels) {
      if (channels.isEmpty()) {
        throw new IOException("SPP send called, but no connection established!");
      }
      for (SPPChannel channel : channels) {
        if (channel != null) {
          channel.send(packet);
        }
      }
    }

  }

  @Override
  public String getDescription()
  {
    return "-" + port;

  }

  class AcceptorDaemon extends Daemon
  {

    private Socket clientSocket;

    private SpacePacket packet;

    protected AcceptorDaemon()
    {
      super("AcceptorDaemon", null);
    }

    @Override
    public final void run()
    {
      int errorCount = 0;
      while (running) {
        if (errorCount >= MAX_ERROR_COUNT) {
          LOGGER.log(Level.SEVERE, "errorCount >= {0}. Stopping the server.", MAX_ERROR_COUNT);
          break;
        }
        try {
          LOGGER.log(Level.INFO, "Listening for a client connection on {0}",
              listenerSocket.getLocalSocketAddress());
          clientSocket = listenerSocket.accept();
        } catch (IOException ex) {
          LOGGER.log(Level.SEVERE, "Error when accepting the client connection", ex);
          errorCount++;
          continue;
        }
        SPPChannel newChannel = null;
        try {
          clientSocket.setTcpNoDelay(tcpNoDelay);
          clientSocket.setSoLinger(true, 1000);
          newChannel = new SPPChannel(clientSocket);
        } catch (IOException ex) {
          LOGGER.log(Level.SEVERE, "Error when configuring the client connection", ex);
          if (newChannel != null) {
            newChannel.close();
            newChannel = null;
          }
          errorCount++;
          continue;
        }
        errorCount = 0;
        LOGGER.log(Level.INFO, "Accepted connection from: {0}",
            clientSocket.getRemoteSocketAddress());
        synchronized (channels) {
          channels.add(newChannel);
        }
        new ClientThread(newChannel).start();
      }
      shutdown();
      finish();
    }

    private class ClientThread extends Thread
    {

      protected SPPChannel channel;

      public ClientThread(SPPChannel channel)
      {
        this.channel = channel;
      }

      public void run()
      {
        try {
          while (running) {
            canStop = true;
            packet = channel.receive();
            input.offer(packet);
          }
        } catch (IOException ex) {
          LOGGER.log(Level.WARNING,
              this.getName() + ", error during packet receive. Closing the client connection.", ex);

        } finally {
          if (channel != null) {
            synchronized (channels) {
              channels.remove(channel);
            }
            channel.close();
            channel = null;
          }
        }
      }
    }

    @Override
    protected void close()
    {
      if (listenerSocket != null) {
        try {
          listenerSocket.close();
        } catch (IOException e) {
        }
      }
    }

    @Override
    protected void shutdown()
    {
      close();
    }
  }

}
