/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO SPP Transport Framework
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
package esa.mo.mal.transport.spp;

import org.ccsds.moims.mo.mal.structures.URI;

/**
 *
 */
public class SPPURIRepresentationSimple implements SPPURIRepresentation
{
  public SPPURIRepresentationSimple()
  {
  }

  @Override
  public short getApid(URI uri)
  {
    String val = uri.getValue();

    int i = val.indexOf('/') + 1;
    int j = val.indexOf('/', i);

    if (-1 == j)
    {
      j = val.length();
    }
    return Short.valueOf(val.substring(i, j));
  }

  @Override
  public boolean hasQualifier(URI uri)
  {
    String val = uri.getValue();
    int i = val.indexOf(':');
    int j = val.indexOf('/');

    return 1 < j - i;
  }

  @Override
  public int getQualifier(URI uri)
  {
    String val = uri.getValue();

    int i = val.indexOf(':') + 1;
    int j = val.indexOf('/', i);

    if (-1 == j)
    {
      j = val.length();
    }
    return Integer.valueOf(val.substring(i, j));
  }

  @Override
  public boolean hasSubId(URI uri)
  {
    String val = uri.getValue();
    int i = val.indexOf('/') + 1;

    return -1 != val.indexOf('/', i);
  }

  @Override
  public short getSubId(URI uri)
  {
    String val = uri.getValue();

    int i = val.indexOf('/') + 1;
    int j = val.indexOf('/', i) + 1;

    return Short.valueOf(val.substring(j, val.length()));
  }

  @Override
  public URI getURI(Integer apidQualifier, short apid, Short subId)
  {
    StringBuilder buf = new StringBuilder("malspp:");
    if (null != apidQualifier)
    {
      buf.append(apidQualifier);
    }

    buf.append('/');
    buf.append(apid);

    if (null != subId)
    {
      buf.append('/');
      buf.append(subId);
    }

    return new URI(buf.toString());
  }
}
