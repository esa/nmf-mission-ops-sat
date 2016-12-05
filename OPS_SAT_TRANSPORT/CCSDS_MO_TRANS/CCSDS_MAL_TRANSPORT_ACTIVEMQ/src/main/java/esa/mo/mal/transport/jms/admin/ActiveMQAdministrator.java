/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO ActiveMQ Transport Framework
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
package esa.mo.mal.transport.jms.admin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import esa.mo.mal.transport.gen.GENTransport;
import esa.mo.mal.transport.jms.JMSAbstractAdministrator;
import esa.mo.mal.transport.jms.JMSTransport;
import java.util.logging.Level;

/**
 *
 */
public class ActiveMQAdministrator extends JMSAbstractAdministrator
{
  public String amqJmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi";
  private String brokerURL = "localhost";

  public ActiveMQAdministrator()
  {
    GENTransport.LOGGER.info("JMS: Creating ActiveMQ Administrator");
  }

  @Override
  public void init(JMSTransport transport, Hashtable namingContextEnv) throws Exception
  {
    super.init(transport, namingContextEnv);

    amqJmxUrl = System.getProperty("java.jmx.provider.url", "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi");
    brokerURL = System.getProperty("java.naming.provider.url", "localhost");
  }

  @Override
  public ConnectionFactory getConnectionFactory() throws Exception
  {
    return new org.apache.activemq.ActiveMQConnectionFactory(brokerURL);
  }

  @Override
  public Queue createQueue(javax.jms.Session session, String name) throws Exception
  {
    return new ActiveMQQueue(name);
  }

  @Override
  public void bindQueue(javax.jms.Session session, Queue queue) throws Exception
  {
  }

  @Override
  public Queue getQueue(Session session, String name) throws Exception
  {
    return createQueue(session, name);
  }

  @Override
  public Topic createTopic(String name) throws Exception
  {
    Topic topic = new ActiveMQTopic(name);

    bindTopic(null, topic);

    return topic;
  }

  @Override
  public void bindTopic(Session session, Topic topic) throws Exception
  {
  }

  @Override
  public Topic getTopic(Session session, String name) throws Exception
  {
    return createTopic(name);
  }

  @Override
  public void deleteQueue(Session session, Queue queue) throws Exception
  {
    MBeanServerConnection conn = connect();

    String brokerNameQuery = "org.apache.activemq:type=Broker,brokerName=localhost";
    String removeTopicOperationName = "removeQueue";
    Object[] params =
    {
      queue.getQueueName()
    };
    String[] sig =
    {
      "java.lang.String"
    };

    doTopicCrud(conn, queue.getQueueName(), brokerNameQuery, removeTopicOperationName, params, sig, "remov");
  }

  @Override
  public void deleteTopic(Session session, Topic topic) throws Exception
  {
    MBeanServerConnection conn = connect();

    String brokerNameQuery = "org.apache.activemq:type=Broker,brokerName=localhost";
    String removeTopicOperationName = "removeTopic";
    Object[] params =
    {
      topic.getTopicName()
    };
    String[] sig =
    {
      "java.lang.String"
    };

    doTopicCrud(conn, topic.getTopicName(), brokerNameQuery, removeTopicOperationName, params, sig, "remov");
  }

  public MBeanServerConnection connect() throws IOException
  {
    JMXConnector connector;
    MBeanServerConnection connection = null;

    String username = "";

    String password = "";

    Map env = new HashMap();
    String[] credentials = new String[]
    {
      username, password
    };
    env.put(JMXConnector.CREDENTIALS, credentials);

    try
    {
      connector = JMXConnectorFactory.newJMXConnector(new JMXServiceURL(amqJmxUrl), env);
      connector.connect();
      connection = connector.getMBeanServerConnection();
    }
    catch (MalformedURLException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return connection;
  }

  private void doTopicCrud(MBeanServerConnection conn, String topicName,
          String brokerNameQuery, String operationName, Object[] params, String[] sig, String verb)
          throws IOException
  {
    if (null != conn)
    {
      try
      {
        GENTransport.LOGGER.log(Level.INFO, "{0}ing new topic: [{1}]", new Object[]
        {
          verb, topicName
        });
        ObjectName brokerObjName = new ObjectName(brokerNameQuery);
        conn.invoke(brokerObjName, operationName, params, sig);
        GENTransport.LOGGER.log(Level.INFO, "Topic [{0}] has been {1}ed", new Object[]
        {
          topicName, verb
        });
      }
      catch (MalformedObjectNameException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      catch (NullPointerException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      catch (InstanceNotFoundException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      catch (MBeanException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      catch (ReflectionException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    else
    {
      GENTransport.LOGGER.log(Level.WARNING, "Unable to {0}e topic: [{1}] as no connection to JMX configured", new Object[]
      {
        verb, topicName
      });
    }
  }
}
