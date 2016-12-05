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

import esa.mo.mal.transport.gen.receivers.GENIncomingMessageDecoder;
import java.util.logging.Level;
import javax.jms.*;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.UShort;
import esa.mo.mal.transport.jms.util.StructureHelper;

/**
 *
 */
public class JMSQueueHandler implements MessageListener
{
  protected final JMSEndpoint endPoint;
  protected final Object interruption;
  protected final Destination messageSource;
  protected final String sourceName;
  protected Session queueSession;
  private final MessageConsumer consumer;

  public JMSQueueHandler(JMSEndpoint endPoint, Object interruption, Session qs, Destination messageSource, String sourceName) throws Exception
  {
    this.endPoint = endPoint;
    this.interruption = interruption;
    this.messageSource = messageSource;
    this.queueSession = qs;
    this.sourceName = sourceName;

    consumer = queueSession.createConsumer(messageSource);
    consumer.setMessageListener(this);

    JMSTransport.RLOGGER.log(Level.FINE, "JMS JMSQueueHandler created for: {0}", messageSource);
  }

  public JMSQueueHandler(JMSEndpoint endPoint, Object interruption, Session qs, Topic messageSource, String sourceName) throws Exception
  {
    this.endPoint = endPoint;
    this.interruption = interruption;
    this.messageSource = messageSource;
    this.queueSession = qs;
    this.sourceName = sourceName;
    this.consumer = null;

    JMSTransport.RLOGGER.log(Level.FINE, "JMS JMSQueueHandler created for: {0}", messageSource);
  }

  /**
   * Reception of a message from JMS implementation
   * @param msg The JMS message
   */
  public void onMessage(Message msg)
  {
    JMSTransport.RLOGGER.fine("JMS onMessage");

    try
    {
      if (msg instanceof ObjectMessage)
      {
        ObjectMessage objMsg = (ObjectMessage) msg;
        Object dat = objMsg.getObject();
        // we use the same message container as RMI protocol
        if (dat instanceof byte[])
        {
          IdentifierList d = StructureHelper.stringToDomain(objMsg.getStringProperty(JMSEndpoint.DOM_PROPERTY));
          Identifier n = new Identifier(objMsg.getStringProperty(JMSEndpoint.NET_PROPERTY));
          UShort a = new UShort(objMsg.getIntProperty(JMSEndpoint.ARR_PROPERTY));
          UShort s = new UShort(objMsg.getIntProperty(JMSEndpoint.SVC_PROPERTY));
          UShort o = new UShort(objMsg.getIntProperty(JMSEndpoint.OPN_PROPERTY));

          endPoint.getJtransport().receive(null, createMessageDecoder(new JMSUpdate(d, n, a, s, o, (byte[]) dat)));
        }
        else
        {
          JMSTransport.RLOGGER.log(Level.WARNING, "JMS received bad message format: {0}", objMsg.getObject().getClass().getName());
        }
      }
      else
      {
        JMSTransport.RLOGGER.log(Level.WARNING, "JMS received bad message type: {0}", msg.getClass().getName());
      }
    }
    catch (Throwable e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  protected GENIncomingMessageDecoder createMessageDecoder(JMSUpdate update)
  {
    return new JMSIncomingMessageDecoder(endPoint.getJtransport(), update);
  }
}
