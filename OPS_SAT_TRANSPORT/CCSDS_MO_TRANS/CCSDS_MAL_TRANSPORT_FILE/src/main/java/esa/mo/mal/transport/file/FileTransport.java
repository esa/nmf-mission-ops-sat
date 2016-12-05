/* ----------------------------------------------------------------------------
 * Copyright (C) 2014      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO File Transport Framework
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
package esa.mo.mal.transport.file;

import esa.mo.mal.transport.gen.GENEndpoint;
import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.GENTransport;
import esa.mo.mal.transport.gen.receivers.GENIncomingStreamMessageDecoderFactory;
import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.util.GENMessagePoller;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.broker.MALBrokerBinding;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.transport.MALEndpoint;
import org.ccsds.moims.mo.mal.transport.MALTransmitErrorException;
import org.ccsds.moims.mo.mal.transport.MALTransportFactory;

/**
 * An implementation of the transport interface for a file based protocol.
 */
public class FileTransport extends GENTransport
{
  /**
   * Logger
   */
  public static final java.util.logging.Logger RLOGGER = Logger.getLogger("org.ccsds.moims.mo.mal.transport.file");
  public static final String FILE_PREFIX = "CCSDS_FILE_TRANSPORT_";
  private static final String QOS_I_MESSAGE_PROPERTY = "ccsds.mal.transport.file.incoming.directory.property";
  private static final String QOS_I_MESSAGE_DIRECTORY = "ccsds.mal.transport.file.incoming.directory.name";
  private static final String QOS_O_MESSAGE_PROPERTY = "ccsds.mal.transport.file.outgoing.directory.property";
  private static final String QOS_O_MESSAGE_DIRECTORY = "ccsds.mal.transport.file.outgoing.directory.name";
  private static final String QOS_DELETE_FILE = "ccsds.mal.transport.file.qos.delete";
  private final boolean deleteFiles;
  private final Thread asyncPollThread;
  private final String transportString;
  private final String filenameString;
  private final WatchService watcher;
  private final Path incomingDirectory;
  private final Path outgoingDirectory;
  private final FileTransceiver tc;

  /**
   * Constructor.
   *
   * @param protocol The protocol string.
   * @param factory The factory that created us.
   * @param properties The QoS properties.
   * @throws MALException On error.
   */
  public FileTransport(final String protocol,
          final MALTransportFactory factory,
          final java.util.Map properties) throws MALException
  {
    super(protocol, '-', false, false, factory, properties);

    String incomingDirectoryName = System.getProperty("user.dir");
    String outgoingDirectoryName = incomingDirectoryName;
    boolean lDeleteFiles = true;

    if (null != properties)
    {
      if (properties.containsKey(QOS_DELETE_FILE))
      {
        RLOGGER.info("File transport set to NOT delete message files");
        lDeleteFiles = false;
      }

      String lIncomingDirectoryName;

      if (properties.containsKey(QOS_I_MESSAGE_PROPERTY) && null != properties.get(QOS_I_MESSAGE_PROPERTY))
      {
        lIncomingDirectoryName = String.valueOf(properties.get(QOS_I_MESSAGE_PROPERTY));
      }
      else
      {
        lIncomingDirectoryName = String.valueOf(properties.get(QOS_I_MESSAGE_DIRECTORY));
      }

      if (null != lIncomingDirectoryName)
      {
        incomingDirectoryName = lIncomingDirectoryName;
      }

      String lOutgoingDirectoryName;

      if (properties.containsKey(QOS_O_MESSAGE_PROPERTY) && null != properties.get(QOS_O_MESSAGE_PROPERTY))
      {
        lOutgoingDirectoryName = String.valueOf(properties.get(QOS_O_MESSAGE_PROPERTY));
      }
      else
      {
        lOutgoingDirectoryName = String.valueOf(properties.get(QOS_O_MESSAGE_DIRECTORY));
      }

      if (null != lOutgoingDirectoryName)
      {
        outgoingDirectoryName = lOutgoingDirectoryName;
      }
    }

    // set up the directory to watch for incoming and outgoing messages
    incomingDirectory = Paths.get(incomingDirectoryName);
    outgoingDirectory = Paths.get(outgoingDirectoryName);
    deleteFiles = lDeleteFiles;

    try
    {
      watcher = FileSystems.getDefault().newWatchService();
      System.out.println("Watching : " + incomingDirectoryName);

      filenameString = ManagementFactory.getRuntimeMXBean().getName();
      transportString = FILE_PREFIX + filenameString + "-";
      incomingDirectory.register(watcher, java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY);
      tc = new FileTransceiver(incomingDirectory, outgoingDirectory, watcher, transportString, filenameString, deleteFiles);

      asyncPollThread = new GENMessagePoller<InputStream>(this, tc, tc, new GENIncomingStreamMessageDecoderFactory());
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      throw new MALException("Error initialising TCP Server", ex);
    }

    // set up polling thread for new files appearing in the file directory
    RLOGGER.log(Level.INFO, "Monitoring directory {0} for file prefix {1}", new Object[]
    {
      incomingDirectoryName, transportString
    });
    RLOGGER.log(Level.INFO, "Writing to directory {0} for outgoing messages", new Object[]
    {
      outgoingDirectoryName
    });
  }

  @Override
  public void init() throws MALException
  {
    super.init();

    asyncPollThread.start();
  }

  @Override
  protected String createTransportAddress() throws MALException
  {
    return filenameString;
  }

  @Override
  protected GENEndpoint internalCreateEndpoint(String localName, String routingName, Map qosProperties) throws MALException
  {
    return new GENEndpoint(this, localName, routingName, uriBase + localName, false);
  }

  @Override
  public MALBrokerBinding createBroker(final String localName,
          final Blob authenticationId,
          final QoSLevel[] expectedQos,
          final UInteger priorityLevelNumber,
          final Map defaultQoSProperties) throws MALException
  {
    // not support by File transport
    return null;
  }

  @Override
  public MALBrokerBinding createBroker(final MALEndpoint endpoint,
          final Blob authenticationId,
          final QoSLevel[] qosLevels,
          final UInteger priorities,
          final Map properties) throws MALException
  {
    // not support by File transport
    return null;
  }

  @Override
  public boolean isSupportedInteractionType(final InteractionType type)
  {
    // Supports all IPs except Pub Sub
    return (InteractionType.PUBSUB.getOrdinal() != type.getOrdinal());
  }

  @Override
  public boolean isSupportedQoSLevel(final QoSLevel qos)
  {
    // The transport only supports BESTEFFORT in reality but this is only a test transport so we say it supports all
    return QoSLevel.BESTEFFORT.equals(qos);
  }

  @Override
  public void close() throws MALException
  {
    asyncPollThread.interrupt();
  }

  @Override
  protected GENMessageSender createMessageSender(GENMessage msg, String remoteRootURI) throws MALException, MALTransmitErrorException
  {
    return tc;
  }

  @Override
  public GENMessage createMessage(InputStream ios) throws MALException
  {
    return new FileBasedMessage(qosProperties, ios, getStreamFactory());
  }

  @Override
  public GENMessage createMessage(byte[] packet) throws MALException
  {
    return null;
  }
}
