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

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.broker.MALBrokerBinding;
import org.ccsds.moims.mo.mal.structures.*;
import org.ccsds.moims.mo.mal.transport.MALEndpoint;
import org.ccsds.moims.mo.mal.transport.MALTransport;
import org.ccsds.moims.mo.mal.transport.MALTransportFactory;
import esa.mo.mal.transport.gen.GENEndpoint;
import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.GENTransport;
import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.sending.GENOutgoingMessageHolder;
import java.io.IOException;
import javax.naming.NameNotFoundException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.transport.MALTransmitErrorException;

/**
 *
 */
public class JMSTransport extends GENTransport implements MALTransport
{
  /**
   * Logger
   */
  public static final java.util.logging.Logger RLOGGER = Logger.getLogger("org.ccsds.moims.mo.mal.transport.jms");
  public static final byte[] authId = "JMS".getBytes();
  public static final char JMS_SERVICE_DELIM = '_';
  public static final char JMS_BROKER_DELIM = '[';
  private final JMSAbstractAdministrator administrator;
  private Connection queueConnection;
  private final Hashtable namingContextEnv;

  public JMSTransport(MALTransportFactory factory, String protocol, JMSAbstractAdministrator administrator, java.util.Map properties) throws Exception
  {
    super(protocol, JMS_SERVICE_DELIM, true, true, factory, properties);

    this.administrator = administrator;

    namingContextEnv = new Hashtable();

    namingContextEnv.put("java.naming.factory.initial",
            System.getProperty("java.naming.factory.initial"));
    namingContextEnv.put("java.naming.factory.host",
            System.getProperty("java.naming.factory.host"));
    namingContextEnv.put("java.naming.factory.port",
            System.getProperty("java.naming.factory.port"));
  }

  @Override
  public void init() throws MALException
  {
    super.init();

    // initialization of the jms administrator
    try
    {
      getAdministrator().init(this, namingContextEnv);
    }
    catch (Exception ex)
    {
      throw new MALException("Error on creating and initialising the JMS administration interface", ex);
    }
  }

  @Override
  protected GENEndpoint internalCreateEndpoint(String localName, String routingName, Map qosProperties) throws MALException
  {
    try
    {
      Session qs = getCurrentConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue q = getAdministrator().createQueue(qs, routingName);

      return new JMSEndpoint(this, localName, routingName, uriBase, qs, q);
    }
    catch (MALException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      RLOGGER.log(Level.SEVERE, "Error occurred when attempting to create end point {0}", ex);
    }

    return null;
  }

  public MALBrokerBinding createBroker(String localName, Blob authenticationId, QoSLevel[] expectedQos, UInteger priorityLevelNumber, Map defaultQoSProperties) throws MALException
  {
    // not support by transport
    return new JMSBrokerBinding(new URI(uriBase + localName), localName, authenticationId, expectedQos, priorityLevelNumber);
  }

  public MALBrokerBinding createBroker(MALEndpoint endpoint, Blob authenticationId, QoSLevel[] qosLevels, UInteger priorities, Map properties) throws MALException
  {
    // not support by transport
    return new JMSBrokerBinding(new URI(uriBase + endpoint.getLocalName()), endpoint.getLocalName(), authenticationId, qosLevels, priorities);
  }

  public boolean isSupportedInteractionType(InteractionType type)
  {
    return true;
  }

  public boolean isSupportedQoSLevel(QoSLevel qos)
  {
    return qos.getOrdinal() == QoSLevel._BESTEFFORT_INDEX;
  }

  public String getJndiPort()
  {
    return (String) namingContextEnv.get("java.naming.factory.port");
  }

  public String getJndiHostName()
  {
    return (String) namingContextEnv.get("java.naming.factory.host");
  }

  public JMSAbstractAdministrator getAdministrator() throws MALException
  {
    return administrator;
  }

  public Connection getCurrentConnection() throws Exception
  {
    if (queueConnection == null)
    {
      ConnectionFactory qcf = getAdministrator().getConnectionFactory();

      queueConnection = qcf.createConnection();
      queueConnection.start();		// used to respond to requests

    }
    return queueConnection;
  }

  @Override
  protected String createTransportAddress() throws MALException
  {
    return getJndiHostName() + ":" + getJndiPort() + "/" + getJndiPort();
  }

  @Override
  protected GENMessageSender createMessageSender(GENMessage msg, String remoteRootURI) throws MALException, MALTransmitErrorException
  {
    RLOGGER.log(Level.FINE, "JMS received request to create connections to URI:{0}", remoteRootURI);

    // create new sender for this URI
    return new JMSMessageSender(remoteRootURI);
  }

  @Override
  public void close() throws MALException
  {
    RLOGGER.info("Transport closing");

    try
    {
      getCurrentConnection().close();
    }
    catch (Exception e)
    {
      RLOGGER.log(Level.WARNING, "Transport closing exception", e);
    }

    super.close();
  }

  private class JMSMessageSender implements GENMessageSender
  {
    private final String remoteRootURI;

    public JMSMessageSender(String remoteRootURI)
    {
      this.remoteRootURI = remoteRootURI;
    }

    public void sendEncodedMessage(GENOutgoingMessageHolder tmsg) throws IOException
    {
      String sendRoutingKey = tmsg.getDestinationURI().substring(remoteRootURI.length() + 1);

      RLOGGER.log(Level.FINE, "Attempting to send to {0}", new Object[]
      {
        remoteRootURI
      });

      Session lqs = (Session) tmsg.getMultiSendHandle();

      try
      {
        // get the queue
        Queue destQueue = null;
        try
        {
          destQueue = getAdministrator().getQueue(lqs, sendRoutingKey);
        }
        catch (NameNotFoundException e)
        {
          RLOGGER.log(Level.SEVERE, "Remote JMS queue name not found {0}", sendRoutingKey);

          throw new MALInteractionException(new MALStandardError(MALHelper.DESTINATION_UNKNOWN_ERROR_NUMBER, null));
        }

        if (null != destQueue)
        {
          ObjectMessage objMsg = lqs.createObjectMessage();
          objMsg.setIntProperty(JMSEndpoint.ARR_PROPERTY, 1);
          objMsg.setIntProperty(JMSEndpoint.SVC_PROPERTY, 1);
          objMsg.setIntProperty(JMSEndpoint.OPN_PROPERTY, 1);

          objMsg.setObject(tmsg.getEncodedMessage());

          MessageProducer sender = lqs.createProducer(destQueue);
          sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          sender.send(objMsg);

          sender.close();

          RLOGGER.log(Level.FINE, "Sending data to {0} : {2}", new Object[]
          {
            sendRoutingKey, tmsg.getEncodedMessage()
          });

          if (tmsg.isLastForHandle())
          {
            if (lqs.getTransacted())
            {
              RLOGGER.fine("Commiting transaction");
              lqs.commit();
            }

            lqs.close();
          }
          RLOGGER.log(Level.FINE, "Sent data to {0}", new Object[]
          {
            sendRoutingKey
          });
        }
        else
        {
          RLOGGER.log(Level.WARNING, "Remote JMS queue name resolved to NULL {0}", sendRoutingKey);

          throw new MALInteractionException(new MALStandardError(MALHelper.DESTINATION_UNKNOWN_ERROR_NUMBER, null));
        }
      }
      catch (Throwable e)
      {
        RLOGGER.log(Level.SEVERE, "Error occurred when sending data to " + sendRoutingKey + " : {0}", e);

        try
        {
          if (tmsg.isLastForHandle())
          {
            if (lqs.getTransacted())
            {
              RLOGGER.fine("Rolling back transaction");
              lqs.rollback();
            }

            lqs.close();
          }
        }
        catch (JMSException ex)
        {
          ex.printStackTrace();
        }
      }
    }

    public void close()
    {
    }
  }
}
