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
import java.util.Map;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.MALOperation;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.broker.MALBrokerBinding;
import org.ccsds.moims.mo.mal.structures.*;
import org.ccsds.moims.mo.mal.transport.MALMessage;
import org.ccsds.moims.mo.mal.transport.MALTransmitErrorListener;

/**
 *
 */
public class JMSBrokerBinding implements MALBrokerBinding
{
  private final URI uri;
  private final String localName;
  private final Blob authenticationId;
  private final QoSLevel[] expectedQos;
  private final UInteger priorityLevelNumber;
  private MALTransmitErrorListener listener = null;

  public JMSBrokerBinding(URI uri, String localName, Blob authenticationId, QoSLevel[] expectedQos, UInteger priorityLevelNumber)
  {
    this.uri = uri;
    this.localName = localName;
    this.authenticationId = new Blob(JMSTransport.authId);
    this.expectedQos = expectedQos;
    this.priorityLevelNumber = priorityLevelNumber;
  }
  
  public Blob getAuthenticationId()
  {
    return authenticationId;
  }

  public URI getURI()
  {
    return uri;
  }

  public void setTransmitErrorListener(MALTransmitErrorListener listener) throws MALException
  {
    this.listener = listener;
  }

  public MALTransmitErrorListener getTransmitErrorListener() throws MALException
  {
    return listener;
  }

  public MALMessage sendNotify(UShort area, UShort service, UShort operation, UOctet version, URI subscriber, Long transactionId, IdentifierList domainId, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel notifyQos, Map notifyQosProps, UInteger notifyPriority, Identifier subscriptionId, UpdateHeaderList updateHeaderList, List... updateList) throws IllegalArgumentException, MALInteractionException, MALException
  {
    JMSTransport.RLOGGER.warning("JMSBrokerBinding::sendNotify1");
    return null;
  }

  public MALMessage sendNotify(MALOperation op, URI subscriber, Long transactionId, IdentifierList domainId, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel notifyQos, Map notifyQosProps, UInteger notifyPriority, Identifier subscriptionId, UpdateHeaderList updateHeaderList, List... updateList) throws IllegalArgumentException, MALInteractionException, MALException
  {
    JMSTransport.RLOGGER.warning("JMSBrokerBinding::sendNotify2");
    return null;
  }

  public MALMessage sendNotifyError(UShort area, UShort service, UShort operation, UOctet version, URI subscriber, Long transactionId, IdentifierList domainId, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel notifyQos, Map notifyQosProps, UInteger notifyPriority, MALStandardError error) throws IllegalArgumentException, MALInteractionException, MALException
  {
    JMSTransport.RLOGGER.warning("JMSBrokerBinding::sendNotifyError1");
    return null;
  }

  public MALMessage sendNotifyError(MALOperation op, URI subscriber, Long transactionId, IdentifierList domainId, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel notifyQos, Map notifyQosProps, UInteger notifyPriority, MALStandardError error) throws IllegalArgumentException, MALInteractionException, MALException
  {
    JMSTransport.RLOGGER.warning("JMSBrokerBinding::sendNotifyError2");
    return null;
  }

  public MALMessage sendPublishError(UShort area, UShort service, UShort operation, UOctet version, URI publisher, Long transactionId, IdentifierList domainId, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel qos, Map qosProps, UInteger priority, MALStandardError error) throws IllegalArgumentException, MALInteractionException, MALException
  {
    JMSTransport.RLOGGER.warning("JMSBrokerBinding::sendPublishError1");
    return null;
  }

  public MALMessage sendPublishError(MALOperation op, URI publisher, Long transactionId, IdentifierList domainId, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel qos, Map qosProps, UInteger priority, MALStandardError error) throws IllegalArgumentException, MALInteractionException, MALException
  {
    JMSTransport.RLOGGER.warning("JMSBrokerBinding::sendPublishError2");
    return null;
  }

  public void close() throws MALException
  {
  }
}
