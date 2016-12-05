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

import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.UShort;

/**
 *
 */
public class JMSUpdate
{
  private final IdentifierList domain;
  private final Identifier network;
  private final UShort serviceArea;
  private final UShort service;
  private final UShort operation;
  private final byte[] dat;

  public JMSUpdate(IdentifierList domain, Identifier network, UShort serviceArea, UShort service, UShort operation, byte[] dat)
  {
    this.domain = domain;
    this.network = network;
    this.serviceArea = serviceArea;
    this.service = service;
    this.operation = operation;
    this.dat = dat;
  }

  public IdentifierList getDomain()
  {
    return domain;
  }

  public Identifier getNetwork()
  {
    return network;
  }

  public UShort getOperation()
  {
    return operation;
  }

  public UShort getService()
  {
    return service;
  }

  public UShort getServiceArea()
  {
    return serviceArea;
  }

  public byte[] getDat()
  {
    return dat;
  }
}
