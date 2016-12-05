/* ----------------------------------------------------------------------------
 * Copyright (C) 2014      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO TCP/IP Transport Framework
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
package esa.mo.mal.transport.tcpip;

import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.GENTransport;
import static esa.mo.mal.transport.gen.GENTransport.LOGGER;
import esa.mo.mal.transport.gen.receivers.GENIncomingByteMessageDecoderFactory;
import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.util.GENMessagePoller;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.broker.MALBrokerBinding;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.transport.MALEndpoint;
import org.ccsds.moims.mo.mal.transport.MALTransmitErrorException;
import org.ccsds.moims.mo.mal.transport.MALTransportFactory;

/**
 * The TCPIP MAL Transport implementation.
 *
 * The following properties configure the transport:
 *
 * org.ccsds.moims.mo.mal.transport.tcpip.wrap org.ccsds.moims.mo.mal.transport.tcpip.debug == debug mode , affects
 * logging org.ccsds.moims.mo.mal.transport.tcpip.numconnections == number of connections to a different MAL (either
 * server / client) org.ccsds.moims.mo.mal.transport.tcpip.inputprocessors == number of threads processing in parallel
 * raw MAL messages org.ccsds.moims.mo.mal.transport.tcpip.host == adapter (host / IP Address) that the transport will
 * use for incoming connections. In case of a pure client (i.e. not offering any services) this property should be
 * omitted. org.ccsds.moims.mo.mal.transport.tcpip.port == port that the transport listens to. In case this is a pure
 * client, this property should be omitted.
 *
 * The general logic is the following : The transport at first initialises the server listen port (if this is a server,
 * offering services).
 *
 * On receiving a request to send a MAL Message the transport tries to find if it has allocated some recourses
 * associated with the target URI (has already the means to exchange data with it) and if not, it creates
 * -numconnections- connections to the target server. If a client has already opened a connection to a server the server
 * will re-use that communication channel to send back data to the client.
 *
 * On the server, each incoming connection is handled separately by a different thread which on the first message
 * reception associates the remote URI with the connection (socket). This has the consequence that if the server wants
 * to either use a service, or reply to the remote URI, it will use on of these already allocated communication
 * resources.
 *
 * In the case of malformed MAL messages or communication errors, all resources related to the remote URI are released
 * and need to be reestablished.
 *
 * URIs:
 *
 * The TCPIP Transport, generates URIs, in the for of : {@code tcpip://<host>:<port or client ID>-<service id>}
 * There are two categories of URIs Client URIs, which are in the form of {@code tcpip://<host>:<clientId>-<serviceId>} , where
 * the client id is a unique identifier for the client on its host, for example : 4783fbc147ab7aa56e7fff and ServerURIs,
 * which are in the form of {@code tcpip://<host>:<port>-<serviceId>} and clients can actively connect to.
 *
 * If a MAL instance does not offer any services then all of its endpoints get a Client URI. If a MAL instance offers at
 * least one service then all of its endpoints get a Server URI. A service provider communicates with a service consumer
 * with the communication channel that the service consumer initiated (uses bidirectional TCP/IP communication).
 *
 */
public class TCPIPTransport extends GENTransport
{
  /**
   * Logger
   */
  public static final java.util.logging.Logger RLOGGER = Logger.getLogger("org.ccsds.moims.mo.mal.transport.tcpip");

  /**
   * Port delimiter
   */
  private static final char PORT_DELIMITER = ':';

  /**
   * The server port that the TCP transport listens for incoming connections
   */
  private final int serverPort;

  /**
   * Server host, this can be one of the IP Addresses / hostnames of the host.
   */
  private final String serverHost;

  /**
   * Holds the server connection listener
   */
  private TCPIPServerConnectionListener serverConnectionListener = null;

  /**
   * Holds the list of data poller threads
   */
  private final List<GENMessagePoller> pollerThreads = new ArrayList<GENMessagePoller>();

  /*
   * Constructor.
   *
   * @param protocol The protocol string.
   * @param serviceDelim The delimiter to use for separating the URL
   * @param supportsRouting True if routing is supported by the naming convention
   * @param wrapBodyParts True is body parts should be wrapped in BLOBs
   * @param factory The factory that created us.
   * @param properties The QoS properties.
   * @throws MALException On error.
   */
  public TCPIPTransport(final String protocol, final char serviceDelim, final boolean supportsRouting, final MALTransportFactory factory, final java.util.Map properties) throws MALException
  {
    super(protocol, serviceDelim, supportsRouting, false, factory, properties);

    // decode configuration
    if (properties != null)
    {
      // host / ip adress
      if (properties.containsKey("org.ccsds.moims.mo.mal.transport.tcpip.host"))
      {
        this.serverHost = (String) properties.get("org.ccsds.moims.mo.mal.transport.tcpip.host");
      }
      else
      {
        this.serverHost = null; // this is only a client
      }

      // port
      if (properties.containsKey("org.ccsds.moims.mo.mal.transport.tcpip.port"))
      {
        this.serverPort = Integer.parseInt((String) properties.get("org.ccsds.moims.mo.mal.transport.tcpip.port"));
      }
      else
      {
        if (serverHost != null)
        {
          //this is a server, use default port
          this.serverPort = 61616;
        }
        else
        {
          //this is a client
          this.serverPort = 0; //0 means this is a client
        }
      }
    }
    else
    {
      // default values
      this.serverPort = 0; //0 means this is a client
      this.serverHost = null; //null means this is a client
    }

    RLOGGER.log(Level.INFO, "TCPIP Wrapping body parts set to  : {0}", this.wrapBodyParts);
  }

  @Override
  public void init() throws MALException
  {
    super.init();

    if (serverHost != null)
    {
      // this is also a server (i.e. provides some services)
      RLOGGER.log(Level.INFO, "Starting TCP Server Transport on port {0}", serverPort);

      // start server socket on predefined port / interface
      try
      {
        InetAddress serverHostAddr = InetAddress.getByName(serverHost);
        ServerSocket serverSocket = new ServerSocket(serverPort, 0, serverHostAddr);

        // create thread that will listen for connections
        synchronized (this)
        {
          serverConnectionListener = new TCPIPServerConnectionListener(this, serverSocket);
          serverConnectionListener.start();
        }

        RLOGGER.log(Level.INFO, "Started TCP Server Transport on port {0}", serverPort);
      }
      catch (Exception ex)
      {
        throw new MALException("Error initialising TCP Server", ex);
      }
    }

  }

  @Override
  public MALBrokerBinding createBroker(final String localName, final Blob authenticationId, final QoSLevel[] expectedQos, final UInteger priorityLevelNumber, final Map defaultQoSProperties) throws MALException
  {
    // not support by TCPIP transport
    return null;
  }

  @Override
  public MALBrokerBinding createBroker(final MALEndpoint endpoint, final Blob authenticationId, final QoSLevel[] qosLevels, final UInteger priorities, final Map properties) throws MALException
  {
    // not support by TCPIP transport
    return null;
  }

  @Override
  public boolean isSupportedInteractionType(final InteractionType type)
  {
    // Supports all IPs except Pub Sub
    return InteractionType.PUBSUB.getOrdinal() != type.getOrdinal();
  }

  @Override
  public boolean isSupportedQoSLevel(final QoSLevel qos)
  {
    // The transport only supports BESTEFFORT in reality but this is only a
    // test transport so we say it supports all
    return true;
  }

  @Override
  public void close() throws MALException
  {
    synchronized (this)
    {
      for (GENMessagePoller entry : pollerThreads)
      {
        entry.close();
      }

      pollerThreads.clear();
    }

    super.close();

    synchronized (this)
    {
      if (null != serverConnectionListener)
      {
        serverConnectionListener.interrupt();
      }
    }
  }

  @Override
  protected String createTransportAddress() throws MALException
  {
    if (serverHost == null)
    {
      //this is a pure client
      //in this case we get the IP Address of the host and provide a unique id as the port.
      //the actual IP and port information does not matter as the server will not try
      //to connect to it, it is used as an identifier for the MAL in the URI.
      return getDefaultHost() + PORT_DELIMITER + getRandomClientId();
    }
    else
    {
      //this a server (and potentially a client)
      return serverHost + PORT_DELIMITER + serverPort;
    }
  }

  @Override
  protected GENMessageSender createMessageSender(GENMessage msg, String remoteRootURI) throws MALException, MALTransmitErrorException
  {
    try
    {
      // decode target address
      String targetAddress = remoteRootURI.replaceAll(protocol + protocolDelim, "");
      targetAddress = targetAddress.replaceAll(protocol, ""); // in case the protocol is in the format tcpip://

      if (!targetAddress.contains(":"))
      {
        // malformed URI
        throw new MALException("Malformed URI:" + remoteRootURI);
      }

      String host = targetAddress.split(":")[0];
      int port = Integer.parseInt(targetAddress.split(":")[1]);

      //create a message sender and receiver for the socket
      TCPIPTransportDataTransceiver trans = createDataTransceiver(new Socket(host, port));

      // create also a data reader thread for this socket in order to read messages from it 
      // no need to register this as it will automatically terminate when the uunderlying connection is terminated.
      GENMessagePoller rcvr = new GENMessagePoller<byte[]>(this, trans, trans, new GENIncomingByteMessageDecoderFactory());
      rcvr.setRemoteURI(remoteRootURI);
      rcvr.start();

      pollerThreads.add(rcvr);

      return trans;
    }
    catch (NumberFormatException nfe)
    {
      LOGGER.log(Level.WARNING, "Have no means to communicate with client URI : {0}", remoteRootURI);
      throw new MALException("Have no means to communicate with client URI : " + remoteRootURI);
    }
    catch (UnknownHostException e)
    {
      LOGGER.log(Level.WARNING, "TCPIP could not find host :{0}", remoteRootURI);
      LOGGER.log(Level.FINE, "TCPIP could not find host :" + remoteRootURI, e);
      throw new MALTransmitErrorException(msg.getHeader(), new MALStandardError(MALHelper.DESTINATION_UNKNOWN_ERROR_NUMBER, null), null);
    }
    catch (java.net.ConnectException e)
    {
      LOGGER.log(Level.WARNING, "TCPIP could not connect to :{0}", remoteRootURI);
      LOGGER.log(Level.FINE, "TCPIP could not connect to :" + remoteRootURI, e);
      throw new MALTransmitErrorException(msg.getHeader(), new MALStandardError(MALHelper.DESTINATION_TRANSIENT_ERROR_NUMBER, null), null);
    }
    catch (IOException e)
    {
      //there was a communication problem, we need to clean up the objects we created in the meanwhile
      communicationError(remoteRootURI, null);

      //rethrow for higher MAL leyers
      throw new MALException("IO Exception", e);
    }
  }

  /**
   * Allows transport derived from this, where the message encoding is changed for example, to easily replace the
   * message transceiver without worrying about the TCPIP connection
   *
   * @param socket the TCPIP socket
   * @return the new transceiver
   * @throws IOException if there is an error
   */
  protected TCPIPTransportDataTransceiver createDataTransceiver(Socket socket) throws IOException
  {
    return new TCPIPTransportDataTransceiver(socket);
  }

  /**
   * Provide a default IP address for this host
   *
   * @return The transport specific address part.
   * @throws MALException On error
   */
  private String getDefaultHost() throws MALException
  {
    try
    {
      // Build RMI url string
      final InetAddress addr = Inet4Address.getLocalHost();
      final StringBuilder hostAddress = new StringBuilder();
      if (addr instanceof Inet6Address)
      {
        RLOGGER.fine("TCPIP Address class is IPv6");
        hostAddress.append('[');
        hostAddress.append(addr.getHostAddress());
        hostAddress.append(']');
      }
      else
      {
        hostAddress.append(addr.getHostAddress());
      }

      return hostAddress.toString();
    }
    catch (UnknownHostException ex)
    {
      throw new MALException("Could not determine local host address", ex);
    }
  }

  /**
   * This method returns a random Id to be used for differentiating different MAL instances in the same host.
   *
   * @return the random, host unique, id
   */
  private String getRandomClientId()
  {
    return new UID().toString().replaceAll("[^abcdef0-9]", "");
  }
}
