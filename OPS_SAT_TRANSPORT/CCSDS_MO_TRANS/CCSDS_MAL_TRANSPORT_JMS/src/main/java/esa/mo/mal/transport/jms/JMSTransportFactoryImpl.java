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

import java.util.Map;
import java.util.logging.Level;
import org.ccsds.moims.mo.mal.MALContext;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.transport.MALTransport;
import org.ccsds.moims.mo.mal.transport.MALTransportFactory;


/**
 *
 */
public class JMSTransportFactoryImpl extends MALTransportFactory
{
  private static final String JMS_ADMIN_CLASS = "org.ccsds.moims.mo.jms.admin.class";
  private static final String AMQP_ADMIN_CLASS = "org.ccsds.moims.mo.jms.amqp.admin.class";
  private static final String JMSPS = "ccsdsjms";
  private static final String AMQPPS = "amqpjms";
  private static final Object mutex = "JMSTransportFactoryImpl.mutex";
  private JMSTransport transport = null;

  public JMSTransportFactoryImpl(String protocol)
  {
    super(protocol);
  }

  @Override
  public MALTransport createTransport(MALContext ctx, Map properties) throws MALException
  {
    synchronized (mutex)
    {
      init(properties);

      return transport;
    }
  }

  protected void init(Map properties) throws MALException
  {
    if (null == transport)
    {
      try
      {
        String prot = getProtocol();
        if (JMSPS.equals(prot))
        {
          transport = new JMSTransport(this, prot, getAdministrator(false), properties);
        }
        else if (AMQPPS.equals(prot))
        {
          transport = new JMSTransport(this, prot, getAdministrator(true), properties);
        }
        else
        {
          throw new MALException("Unknown JMS transport required! " + prot);
        }

        transport.init();
      }
      catch (Exception ex)
      {
        JMSTransport.RLOGGER.log(Level.SEVERE, "Exception thrown during the creation of the JMSTransport: {0}", ex);
      }
    }
  }

  private JMSAbstractAdministrator getAdministrator(boolean amqp) throws MALException
  {
    try
    {
      String adminClassName;

      if (amqp)
      {
        adminClassName = System.getProperty(AMQP_ADMIN_CLASS);
      }
      else
      {
        adminClassName = System.getProperty(JMS_ADMIN_CLASS);
      }

      Class adminClass = Class.forName(adminClassName);
      return (JMSAbstractAdministrator) adminClass.newInstance();
    }
    catch (Exception e)
    {
      throw new MALException("Unable to create JMS adminstration class", e);
    }
  }
}
