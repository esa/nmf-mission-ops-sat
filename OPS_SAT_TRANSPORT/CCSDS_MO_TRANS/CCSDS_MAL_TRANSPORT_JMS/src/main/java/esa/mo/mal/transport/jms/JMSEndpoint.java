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

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.NameNotFoundException;
import org.ccsds.moims.mo.mal.*;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.structures.*;
import org.ccsds.moims.mo.mal.transport.*;
import esa.mo.mal.transport.gen.GENEndpoint;
import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.GENMessageHeader;

/**
 *
 */
public class JMSEndpoint extends GENEndpoint implements MALEndpoint
{
  public static final String DOM_PROPERTY = "DOM";
  public static final String NET_PROPERTY = "NET";
  public static final String ARR_PROPERTY = "ARR";
  public static final String SVC_PROPERTY = "SVC";
  public static final String OPN_PROPERTY = "OPN";
  public static final String EID_PROPERTY = "EID";
  public static final String DID_PROPERTY = "DID";
  public static final String OID_PROPERTY = "OID";
  public static final String SID_PROPERTY = "SID";
  public static final String MOD_PROPERTY = "MOD";
  private final JMSTransport jtransport;
  private final String queueName;
  private final Queue messageSink;
  private final Session qs;
  private final JMSQueueHandler rspnHandler;
  private final Map<String, JMSConsumeHandler> consumeHandlerMap = new TreeMap<String, JMSConsumeHandler>();
  private final Map<String, JMSPublishHandler> publishHandlerMap = new TreeMap<String, JMSPublishHandler>();
  final Object interruption = new Object();

  /**
   * Constructor.
   *
   * @param transport Parent transport.
   * @param localName Endpoint local MAL name.
   * @param routingName Endpoint local routing name.
   * @param baseuri The URI string for this end point.
   */
  public JMSEndpoint(final JMSTransport transport, final String localName, final String routingName, String baseuri, Session qs, Queue q) throws Exception
  {
    super(transport, localName, routingName, baseuri + q.getQueueName(), false);

    this.jtransport = transport;
    this.qs = qs;
    this.messageSink = q;
    this.queueName = messageSink.getQueueName();

    try
    {
      jtransport.getAdministrator().bindQueue(qs, q);
    }
    catch (NameNotFoundException e)
    {
      JMSTransport.RLOGGER.log(Level.WARNING, "JMS: unable to register queue name in JNDI: {0}", this.queueName);
      throw e;
    }

    rspnHandler = new JMSQueueHandler(this, interruption, qs, q, queueName);

    JMSTransport.RLOGGER.log(Level.INFO, "Creating endpoint: {0}", queueName);
  }

  public JMSTransport getJtransport()
  {
    return jtransport;
  }

  @Override
  public void close() throws MALException
  {
    JMSTransport.RLOGGER.log(Level.INFO, "Closing endpoint: {0}", this.queueName);
    
    try
    {
      for (JMSConsumeHandler handler : consumeHandlerMap.values())
      {
        handler.deregister(true);
      }

      consumeHandlerMap.clear();
      
      jtransport.getAdministrator().deleteQueue(qs, messageSink);
      qs.close();
    }
    catch (Exception e)
    {
      JMSTransport.RLOGGER.log(Level.WARNING, "JMS: issues closing JMS connection: " + this.queueName + " : {0}", e);
    }
  }

  @Override
  protected Object internalCreateMultiSendHandle(MALMessage[] msgList) throws Exception
  {
    return jtransport.getCurrentConnection().createSession(true, Session.AUTO_ACKNOWLEDGE);
  }

  @Override
  protected void internalCloseMultiSendHandle(Object handle, MALMessage[] msgList) throws Exception
  {
    Session lqs = (Session) handle;

    if (lqs.getTransacted())
    {
      JMSTransport.RLOGGER.fine("Commiting transaction");
      lqs.commit();
    }

    lqs.close();
  }

  @Override
  protected void internalSendMessage(Object handle, boolean lastForHandle, GENMessage msg) throws MALTransmitErrorException
  {
    try
    {
      Session lqs = (Session) handle;
      boolean localSession = false;

      if (msg.getHeader().getInteractionType() == InteractionType.PUBSUB)
      {
        switch (msg.getHeader().getInteractionStage().getValue())
        {
          case MALPubSubOperation._REGISTER_STAGE:
          {
            if (null == lqs)
            {
              lqs = jtransport.getCurrentConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
              localSession = true;
            }

            internalHandleRegister(msg, lqs);
            break;
          }
          case MALPubSubOperation._PUBLISH_REGISTER_STAGE:
          {
            if (null == lqs)
            {
              lqs = jtransport.getCurrentConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
              localSession = true;
            }

            internalHandlePublishRegister(msg, lqs);
            break;
          }
          case MALPubSubOperation._PUBLISH_STAGE:
          {
            if (null == lqs)
            {
              lqs = jtransport.getCurrentConnection().createSession(true, Session.AUTO_ACKNOWLEDGE);
              localSession = true;
            }

            internalHandlePublish(msg, lqs);

            if (localSession)
            {
              if (lqs.getTransacted())
              {
                JMSTransport.RLOGGER.fine("Commiting transaction");
                lqs.commit();
              }
            }
            break;
          }
          case MALPubSubOperation._DEREGISTER_STAGE:
          {
            if (null == lqs)
            {
              lqs = jtransport.getCurrentConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
              localSession = true;
            }

            internalHandleDeregister(msg, lqs);
            break;
          }
          case MALPubSubOperation._PUBLISH_DEREGISTER_STAGE:
          {
            if (null == lqs)
            {
              lqs = jtransport.getCurrentConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
              localSession = true;
            }

            internalHandlePublishDeregister(msg, lqs);
            break;
          }
          default:
          {
            throw new UnsupportedOperationException("JMS should not be sending this PubSub message stage.: " + msg.getHeader().getInteractionStage().getValue());
          }
        }
      }
      else
      {
        if (null == lqs)
        {
          lqs = jtransport.getCurrentConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
        }

        super.internalSendMessage(lqs, lastForHandle, msg);
      }

      if (localSession)
      {
        lqs.close();
      }
    }
    catch (MALTransmitErrorException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      JMSTransport.RLOGGER.log(Level.WARNING, "JMS Error occurred {0}", e);

      throw new MALTransmitErrorException(msg.getHeader(), new MALStandardError(MALHelper.INTERNAL_ERROR_NUMBER, new Union(e.getMessage())), null);
    }
  }

  protected void internalHandleRegister(final GENMessage msg, Session lqs) throws MALException, MALInteractionException
  {
    // get components parts of messsage
    Subscription subscription = (Subscription) msg.getBody().getBodyElement(0, new Subscription());
    final String strURL = msg.getHeader().getURITo().getValue();
    final int iSecond = strURL.indexOf(JMSTransport.JMS_SERVICE_DELIM);
    final String providerExchangeName = strURL.substring(iSecond + 1);

    String subscriptionKey = queueName + "::" + providerExchangeName + "::" + subscription.getSubscriptionId().getValue();

    JMSConsumeHandler handler;
    if (consumeHandlerMap.containsKey(subscriptionKey))
    {
      handler = consumeHandlerMap.get(subscriptionKey);
      //handler.deregister();
    }
    else
    {
      try
      {
        // get the queue
        String exchangeName = providerExchangeName + ":" + msg.getHeader().getSession().toString() + ":" + msg.getHeader().getSessionName();
        Topic dest = jtransport.getAdministrator().getTopic(lqs, exchangeName);

        handler = new JMSConsumeHandler(this, interruption, qs, dest, subscriptionKey, msg.getHeader().getServiceArea(), msg.getHeader().getService(), msg.getHeader().getOperation(), msg.getHeader().getAreaVersion());
        consumeHandlerMap.put(subscriptionKey, handler);
      }
      catch (NameNotFoundException e)
      {
        JMSTransport.RLOGGER.log(Level.WARNING, "JMS: remote topic name not found {0}", providerExchangeName);

        throw new MALException("MALException.DESTINATION_TRANSIENT", null);
      }
      catch (Exception e)
      {
        JMSTransport.RLOGGER.log(Level.WARNING, "JMS Error occurred when registering {0}", e);

        throw new MALException("MALException.INTERNAL_FAILURE", null);
      }
    }
    
    try
    {
      handler.register(jtransport, providerExchangeName, msg, subscription);
    }
    catch (Exception e)
    {
      JMSTransport.RLOGGER.log(Level.WARNING, "JMS Error occurred when registering {0}", e);

      throw new MALException("MALException.INTERNAL_FAILURE", null);
    }

    // create response and do callback
    GENMessage returnMsg = new GENMessage(false, createReturnHeader(msg, false), null, null, (Object[]) null);
    receiveMessage(returnMsg);
  }

  protected void internalHandlePublishRegister(final GENMessage msg, Session lqs) throws MALException, MALInteractionException
  {
    MALMessageHeader hdr = msg.getHeader();

    JMSPublishHandler details = publishHandlerMap.get(createProviderKey(hdr));

    if (null == details)
    {
      details = new JMSPublishHandler(jtransport, msg);
      publishHandlerMap.put(createProviderKey(hdr), details);
      JMSTransport.RLOGGER.log(Level.FINE, "New JMS publisher registering: {0}", hdr);
    }

    details.setKeyList(hdr, ((MALPublishRegisterBody) msg.getBody()).getEntityKeyList());

    // create response and do callback
    GENMessage returnMsg = new GENMessage(false, createReturnHeader(msg, false), null, null, (Object[]) null);
    receiveMessage(returnMsg);
  }

  protected void internalHandlePublish(final GENMessage msg, Session lqs) throws MALException, MALInteractionException, MALTransmitErrorException
  {
    JMSTransport.RLOGGER.fine("Starting PUBLISH");
    JMSPublishHandler details = publishHandlerMap.get(createProviderKey(msg.getHeader()));

    if (null == details)
    {
      JMSTransport.RLOGGER.warning("JMS : ERR Provider not known");
      throw new MALInteractionException(new MALStandardError(MALHelper.INCORRECT_STATE_ERROR_NUMBER, null));
    }

    GENMessage rMsg = details.publish(msg, lqs);
    if (null != rMsg)
    {
      receiveMessage(rMsg);
    }
  }

  protected void internalHandleDeregister(final GENMessage msg, Session lqs) throws MALException, MALInteractionException
  {
    // get components parts of messsage
    IdentifierList subList = (IdentifierList) msg.getBody().getBodyElement(0, new IdentifierList());
    final String strURL = msg.getHeader().getURITo().getValue();
    final int iSecond = strURL.indexOf(JMSTransport.JMS_SERVICE_DELIM);
    final String providerExchangeName = strURL.substring(iSecond + 1);

    for (int i = 0; i < subList.size(); ++i)
    {
      String subscriptionKey = queueName + "::" + providerExchangeName + "::" + subList.get(i);

      if (consumeHandlerMap.containsKey(subscriptionKey))
      {
        JMSConsumeHandler handler = consumeHandlerMap.get(subscriptionKey);
        consumeHandlerMap.remove(subscriptionKey);
        handler.deregister(true);
      }
      else
      {
        JMSTransport.RLOGGER.log(Level.WARNING, "JMS Error deregistering for unregistered subscription {0}", subscriptionKey);
      }
    }

    // create response and do callback
    GENMessage returnMsg = new GENMessage(false, createReturnHeader(msg, false), null, null, (Object[]) null);
    receiveMessage(returnMsg);
  }

  protected void internalHandlePublishDeregister(final GENMessage msg, Session lqs) throws MALException, MALInteractionException
  {
    GENMessage returnMsg = new GENMessage(false, createReturnHeader(msg, false), null, null, (Object[]) null);
    
    JMSPublishHandler hdlr = publishHandlerMap.remove(createProviderKey(msg.getHeader()));
    if (null != hdlr)
    {
      hdlr.deregister(returnMsg);
      JMSTransport.RLOGGER.log(Level.FINE, "Removing JMS publisher details: {0}", msg.getHeader());
    }

    // create response and do callback
    receiveMessage(returnMsg);
  }

  public static byte[] createExchangeMessage(int index, UpdateHeaderList headerList, List[] valueLists, MALElementStreamFactory streamFactory) throws MALException
  {
    try
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      MALElementOutputStream enc = streamFactory.createOutputStream(baos);

      enc.writeElement(new UShort(valueLists.length + 1), null);
      writeListElement(index, headerList, enc);

      for (List valueList : valueLists)
      {
        writeListElement(index, valueList, enc);
      }

      enc.flush();
      enc.close();

      return baos.toByteArray();
    }
    catch (Throwable ex)
    {
      throw new MALException("Internal error encoding message", ex);
    }
  }

  public static void writeListElement(int index, List srcList, MALElementOutputStream enc) throws MALException
  {
    Object e = srcList.get(index);
    List l = (List) ((Element) srcList).createElement();
    l.add(e);
    enc.writeElement(l, null);
  }

  static GENMessageHeader createReturnHeader(MALMessage sourceMessage, boolean isError)
  {
    return createReturnHeader(sourceMessage, isError, (short) (sourceMessage.getHeader().getInteractionStage().getValue() + 1));
  }

  static GENMessageHeader createReturnHeader(MALMessage sourceMessage, boolean isError, short stage)
  {
    GENMessageHeader hdr = new GENMessageHeader();
    MALMessageHeader srcHdr = sourceMessage.getHeader();

    hdr.setURIFrom(srcHdr.getURITo());
    hdr.setURITo(srcHdr.getURIFrom());
    hdr.setAuthenticationId(new Blob(JMSTransport.authId));
    hdr.setTimestamp(new Time(new java.util.Date().getTime()));
    hdr.setQoSlevel(srcHdr.getQoSlevel());
    hdr.setPriority(srcHdr.getPriority());
    hdr.setDomain(srcHdr.getDomain());
    hdr.setNetworkZone(srcHdr.getNetworkZone());
    hdr.setSession(srcHdr.getSession());
    hdr.setSessionName(srcHdr.getSessionName());
    hdr.setInteractionType(srcHdr.getInteractionType());
    hdr.setInteractionStage(new UOctet(stage));
    hdr.setTransactionId(srcHdr.getTransactionId());
    hdr.setServiceArea(srcHdr.getServiceArea());
    hdr.setService(srcHdr.getService());
    hdr.setOperation(srcHdr.getOperation());
    hdr.setAreaVersion(srcHdr.getAreaVersion());
    hdr.setIsErrorMessage(isError);

    return hdr;
  }

  private static String createProviderKey(MALMessageHeader details)
  {
    StringBuilder buf = new StringBuilder();

    buf.append(details.getSession());
    buf.append(':');
    buf.append(details.getSessionName());
    buf.append(':');
    buf.append(details.getNetworkZone());
    buf.append(':');
    buf.append(details.getDomain());

    return buf.toString();
  }

  protected static class PublishEntry
  {
    public final EntityKey eKey;
    public final boolean isModification;
    public final byte[] update;

    public PublishEntry(EntityKey eKey, boolean isModification, byte[] update)
    {
      this.eKey = eKey;
      this.isModification = isModification;
      this.update = update;
    }
  }

  protected static class MessageContext
  {
    public final Session lqs;
    public final boolean closeSession;

    public MessageContext(Session lqs, boolean closeSession)
    {
      this.lqs = lqs;
      this.closeSession = closeSession;
    }
  }
}
