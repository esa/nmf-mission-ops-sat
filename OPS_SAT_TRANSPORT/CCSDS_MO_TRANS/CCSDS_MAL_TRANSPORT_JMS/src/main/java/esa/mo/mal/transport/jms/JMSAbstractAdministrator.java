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
import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Topic;

public abstract class JMSAbstractAdministrator
{
  protected JMSTransport transport;

  public void init(JMSTransport transport, Hashtable namingContextEnv) throws Exception
  {
    this.transport = transport;
  }

  public abstract Topic createTopic(String name) throws Exception;
  public abstract void bindTopic(javax.jms.Session session, Topic topic) throws Exception;
  public abstract Topic getTopic(javax.jms.Session session, String name) throws Exception;
  public abstract void deleteTopic(javax.jms.Session session, Topic topic) throws Exception;

  public abstract Queue createQueue(javax.jms.Session session, String name) throws Exception;
  public abstract void bindQueue(javax.jms.Session session, Queue queue) throws Exception;
  public abstract Queue getQueue(javax.jms.Session session, String name) throws Exception;
  public abstract void deleteQueue(javax.jms.Session session, Queue queue) throws Exception;

  public abstract ConnectionFactory getConnectionFactory() throws Exception;
}
