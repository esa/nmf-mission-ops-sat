/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO JMS Transport Framework
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
package esa.mo.mal.transport.jms;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.structures.*;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.receivers.GENIncomingMessageDecoder;
import esa.mo.mal.transport.jms.util.StructureHelper;

/**
 *
 */
public class JMSConsumeHandler extends JMSQueueHandler
{
  private final List<MessageConsumer> consumerList = new LinkedList<MessageConsumer>();
  private final UOctet version;
  private Identifier subId = null;
  private URI URIFrom = null;
  private QoSLevel level = null;
  private UInteger priority = null;
  private Identifier networkZone = null;
  private SessionType session = null;
  private Identifier sessionName = null;
  private Long transactionId = null;

  public JMSConsumeHandler(JMSEndpoint endPoint, Object interruption, Session qs, Topic messageSource, String sourceName, UShort area, UShort service, UShort operation, UOctet version) throws Exception
  {
    super(endPoint, interruption, qs, messageSource, sourceName);

    this.version = version;
  }

  public void register(JMSTransport transport, final String providerExchangeName, final GENMessage msg, final Subscription subscription) throws Exception
  {
    // remove old subscriptions
    deregister(false);

    //this.queueSession = transport.getCurrentConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
    MALMessageHeader hdr = msg.getHeader();
    URIFrom = hdr.getURITo();
    level = hdr.getQoSlevel();
    priority = hdr.getPriority();
    networkZone = hdr.getNetworkZone();
    session = hdr.getSession();
    sessionName = hdr.getSessionName();

    if (null == transactionId)
    {
      transactionId = hdr.getTransactionId();
    }

    // decompose subscription into required subscriptions
    EntityRequestList entities = subscription.getEntities();
    subId = subscription.getSubscriptionId();

    // need to clear all previous bindings
    StringBuilder buf = new StringBuilder();
    boolean notFirst = false;

    String sdomain = StructureHelper.domainToString(msg.getHeader().getDomain());

    for (EntityRequest entitie : entities)
    {
      EntityRequest rqst = (EntityRequest) entitie;
      buf.append('(');
      // insert request specific filters
      StringBuilder pbuf = new StringBuilder();
      boolean pvalueSet;
      IdentifierList sdl = rqst.getSubDomain();
      if ((null != sdl) && (0 < sdl.size()))
      {
        boolean wildcard = false;
        int trunc = 0;
        if (sdl.get(sdl.size() - 1).getValue().equals("*"))
        {
          wildcard = true;
          trunc = 1;
        }

        String subdomain = StructureHelper.domainToString(sdl, trunc);

        if (0 < subdomain.length())
        {
          subdomain = sdomain + "." + subdomain;
        }
        else
        {
          subdomain = sdomain;
        }

        pvalueSet = createRoutingKeyString(pbuf, JMSEndpoint.DOM_PROPERTY, subdomain, wildcard, false);
      }
      else
      {
        pvalueSet = createRoutingKeyString(pbuf, JMSEndpoint.DOM_PROPERTY, sdomain, false, false);
      }
      if (!rqst.getAllAreas())
      {
        pvalueSet = createRoutingKeyLong(pbuf, JMSEndpoint.ARR_PROPERTY, (long)msg.getHeader().getServiceArea().getValue(), pvalueSet);
      }
      if (!rqst.getAllServices())
      {
        pvalueSet = createRoutingKeyLong(pbuf, JMSEndpoint.SVC_PROPERTY, (long)msg.getHeader().getService().getValue(), pvalueSet);
      }
      if (!rqst.getAllOperations())
      {
        pvalueSet = createRoutingKeyLong(pbuf, JMSEndpoint.OPN_PROPERTY, (long)msg.getHeader().getOperation().getValue(), pvalueSet);
      }
      if (rqst.getOnlyOnChange())
      {
        createRoutingKeyBoolean(pbuf, JMSEndpoint.MOD_PROPERTY, true, pvalueSet);
      }
      buf.append(pbuf);
      StringBuilder ebuf = new StringBuilder();
      EntityKeyList entityKeys = rqst.getEntityKeys();
      for (EntityKey entityKey : entityKeys)
      {
        EntityKey id = (EntityKey) entityKey;
        StringBuilder lbuf = new StringBuilder();
        boolean valueSet = createRoutingKeyIdentifier(lbuf, JMSEndpoint.EID_PROPERTY, id.getFirstSubKey());
        valueSet = createRoutingKeyLong(lbuf, JMSEndpoint.DID_PROPERTY, id.getSecondSubKey(), valueSet);
        valueSet = createRoutingKeyLong(lbuf, JMSEndpoint.OID_PROPERTY, id.getThirdSubKey(), valueSet);
        createRoutingKeyLong(lbuf, JMSEndpoint.SID_PROPERTY, id.getFourthSubKey(), valueSet);
        if (lbuf.length() > 0)
        {
          if (notFirst)
          {
            ebuf.append(" OR ");
          }
          else
          {
            notFirst = true;
          }

          ebuf.append('(');
          ebuf.append(lbuf);
          ebuf.append(')');
        }
      }
      if (0 < ebuf.length())
      {
        if (0 < pbuf.length())
        {
          buf.append(" AND (");
        }

        buf.append(ebuf);

        if (0 < pbuf.length())
        {
          buf.append(')');
        }
      }
      buf.append(')');
    }
    JMSTransport.RLOGGER.log(Level.FINE, "JMS Registering to {0} for {1}", new Object[]
    {
      providerExchangeName, buf.toString()
    });

    try
    {
      MessageConsumer consumer = queueSession.createConsumer(messageSource, buf.toString());
      consumer.setMessageListener(this);
      consumerList.add(consumer);
    }
    catch (Exception e)
    {
      JMSTransport.RLOGGER.log(Level.WARNING, "JMS Error occurred when subscribing {0}", e);
    }
  }

  public void deregister(boolean clearTransId) throws MALException
  {
    // remove old subscriptions
    for (MessageConsumer consumer : consumerList)
    {
      try
      {
        consumer.close();
      }
      catch (Exception e)
      {
        JMSTransport.RLOGGER.log(Level.WARNING, "JMS Error occurred when unsubscribing {0}", e);
      }
    }

    consumerList.clear();

    if (clearTransId)
    {
      transactionId = null;
    }
  }

  protected static boolean createRoutingKeyIdentifier(StringBuilder buf, String propertyId, Identifier id)
  {
    if ((null != id) && (null != id.getValue()) && (!"*".equals(id.getValue())))
    {
      buf.append(propertyId);
      buf.append(" = '");
      buf.append(id.getValue());
      buf.append('\'');

      return true;
    }

    return false;
  }

  protected static boolean createRoutingKeyLong(StringBuilder buf, String propertyId, Long i, boolean withPrevious)
  {
    if (null != i)
    {
      if (0 != i)
      {
        if (withPrevious)
        {
          buf.append(" AND ");
        }
        buf.append(propertyId);
        buf.append(" = ");
        buf.append(i);

        withPrevious = true;
      }
    }
    else
    {
      if (withPrevious)
      {
        buf.append(" AND ");
      }
      buf.append(propertyId);
      buf.append(" IS NULL");

      withPrevious = true;
    }

    return withPrevious;
  }

  protected static boolean createRoutingKeyString(StringBuilder buf, String propertyId, String i, boolean substringMatch, boolean withPrevious)
  {
    if (null != i)
    {
      if (!i.contentEquals(""))
      {
        if (withPrevious)
        {
          buf.append(" AND ");
        }
        buf.append(propertyId);
        if (substringMatch)
        {
          buf.append(" LIKE '");
        }
        else
        {
          buf.append(" = '");
        }
        buf.append(i);
        if (substringMatch)
        {
          buf.append("%");
        }
        buf.append('\'');

        withPrevious = true;
      }
    }
    else
    {
      if (withPrevious)
      {
        buf.append(" AND ");
      }
      buf.append(propertyId);
      buf.append(" IS NULL");

      withPrevious = true;
    }

    return withPrevious;
  }

  protected static boolean createRoutingKeyBoolean(StringBuilder buf, String propertyId, boolean b, boolean withPrevious)
  {
    if (withPrevious)
    {
      buf.append(" AND ");
    }
    buf.append(propertyId);
    if (b)
    {
      buf.append(" = TRUE");
    }
    else
    {
      buf.append(" = FALSE");
    }

    return true;
  }

  @Override
  protected GENIncomingMessageDecoder createMessageDecoder(JMSUpdate update)
  {
    return new JMSIncomingPSMessageDecoder(endPoint.getJtransport(), update, endPoint.getURI(), version, subId, URIFrom, level, priority, networkZone, session, sessionName, transactionId);
  }
}
