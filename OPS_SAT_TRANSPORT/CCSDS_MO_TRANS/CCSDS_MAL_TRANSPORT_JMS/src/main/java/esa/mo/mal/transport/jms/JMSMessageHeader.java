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

import esa.mo.mal.transport.gen.GENMessageHeader;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;

/**
 *
 */
public class JMSMessageHeader extends GENMessageHeader
{
  public JMSMessageHeader(MALMessageHeader srcHeader, JMSUpdate update)
  {
    super(srcHeader.getURIFrom(), srcHeader.getAuthenticationId(), srcHeader.getURITo(), srcHeader.getTimestamp(), srcHeader.getQoSlevel(), srcHeader.getPriority(), update.getDomain(), update.getNetwork(), srcHeader.getSession(), srcHeader.getSessionName(), srcHeader.getInteractionType(), srcHeader.getInteractionStage(), srcHeader.getTransactionId(), update.getServiceArea(), update.getService(), update.getOperation(), srcHeader.getAreaVersion(), srcHeader.getIsErrorMessage());
  }
}
