/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO SPP Transport Framework
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
package esa.mo.mal.transport.spp;

import esa.mo.mal.transport.gen.GENEndpoint;
import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.GENMessageHeader;
import esa.mo.mal.transport.gen.GENTransport;
import esa.mo.mal.transport.gen.sending.GENOutgoingMessageHolder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.broker.MALBrokerBinding;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.transport.MALEndpoint;
import org.ccsds.moims.mo.mal.transport.MALTransportFactory;

public abstract class SPPBaseTransport<I> extends GENTransport<I, List<ByteBuffer>>
{
  /**
   * Logger
   */
  public static final java.util.logging.Logger LOGGER = Logger.getLogger("org.ccsds.moims.mo.mal.transport.spp");

  public static final String ENCODE_BODY_FIXED = "org.ccsds.moims.mo.malspp.isFixedBody";
  public static final String IS_TC_PACKET_PROPERTY = "org.ccsds.moims.mo.malspp.isTcPacket";
  public static final String SEGMENT_MAX_SIZE_PROPERTY = "org.ccsds.moims.mo.malspp.segmentMaxSize";
  public static final String APID_QUALIFIER_PROPERTY = "org.ccsds.moims.mo.malspp.apidQualifier";
  public static final String APID_PROPERTY = "org.ccsds.moims.mo.malspp.apid";
  public static final String APPEND_ID_TO_URI = "org.ccsds.moims.mo.malspp.appendIdToUri";
  public static final String AUTHENTICATION_ID_FLAG = "org.ccsds.moims.mo.malspp.authenticationIdFlag";
  public static final String DOMAIN_FLAG = "org.ccsds.moims.mo.malspp.domainFlag";
  public static final String NETWORK_ZONE_FLAG = "org.ccsds.moims.mo.malspp.networkZoneFlag";
  public static final String PRIORITY_FLAG = "org.ccsds.moims.mo.malspp.priorityFlag";
  public static final String SESSION_NAME_FLAG = "org.ccsds.moims.mo.malspp.sessionNameFlag";
  public static final String TIMESTAMP_FLAG = "org.ccsds.moims.mo.malspp.timestampFlag";

  protected final SPPConfiguration defaultConfiguration;
  protected final SPPURIRepresentation uriRep;
  protected final SPPSourceSequenceCounterSimple ssc;
  protected final int defaultApidQualifier;
  protected final int defaultApid;
  protected final Map<QualifiedApid, SPPConfiguration> apidConfigurations = new HashMap<QualifiedApid, SPPConfiguration>();
  protected final Map<QualifiedApid, Map<Long, SPPSegmentsHandler>> segmentHandlers = new HashMap<QualifiedApid, Map<Long, SPPSegmentsHandler>>();
          
  /**
   * The stream factory used for encoding and decoding message headers.
   */
  private final MALElementStreamFactory hdrStreamFactory;
  private final AtomicInteger uniqueIdGenerator = new AtomicInteger(0);

  /*
   * Constructor.
   *
   * @param protocol The protocol string.
   * @param factory The factory that created us.
   * @param properties The QoS properties.
   * @throws MALException On error.
   */
  public SPPBaseTransport(SPPConfiguration configuration, SPPURIRepresentation uriRep, SPPSourceSequenceCounterSimple ssc, String protocol, String protocolDelim, char serviceDelim, char routingDelim, boolean supportsRouting, boolean wrapBodyParts, MALTransportFactory factory, Map properties) throws MALException
  {
    super(protocol, protocolDelim, serviceDelim, routingDelim, supportsRouting, wrapBodyParts, factory, properties);

    this.defaultConfiguration = configuration;
    this.uriRep = uriRep;
    this.ssc = ssc;

    int aq = -1;
    int a = 1;

    // decode configuration
    if (properties != null)
    {
      if (properties.containsKey(APID_QUALIFIER_PROPERTY))
      {
        aq = Integer.parseInt((String) properties.get(APID_QUALIFIER_PROPERTY));
      }

      if (properties.containsKey(APID_PROPERTY))
      {
        a = Integer.parseInt((String) properties.get(APID_PROPERTY));
      }
    }

    this.defaultApidQualifier = aq;
    this.defaultApid = a;

    MALElementStreamFactory lsf = super.getStreamFactory();

    try
    {
      lsf = MALElementStreamFactory.newFactory("malspp_header", properties);
    }
    catch (MALException ex)
    {
      // body and header should be the same encoder then
      LOGGER.info("No separate stream encoder configured for SPP header");
    }

    hdrStreamFactory = lsf;
  }

  @Override
  public MALBrokerBinding createBroker(final String localName, final Blob authenticationId, final QoSLevel[] expectedQos, final UInteger priorityLevelNumber, final Map defaultQoSProperties) throws MALException
  {
    // not support by SPP transport
    return null;
  }

  @Override
  public MALBrokerBinding createBroker(final MALEndpoint endpoint, final Blob authenticationId, final QoSLevel[] qosLevels, final UInteger priorities, final Map properties) throws MALException
  {
    // not support by SPP transport
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
  protected String getLocalName(String localName, final java.util.Map properties)
  {
    StringBuilder buf = new StringBuilder();

    int a = defaultApid;
    int aq = defaultApidQualifier;

    // decode configuration
    if (properties != null)
    {
      if (properties.containsKey(APID_PROPERTY))
      {
        a = Integer.parseInt(properties.get(APID_PROPERTY).toString());
      }
      if (properties.containsKey(APID_QUALIFIER_PROPERTY))
      {
        aq = Integer.parseInt(properties.get(APID_QUALIFIER_PROPERTY).toString());
      }
    }

    buf.append(aq);
    buf.append('/');
    buf.append(a);

    if ((properties == null)
            || !properties.containsKey(APPEND_ID_TO_URI)
            || Boolean.parseBoolean(properties.get(APPEND_ID_TO_URI).toString()))
    {
      buf.append('/');
      buf.append(Math.abs((byte) getNextSubId(aq, a)));
    }

    return buf.toString();
  }

  private int getNextSubId(long qualifier, int apid)
  {
    return (byte) uniqueIdGenerator.getAndIncrement();
  }

  @Override
  public String getRoutingPart(String uriValue)
  {
    String endpointUriPart = uriValue;
    int iFirst = endpointUriPart.indexOf(protocolDelim) + 1;

    return endpointUriPart.substring(iFirst);
  }

  @Override
  protected GENEndpoint internalCreateEndpoint(final String localName, final String routingName, final Map properties) throws MALException
  {
    return new SPPEndpoint(this, defaultConfiguration, defaultApidQualifier, uriRep, ssc, localName, routingName, uriBase + routingName, wrapBodyParts, properties);
  }

  protected GENOutgoingMessageHolder<List<ByteBuffer>> internalEncodeMessage(final String destinationRootURI,
          final String destinationURI,
          final Object multiSendHandle,
          final boolean lastForHandle,
          final String targetURI,
          final GENMessage msg) throws Exception
  {
    byte[] buf = internalEncodeByteMessage(destinationRootURI, destinationURI, multiSendHandle, lastForHandle, targetURI, msg);

    int sequenceFlags = (buf[2] & 0xC0) >> 6;

    List<ByteBuffer> encodedMessage = new ArrayList<ByteBuffer>();

    if (3 == sequenceFlags)
    {
      encodedMessage.add(ByteBuffer.wrap(buf));
    }
    else
    {
      ByteBuffer buffer = ByteBuffer.wrap(buf);
      int index = 0;
      while ((buf.length - index) > 0)
      {
        short shortVal = buffer.getShort(index + 4);
        int bodyLength = shortVal >= 0 ? shortVal : 0x10000 + shortVal;
        bodyLength += 7;

        encodedMessage.add(ByteBuffer.wrap(buf, index, bodyLength));
        index += bodyLength;
      }
    }

    return new GENOutgoingMessageHolder<List<ByteBuffer>>(defaultApid,
            destinationRootURI,
            destinationURI,
            multiSendHandle,
            lastForHandle,
            msg,
            encodedMessage);
  }

  protected GENMessage internalCreateMessage(final int apidQualifier, final int apid, int sequenceFlags, final byte[] packet) throws MALException
  {
    if (3 == sequenceFlags)
    {
      SPPConfiguration configuration = apidConfigurations.get(new QualifiedApid(apidQualifier, apid));
      if (null == configuration)
      {
        configuration = defaultConfiguration;
      }

      MALElementStreamFactory localBodyStreamFactory = hdrStreamFactory;
      if (!configuration.isFixedBody())
      {
        localBodyStreamFactory = getStreamFactory();
      }

      // need to decode in two stages, first message header
      SPPMessage dummyMessage = internalDecodeMessageHeader(apidQualifier, apid, packet);

      // now full message including body
      return new SPPMessage(hdrStreamFactory, configuration, null, wrapBodyParts, false,
              (GENMessageHeader) dummyMessage.getHeader(), qosProperties,
              dummyMessage.getBody().getEncodedBody().getEncodedBody().getValue(), localBodyStreamFactory);
    }
    else
    {
      // find packet segment handler
      final Long transactionId = (long) java.nio.ByteBuffer.wrap(packet).getLong(18);

      Map<Long, SPPSegmentsHandler> map = segmentHandlers.get(new QualifiedApid(apidQualifier, apid));
      
      if (null == map)
      {
        map = new HashMap<Long, SPPSegmentsHandler>();
        segmentHandlers.put(new QualifiedApid(apidQualifier, apid), map);
      }

      SPPSegmentsHandler segmentHandler = map.get(transactionId);

      if (null == segmentHandler)
      {
        segmentHandler = new SPPSegmentsHandler(this, apidQualifier, apid);
        map.put(transactionId, segmentHandler);
      }

      segmentHandler.addSegment(sequenceFlags, packet);
      byte[] sppRaw = segmentHandler.getNextMessage();
      
      if (sppRaw != null)
      {
        if(map.get(transactionId).isEmpty()){
            map.remove(transactionId);
        }
        
        // We don't remove the map from the segmentHandlers because we are not expecting
        // to have a big number of different consumers connected. Removing it and
        // re-adding it from/to the map every time we have a new message would
        // make things go slower. The map won't grow like crazy, no worries...
        
        GENMessage msg = internalCreateMessage(apidQualifier, apid, 3, sppRaw);
        LOGGER.log(Level.FINE, "Decoded SPP segmented message: {0}", msg.getHeader());
        return msg;
      }

      return null;
    }
  }

  protected SPPMessage internalDecodeMessageHeader(final int apidQualifier, final int apid, final byte[] packet) throws MALException
  {
    SPPConfiguration configuration = apidConfigurations.get(new QualifiedApid(apidQualifier, apid));
    if (null == configuration)
    {
      configuration = defaultConfiguration;
    }

    // need to decode in two stages, first message header
    return new SPPMessage(hdrStreamFactory, configuration, null, wrapBodyParts, true,
            new SPPMessageHeader(hdrStreamFactory, configuration, null, apidQualifier, uriRep, ssc),
            qosProperties, packet, hdrStreamFactory);
  }

  protected MALElementStreamFactory getHeaderStreamFactory()
  {
    return hdrStreamFactory;
  }

  public static class QualifiedApid
  {
    public final int apidQualifier;
    public final int apid;

    public QualifiedApid(int apidQualifier, int apid)
    {
      this.apidQualifier = apidQualifier;
      this.apid = apid;
    }

    @Override
    public int hashCode()
    {
      int hash = 3;
      hash = 29 * hash + this.apidQualifier;
      hash = 29 * hash + this.apid;
      return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }
      if (obj == null)
      {
        return false;
      }
      if (getClass() != obj.getClass())
      {
        return false;
      }
      final QualifiedApid other = (QualifiedApid) obj;
      if (this.apidQualifier != other.apidQualifier)
      {
        return false;
      }
      return this.apid == other.apid;
    }
  }
}