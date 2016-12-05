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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import javax.jms.*;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.structures.*;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import org.ccsds.moims.mo.mal.transport.MALTransmitErrorException;
import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.body.GENPublishBody;
import org.ccsds.moims.mo.mal.MALInteractionException;
import esa.mo.mal.transport.jms.JMSEndpoint.PublishEntry;
import esa.mo.mal.transport.jms.util.StructureHelper;

/**
 *
 */
public class JMSPublishHandler
{
  private final JMSTransport jtransport;
  private final Set<JMSPublisherKey> keySet = new TreeSet<JMSPublisherKey>();
  private final QoSLevel registerQoS;
  private IdentifierList domain = null;

  public JMSPublishHandler(JMSTransport jtransport, final GENMessage msg)
  {
    this.jtransport = jtransport;
    this.registerQoS = msg.getHeader().getQoSlevel();
  }

  void setKeyList(MALMessageHeader hdr, EntityKeyList l)
  {
    domain = hdr.getDomain();
    keySet.clear();
    for (EntityKey l1 : l)
    {
      keySet.add(new JMSPublisherKey(l1));
    }
  }

  protected GENMessage publish(final GENMessage msg, Session lqs) throws MALException, MALTransmitErrorException, MALInteractionException
  {
    final String strURL = msg.getHeader().getURITo().getValue();
    final int iSecond = strURL.indexOf(JMSTransport.JMS_SERVICE_DELIM);
    final String providerExchangeName = strURL.substring(iSecond + 1);
    
    // decompose update list into separate updates
    MALMessageHeader hdr = msg.getHeader();
    GENPublishBody body = (GENPublishBody) msg.getBody();
    UpdateHeaderList headerList = body.getUpdateHeaderList();

    try
    {
      preCheckAllowedToPublish(hdr, headerList);
    }
    catch (MALTransmitErrorException ex)
    {
      // create response and do callback
      return new GENMessage(false, JMSEndpoint.createReturnHeader(msg, true, MALPubSubOperation._PUBLISH_STAGE), null, null, ex.getStandardError().getErrorNumber(), ex.getStandardError().getExtraInformation());
    }

    List[] valueLists = body.getUpdateLists((List[]) null);
    java.util.Vector<PublishEntry> publishList = new Vector<PublishEntry>(headerList.size());

    try
    {
      for (int i = 0; i < headerList.size(); ++i)
      {
        UpdateHeader uhdr = headerList.get(i);
        publishList.add(new PublishEntry(uhdr.getKey(), UpdateType.UPDATE != uhdr.getUpdateType(), JMSEndpoint.createExchangeMessage(i, headerList, valueLists, jtransport.getStreamFactory())));
      }
    }
    catch (MALException ex)
    {
      ex.printStackTrace();
      throw new MALTransmitErrorException(msg.getHeader(), new MALStandardError(MALHelper.BAD_ENCODING_ERROR_NUMBER, new Union(ex.getLocalizedMessage())), msg.getQoSProperties());
    }

    String exchangeName = providerExchangeName + ":" + msg.getHeader().getSession().toString() + ":" + msg.getHeader().getSessionName();
    String ldomain = StructureHelper.domainToString(msg.getHeader().getDomain());
    String lnetwork = msg.getHeader().getNetworkZone().getValue();
    int area = msg.getHeader().getServiceArea().getValue();
    int service = msg.getHeader().getService().getValue();
    int operation = msg.getHeader().getOperation().getValue();

    try
    {
      // get the queue
      Topic destTopic = jtransport.getAdministrator().getTopic(lqs, exchangeName);

      MessageProducer sender = lqs.createProducer(destTopic);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

      for (PublishEntry publishEntry : publishList)
      {
        try
        {
          ObjectMessage objMsg = lqs.createObjectMessage();
          objMsg.setStringProperty(JMSEndpoint.DOM_PROPERTY, ldomain);
          objMsg.setStringProperty(JMSEndpoint.NET_PROPERTY, lnetwork);
          objMsg.setIntProperty(JMSEndpoint.ARR_PROPERTY, area);
          objMsg.setIntProperty(JMSEndpoint.SVC_PROPERTY, service);
          objMsg.setIntProperty(JMSEndpoint.OPN_PROPERTY, operation);
          objMsg.setStringProperty(JMSEndpoint.EID_PROPERTY, publishEntry.eKey.getFirstSubKey().getValue());
          objMsg.setObjectProperty(JMSEndpoint.DID_PROPERTY, publishEntry.eKey.getSecondSubKey());
          objMsg.setObjectProperty(JMSEndpoint.OID_PROPERTY, publishEntry.eKey.getThirdSubKey());
          objMsg.setObjectProperty(JMSEndpoint.SID_PROPERTY, publishEntry.eKey.getFourthSubKey());
          objMsg.setBooleanProperty(JMSEndpoint.MOD_PROPERTY, publishEntry.isModification);
          objMsg.setObject(publishEntry.update);
          sender.send(objMsg);

          JMSTransport.RLOGGER.log(Level.FINE, "JMS Sending data to {0} with {1} and ({2}, {3}, {4}, {5})", new Object[]{destTopic.getTopicName(), publishEntry.eKey, ldomain, area, service, operation});
        }
        catch (Exception e)
        {
          JMSTransport.RLOGGER.log(Level.WARNING, "JMS Error occurred when sending data {0}", e);
        }
      }

      sender.close();
    }
    catch (Throwable e)
    {
      JMSTransport.RLOGGER.log(Level.WARNING, "JMS Error occurred when publishing data to " + exchangeName + " : {0}", e);
    }

    return null;
  }
  
  public void deregister(GENMessage returnMsg)
  {
    returnMsg.getHeader().setQoSlevel(registerQoS);
  }

  protected void preCheckAllowedToPublish(MALMessageHeader hdr, UpdateHeaderList updateList) throws MALTransmitErrorException
  {
    if (StructureHelper.isSubDomainOf(domain, hdr.getDomain()))
    {
      EntityKeyList lst = new EntityKeyList();
      for (UpdateHeader updateList1 : updateList)
      {
        UpdateHeader update = (UpdateHeader) updateList1;
        EntityKey updateKey = update.getKey();
        boolean matched = false;
        for (JMSPublisherKey key : keySet)
        {
          if (key.matches(updateKey))
          {
            JMSTransport.RLOGGER.log(Level.FINE, "JMS : Provider allowed to publish key: {0} because of: {1}", new Object[]{updateKey, key});
            matched = true;
            break;
          }
        }
        if (!matched)
        {
          lst.add(updateKey);
        }
      }
      if (0 < lst.size())
      {
        JMSTransport.RLOGGER.warning("ERR : Provider not allowed to publish some keys");
        throw new MALTransmitErrorException(hdr, new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, lst), null);
      }
    }
    else
    {
      JMSTransport.RLOGGER.warning("ERR : Provider not allowed to publish to the domain");
      throw new MALTransmitErrorException(hdr, new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, null), null);
    }
  }
}
