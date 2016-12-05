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
import esa.mo.mal.transport.gen.GENMessageHeader;
import esa.mo.mal.transport.gen.GENTransport;
import static esa.mo.mal.transport.spp.SPPBaseTransport.APID_QUALIFIER_PROPERTY;
import static esa.mo.mal.transport.spp.SPPBaseTransport.IS_TC_PACKET_PROPERTY;
import java.util.HashMap;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.MALOperation;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.SessionType;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.transport.MALEncodedBody;
import org.ccsds.moims.mo.mal.transport.MALMessage;

/**
 *
 */
public class SPPEndpoint extends GENEndpoint
{
  private final SPPConfiguration configuration;
  private final int apidQualifier;
  private final Boolean forceTC;
  private final SPPURIRepresentation uriRep;
  private final SPPSourceSequenceCounter ssCounter;
  private final Map<SegmentIndex, SPPSegmentCounter> segmentCounterMap = new HashMap<SegmentIndex, SPPSegmentCounter>();

  public SPPEndpoint(GENTransport transport,
          SPPConfiguration configuration,
          int apidQualifier,
          SPPURIRepresentation uriRep,
          SPPSourceSequenceCounter ssCounter,
          String localName, String routingName, String uri, boolean wrapBodyParts,
          final Map properties)
  {
    super(transport, localName, routingName, uri, wrapBodyParts);

    this.configuration = new SPPConfiguration(configuration, properties);

    int aq = apidQualifier;
    // decode configuration
    if (properties != null)
    {
//      if (properties.containsKey("org.ccsds.moims.mo.malspp.apid"))
//      {
//        a = Integer.parseInt(properties.get("org.ccsds.moims.mo.malspp.apid").toString());
//      }
      if (properties.containsKey(APID_QUALIFIER_PROPERTY))
      {
        aq = Integer.parseInt(properties.get(APID_QUALIFIER_PROPERTY).toString());
      }

      if (properties.containsKey(IS_TC_PACKET_PROPERTY))
      {
        forceTC = Boolean.parseBoolean(properties.get(IS_TC_PACKET_PROPERTY).toString());
      }
      else
      {
        forceTC = null;
      }
    }
    else
    {
      forceTC = null;
    }

    this.apidQualifier = aq;
    this.uriRep = uriRep;
    this.ssCounter = ssCounter;
  }

  @Override
  public MALMessage createMessage(final Blob authenticationId,
          final URI uriTo,
          final Time timestamp,
          final QoSLevel qosLevel,
          final UInteger priority,
          final IdentifierList domain,
          final Identifier networkZone,
          final SessionType session,
          final Identifier sessionName,
          final InteractionType interactionType,
          final UOctet interactionStage,
          final Long transactionId,
          final UShort serviceArea,
          final UShort service,
          final UShort operation,
          final UOctet serviceVersion,
          final Boolean isErrorMessage,
          final Map qosProperties,
          final Object... body) throws MALException
  {
    try
    {
      SPPMessageHeader hdr = (SPPMessageHeader)createMessageHeader(getURI(),
              authenticationId,
              uriTo,
              timestamp,
              qosLevel,
              priority,
              domain,
              networkZone,
              session,
              sessionName,
              interactionType,
              interactionStage,
              transactionId,
              serviceArea,
              service,
              operation,
              serviceVersion,
              isErrorMessage,
              qosProperties);
      
      return new SPPMessage(((SPPBaseTransport)transport).getHeaderStreamFactory(),
              hdr.getConfiguration(),
              getMessageSegmentCounter(hdr),
              false, hdr,
              qosProperties, null, transport.getStreamFactory(), body);
    }
    catch (MALInteractionException ex)
    {
      throw new MALException("Error creating message", ex);
    }
  }

  @Override
  public MALMessage createMessage(final Blob authenticationId,
          final URI uriTo,
          final Time timestamp,
          final QoSLevel qosLevel,
          final UInteger priority,
          final IdentifierList domain,
          final Identifier networkZone,
          final SessionType session,
          final Identifier sessionName,
          final InteractionType interactionType,
          final UOctet interactionStage,
          final Long transactionId,
          final UShort serviceArea,
          final UShort service,
          final UShort operation,
          final UOctet serviceVersion,
          final Boolean isErrorMessage,
          final Map qosProperties,
          final MALEncodedBody body) throws MALException
  {
    try
    {
      SPPMessageHeader hdr = (SPPMessageHeader)createMessageHeader(getURI(),
              authenticationId,
              uriTo,
              timestamp,
              qosLevel,
              priority,
              domain,
              networkZone,
              session,
              sessionName,
              interactionType,
              interactionStage,
              transactionId,
              serviceArea,
              service,
              operation,
              serviceVersion,
              isErrorMessage,
              qosProperties);
      
      return new SPPMessage(((SPPBaseTransport)transport).getHeaderStreamFactory(),
              hdr.getConfiguration(), getMessageSegmentCounter(hdr), false, hdr,
              qosProperties, null, transport.getStreamFactory(), body);
    }
    catch (MALInteractionException ex)
    {
      throw new MALException("Error creating message", ex);
    }
  }

  @Override
  public MALMessage createMessage(final Blob authenticationId,
          final URI uriTo,
          final Time timestamp,
          final QoSLevel qosLevel,
          final UInteger priority,
          final IdentifierList domain,
          final Identifier networkZone,
          final SessionType session,
          final Identifier sessionName,
          final Long transactionId,
          final Boolean isErrorMessage,
          final MALOperation op,
          final UOctet interactionStage,
          final Map qosProperties,
          final MALEncodedBody body) throws MALException
  {
    try
    {
      SPPMessageHeader hdr = (SPPMessageHeader)createMessageHeader(getURI(),
              authenticationId,
              uriTo,
              timestamp,
              qosLevel,
              priority,
              domain,
              networkZone,
              session,
              sessionName,
              op.getInteractionType(),
              interactionStage,
              transactionId,
              op.getService().getArea().getNumber(),
              op.getService().getNumber(),
              op.getNumber(),
              op.getService().getArea().getVersion(),
              isErrorMessage,
              qosProperties);
      
      return new SPPMessage(((SPPBaseTransport)transport).getHeaderStreamFactory(),
              hdr.getConfiguration(), getMessageSegmentCounter(hdr), false, hdr,
              qosProperties,
              op,
              transport.getStreamFactory(), body);
    }
    catch (MALInteractionException ex)
    {
      throw new MALException("Error creating message", ex);
    }
  }

  @Override
  public MALMessage createMessage(final Blob authenticationId,
          final URI uriTo,
          final Time timestamp,
          final QoSLevel qosLevel,
          final UInteger priority,
          final IdentifierList domain,
          final Identifier networkZone,
          final SessionType session,
          final Identifier sessionName,
          final Long transactionId,
          final Boolean isErrorMessage,
          final MALOperation op,
          final UOctet interactionStage,
          final Map qosProperties,
          final Object... body) throws MALException
  {
    try
    {
      SPPMessageHeader hdr = (SPPMessageHeader)createMessageHeader(getURI(),
              authenticationId,
              uriTo,
              timestamp,
              qosLevel,
              priority,
              domain,
              networkZone,
              session,
              sessionName,
              op.getInteractionType(),
              interactionStage,
              transactionId,
              op.getService().getArea().getNumber(),
              op.getService().getNumber(),
              op.getNumber(),
              op.getService().getArea().getVersion(),
              isErrorMessage,
              qosProperties);
      
      return new SPPMessage(((SPPBaseTransport)transport).getHeaderStreamFactory(),
              hdr.getConfiguration(), getMessageSegmentCounter(hdr), false, hdr,
              qosProperties,
              op,
              transport.getStreamFactory(), body);
    }
    catch (MALInteractionException ex)
    {
      throw new MALException("Error creating message", ex);
    }
  }

  @Override
  public GENMessageHeader createMessageHeader(URI uriFrom,
          Blob authenticationId,
          URI uriTo,
          Time timestamp,
          QoSLevel qosLevel,
          UInteger priority,
          IdentifierList domain,
          Identifier networkZone,
          SessionType session,
          Identifier sessionName,
          InteractionType interactionType,
          UOctet interactionStage,
          Long transactionId,
          UShort serviceArea,
          UShort service,
          UShort operation,
          UOctet serviceVersion,
          Boolean isErrorMessage,
          Map qosProperties)
  {
    return new SPPMessageHeader(((SPPBaseTransport)transport).getHeaderStreamFactory(),
            new SPPConfiguration(configuration, qosProperties),
            forceTC, apidQualifier, uriRep, ssCounter, getURI(),
            authenticationId,
            uriTo,
            timestamp,
            qosLevel,
            priority,
            domain,
            networkZone,
            session,
            sessionName,
            interactionType,
            interactionStage,
            transactionId,
            serviceArea,
            service,
            operation,
            serviceVersion,
            isErrorMessage);
  }

  private SPPSegmentCounter getMessageSegmentCounter(GENMessageHeader hdr)
  {
    SegmentIndex idx = new SegmentIndex(hdr);
    
    SPPSegmentCounter cnt = segmentCounterMap.get(idx);
    
    if (null == cnt)
    {
      cnt = new SPPSegmentCounter();
      segmentCounterMap.put(idx, cnt);
    }
    
    return cnt;
  }

  private static class SegmentIndex
  {
    private final URI uriFrom;
    private final URI uriTo;
    private final IdentifierList domain;
    private final Identifier networkZone;
    private final SessionType session;
    private final Identifier sessionName;
    private final InteractionType interactionType;
    private final Long transactionId;
    private final UShort serviceArea;
    private final UShort service;
    private final UShort operation;

    public SegmentIndex(GENMessageHeader hdr)
    {
      this.uriFrom = hdr.getURIFrom();
      this.uriTo = hdr.getURITo();
      this.domain = hdr.getDomain();
      this.networkZone = hdr.getNetworkZone();
      this.session = hdr.getSession();
      this.sessionName = hdr.getSessionName();
      this.interactionType = hdr.getInteractionType();
      this.transactionId = hdr.getTransactionId();
      this.serviceArea = hdr.getAreaNumber();
      this.service = hdr.getService();
      this.operation = hdr.getOperation();
    }

    @Override
    public int hashCode()
    {
      int hash = 3;
      hash = 47 * hash + (this.uriFrom != null ? this.uriFrom.hashCode() : 0);
      hash = 47 * hash + (this.uriTo != null ? this.uriTo.hashCode() : 0);
      hash = 47 * hash + (this.domain != null ? this.domain.hashCode() : 0);
      hash = 47 * hash + (this.networkZone != null ? this.networkZone.hashCode() : 0);
      hash = 47 * hash + (this.session != null ? this.session.hashCode() : 0);
      hash = 47 * hash + (this.sessionName != null ? this.sessionName.hashCode() : 0);
      hash = 47 * hash + (this.interactionType != null ? this.interactionType.hashCode() : 0);
      hash = 47 * hash + (this.transactionId != null ? this.transactionId.hashCode() : 0);
      hash = 47 * hash + (this.serviceArea != null ? this.serviceArea.hashCode() : 0);
      hash = 47 * hash + (this.service != null ? this.service.hashCode() : 0);
      hash = 47 * hash + (this.operation != null ? this.operation.hashCode() : 0);
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
      final SegmentIndex other = (SegmentIndex) obj;
      if (this.uriFrom != other.uriFrom && (this.uriFrom == null || !this.uriFrom.equals(other.uriFrom)))
      {
        return false;
      }
      if (this.uriTo != other.uriTo && (this.uriTo == null || !this.uriTo.equals(other.uriTo)))
      {
        return false;
      }
      if (this.domain != other.domain && (this.domain == null || !this.domain.equals(other.domain)))
      {
        return false;
      }
      if (this.networkZone != other.networkZone && (this.networkZone == null || !this.networkZone.equals(other.networkZone)))
      {
        return false;
      }
      if (this.session != other.session && (this.session == null || !this.session.equals(other.session)))
      {
        return false;
      }
      if (this.sessionName != other.sessionName && (this.sessionName == null || !this.sessionName.equals(other.sessionName)))
      {
        return false;
      }
      if (this.interactionType != other.interactionType && (this.interactionType == null || !this.interactionType.equals(other.interactionType)))
      {
        return false;
      }
      if (this.transactionId != other.transactionId && (this.transactionId == null || !this.transactionId.equals(other.transactionId)))
      {
        return false;
      }
      if (this.serviceArea != other.serviceArea && (this.serviceArea == null || !this.serviceArea.equals(other.serviceArea)))
      {
        return false;
      }
      if (this.service != other.service && (this.service == null || !this.service.equals(other.service)))
      {
        return false;
      }
      if (this.operation != other.operation && (this.operation == null || !this.operation.equals(other.operation)))
      {
        return false;
      }
      return true;
    }
  }
}
