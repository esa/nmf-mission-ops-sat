/*******************************************************************************
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
 *******************************************************************************/
package org.ccsds.moims.mo.testbed.util.sppimpl.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.ccsds.moims.mo.testbed.util.spp.SPPSocket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;
import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;

import fr.dyade.aaa.common.Daemon;

public class ServerTCPSPPSocket implements SPPSocket {
  
  public final static Logger logger = fr.dyade.aaa.common.Debug
      .getLogger(ServerTCPSPPSocket.class.getName());
  
  public final static String PORT_PROP = "org.ccsds.moims.mo.malspp.test.sppimpl.tcp.port";
  
  private int port;
  
  private ServerSocket listen;

  private boolean tcpNoDelay;
  
  private SPPChannel channel;
  
  private ReaderDaemon readerDaemon;
  
  private LinkedBlockingQueue<SpacePacket> input;
  
  public ServerTCPSPPSocket() {
	  super();
	  input = new LinkedBlockingQueue<SpacePacket>();
  }

	public void init(Map properties) throws Exception {
    String portS = (String) properties.get(PORT_PROP);
    port = Integer.parseInt(portS);
    listen(port);
  }
  
  public void listen(int port) throws Exception {
    listen = new ServerSocket(port);
    readerDaemon = new ReaderDaemon();
    readerDaemon.start();
  }

  public void close() throws Exception {
    channel.close();
    readerDaemon.stop();
  }

  public SpacePacket receive() throws Exception {
    SpacePacket packet = input.take();
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "Received: " + packet);
    return packet;
  }

  public void send(SpacePacket packet) throws Exception {
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "ServerTCPSPPSocket.send(" + packet + ')');
    channel.send(packet);
  }
  
  public String getDescription() {
    return "-" + port;
  }
  
  class ReaderDaemon extends Daemon {
    
    private Socket socket; 
    
    private SpacePacket packet;

    protected ReaderDaemon() {
      super("ReaderDaemon", logger);
    }

    public final void run() {
      try {
        if (logger.isLoggable(BasicLevel.DEBUG))
          logger.log(BasicLevel.DEBUG, "Listen...");
        socket = listen.accept();
        socket.setTcpNoDelay(tcpNoDelay);
        socket.setSoLinger(true, 1000);
        channel = new SPPChannel(socket); 
        
        if (logger.isLoggable(BasicLevel.DEBUG))
          logger.log(BasicLevel.DEBUG,
              "Accepted connection from: " + socket.getRemoteSocketAddress());

        while (running) {
          canStop = true;
          packet = channel.receive();
          input.offer(packet);
        }
      } catch (Exception exc) {
        logger.log(BasicLevel.DEBUG, this.getName()
            + ", error during packet receive", exc);
      } finally {
        finish();
      }
    }
    
    protected void close() {
      if (listen != null) {
        try {
          listen.close();
        } catch (IOException e) {}
      }
    }

    protected void shutdown() {
      close();
    }
  }
  
}
