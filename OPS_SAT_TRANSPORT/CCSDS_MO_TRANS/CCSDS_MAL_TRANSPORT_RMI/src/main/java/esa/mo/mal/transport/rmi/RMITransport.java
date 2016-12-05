/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO RMI Transport
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
package esa.mo.mal.transport.rmi;

import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.GENMessageHeader;
import esa.mo.mal.transport.gen.GENTransport;
import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.sending.GENOutgoingMessageHolder;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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
 * An implementation of the transport interface for the RMI protocol.
 */
public class RMITransport extends GENTransport<byte[], byte[]>
{
  /**
   * Logger
   */
  public static final java.util.logging.Logger RLOGGER = Logger.getLogger("org.ccsds.moims.mo.mal.transport.rmi");
  /**
   * System property to set the host name used locally.
   */
  public static final String RMI_HOSTNAME_PROPERTY = "org.ccsds.moims.mo.mal.transport.rmi.host";
  private static final char RMI_PORT_DELIM = ':';
  private final String serverHost;
  private Registry registry;
  private int portNumber;
  private UnicastRemoteObject ourRMIinterface;

  /**
   * Constructor.
   *
   * @param protocol The protocol string.
   * @param factory The factory that created us.
   * @param properties The QoS properties.
   * @throws MALException On error.
   */
  public RMITransport(final String protocol,
          final MALTransportFactory factory,
          final java.util.Map properties) throws MALException
  {
    super(protocol, '-', true, true, factory, properties);

    String lhost = null;

    if ((properties != null) && (properties.containsKey(RMI_HOSTNAME_PROPERTY)))
    {
      lhost = (String) properties.get(RMI_HOSTNAME_PROPERTY);
    }

    this.serverHost = lhost;
  }

  @Override
  public void init() throws MALException
  {
    // Port numbers above 1023 are up for grabs on any machine....
    int iRmiPort = 1024;
    while (true)
    {
      try
      {
        registry = java.rmi.registry.LocateRegistry.createRegistry(iRmiPort);
        // Got a valid port number, lets get out of here...
        break;
      }
      catch (RemoteException e)
      {
        // Port already in use, lets try the next one...
        ++iRmiPort;
      }
    }

    portNumber = iRmiPort;
    RLOGGER.log(Level.INFO, "RMI Creating registory on port {0}", portNumber);

    super.init();

    try
    {
      ourRMIinterface = new RMIReceiveImpl(this);
      registry.rebind(String.valueOf(portNumber), ourRMIinterface);
      RLOGGER.log(Level.INFO, "RMI Bound to registory on port {0}", portNumber);
    }
    catch (RemoteException ex)
    {
      throw new MALException("Error initialising RMI connection", ex);
    }
  }

  @Override
  protected String createTransportAddress() throws MALException
  {
    final StringBuilder transportAddress = new StringBuilder();

    transportAddress.append(getHostName(serverHost));
    transportAddress.append(RMI_PORT_DELIM);
    transportAddress.append(portNumber);
    transportAddress.append('/');
    transportAddress.append(portNumber);

    return transportAddress.toString();
  }

  @Override
  public MALBrokerBinding createBroker(final String localName,
          final Blob authenticationId,
          final QoSLevel[] expectedQos,
          final UInteger priorityLevelNumber,
          final Map defaultQoSProperties) throws MALException
  {
    // not support by RMI transport
    return null;
  }

  @Override
  public MALBrokerBinding createBroker(final MALEndpoint endpoint,
          final Blob authenticationId,
          final QoSLevel[] qosLevels,
          final UInteger priorities,
          final Map properties) throws MALException
  {
    // not support by RMI transport
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
    // The transport only supports BESTEFFORT in reality but this is only a test transport so we say it supports all
    return true;
  }

  @Override
  public void close() throws MALException
  {
    super.close();

    try
    {
      registry.unbind(String.valueOf(portNumber));
      UnicastRemoteObject.unexportObject(ourRMIinterface, true);
      UnicastRemoteObject.unexportObject(registry, true);
    }
    catch (java.rmi.NotBoundException ex)
    {
      // NoOp
    }
    catch (RemoteException ex)
    {
      // NoOp
    }

    registry = null;
    ourRMIinterface = null;
  }

  @Override
  protected GENMessageSender<byte[]> createMessageSender(GENMessage msg, String remoteRootURI) throws MALException, MALTransmitErrorException
  {
    RLOGGER.log(Level.INFO, "RMI received request to create connections to URI:{0}", remoteRootURI);

    try
    {
      // create new sender for this URI
      return new RMIMessageSender(remoteRootURI);
    }
    catch (NotBoundException e)
    {
      RLOGGER.log(Level.WARNING, "RMI could not connect to :" + remoteRootURI, e);
      throw new MALTransmitErrorException(msg.getHeader(), new MALStandardError(MALHelper.DESTINATION_UNKNOWN_ERROR_NUMBER, null), null);

    }
    catch (IOException e)
    {
      RLOGGER.log(Level.WARNING, "RMI could not connect to :" + remoteRootURI, e);
      throw new MALTransmitErrorException(msg.getHeader(), new MALStandardError(MALHelper.DELIVERY_FAILED_ERROR_NUMBER, null), null);
    }
  }

  @Override
  public GENMessage createMessage(byte[] packet) throws MALException
  {
    return new GENMessage(wrapBodyParts, true, new GENMessageHeader(), qosProperties, packet, getStreamFactory());
  }

  @Override
  protected GENOutgoingMessageHolder<byte[]> internalEncodeMessage(String destinationRootURI,
          String destinationURI,
          Object multiSendHandle,
          boolean lastForHandle,
          String targetURI,
          GENMessage msg) throws Exception
  {
    return new GENOutgoingMessageHolder<byte[]>(10,
            destinationRootURI,
            destinationURI,
            multiSendHandle,
            lastForHandle,
            msg,
            internalEncodeByteMessage(destinationRootURI, destinationURI, multiSendHandle, lastForHandle, targetURI, msg));
  }

  /**
   * Provide an IP address for this host
   *
   * @param preferredHostname The preferred host name, may be NULL in which case the name will be calculated.
   * @return The transport specific address part.
   * @throws MALException On error
   */
  private static String getHostName(String preferredHostname) throws MALException
  {
    if (null == preferredHostname)
    {
      try
      {
        // Build RMI url string
        final InetAddress addr = Inet4Address.getLocalHost();
        final StringBuilder hostAddress = new StringBuilder();
        if (addr instanceof Inet6Address)
        {
          RLOGGER.fine("RMI Address class is IPv6");
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

    return preferredHostname;
  }
}
