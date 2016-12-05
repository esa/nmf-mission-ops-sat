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

import static esa.mo.mal.transport.spp.SPPBaseTransport.AUTHENTICATION_ID_FLAG;
import static esa.mo.mal.transport.spp.SPPBaseTransport.DOMAIN_FLAG;
import static esa.mo.mal.transport.spp.SPPBaseTransport.ENCODE_BODY_FIXED;
import static esa.mo.mal.transport.spp.SPPBaseTransport.NETWORK_ZONE_FLAG;
import static esa.mo.mal.transport.spp.SPPBaseTransport.PRIORITY_FLAG;
import static esa.mo.mal.transport.spp.SPPBaseTransport.SEGMENT_MAX_SIZE_PROPERTY;
import static esa.mo.mal.transport.spp.SPPBaseTransport.SESSION_NAME_FLAG;
import static esa.mo.mal.transport.spp.SPPBaseTransport.TIMESTAMP_FLAG;
import java.util.Map;

/**
 * Small class that holds the encoding configuration for out going messages.
 */
public class SPPConfiguration
{
  private final boolean fixedBody;
  private final int segmentSize;
  private int flags = 0x0;
  private boolean srcSubId;
  private boolean dstSubId;
  private boolean priority;
  private boolean timestamp;
  private boolean network;
  private boolean session;
  private boolean domain;
  private boolean auth;

  public SPPConfiguration(boolean fixedBody,
          int segmentSize,
          boolean hasSrcSubId,
          boolean hasDstSubId,
          boolean hasPriority,
          boolean hasTimestamp,
          boolean hasNetwork,
          boolean hasSession,
          boolean hasDomain,
          boolean hasAuth)
  {
    this.fixedBody = fixedBody;
    this.segmentSize = segmentSize;
    this.srcSubId = hasSrcSubId;
    this.dstSubId = hasDstSubId;
    this.priority = hasPriority;
    this.timestamp = hasTimestamp;
    this.network = hasNetwork;
    this.session = hasSession;
    this.domain = hasDomain;
    this.auth = hasAuth;

    updateFlags();
  }

  public SPPConfiguration(SPPConfiguration other, final Map properties)
  {
    fixedBody = getBooleanProperty(properties, ENCODE_BODY_FIXED, other.fixedBody);
    
    int sms = other.segmentSize;
    if ((null != properties) && properties.containsKey(SEGMENT_MAX_SIZE_PROPERTY))
    {
      sms = Integer.parseInt(properties.get(SEGMENT_MAX_SIZE_PROPERTY).toString());
    }

    segmentSize = sms;
    srcSubId = other.srcSubId;
    dstSubId = other.dstSubId;
    priority = getBooleanProperty(properties, PRIORITY_FLAG, other.priority);
    timestamp = getBooleanProperty(properties, TIMESTAMP_FLAG, other.timestamp);
    network = getBooleanProperty(properties, NETWORK_ZONE_FLAG, other.network);
    session = getBooleanProperty(properties, SESSION_NAME_FLAG, other.session);
    domain = getBooleanProperty(properties, DOMAIN_FLAG, other.domain);
    auth = getBooleanProperty(properties, AUTHENTICATION_ID_FLAG, other.auth);
    updateFlags();
  }

  private boolean getBooleanProperty(final Map properties, final String propertyName, boolean existingValue)
  {
    if ((null != properties) && properties.containsKey(propertyName))
    {
      return Boolean.parseBoolean(properties.get(propertyName).toString());
    }

    return existingValue;

  }

  public int getFlags(boolean hasFromSubId, boolean hasToSubId)
  {
    if ((hasFromSubId == srcSubId) && (hasToSubId == dstSubId))
    {
      return flags;
    }

    return calculateFlags(hasFromSubId, hasToSubId, priority, timestamp, network, session, domain, auth);
  }

  public boolean isFixedBody()
  {
    return fixedBody;
  }

  public int getSegmentSize()
  {
    return segmentSize;
  }

  public boolean isSrcSubId()
  {
    return srcSubId;
  }

  public boolean isDstSubId()
  {
    return dstSubId;
  }

  public boolean isPriority()
  {
    return priority;
  }

  public boolean isTimestamp()
  {
    return timestamp;
  }

  public boolean isNetwork()
  {
    return network;
  }

  public boolean isSession()
  {
    return session;
  }

  public boolean isDomain()
  {
    return domain;
  }

  public boolean isAuth()
  {
    return auth;
  }

  public void setSrcSubId(boolean srcSubId)
  {
    this.srcSubId = srcSubId;
    updateFlags();
  }

  public void setDstSubId(boolean dstSubId)
  {
    this.dstSubId = dstSubId;
    updateFlags();
  }

  public void setPriority(boolean priority)
  {
    this.priority = priority;
    updateFlags();
  }

  public void setTimestamp(boolean timestamp)
  {
    this.timestamp = timestamp;
    updateFlags();
  }

  public void setNetwork(boolean network)
  {
    this.network = network;
    updateFlags();
  }

  public void setSession(boolean session)
  {
    this.session = session;
    updateFlags();
  }

  public void setDomain(boolean domain)
  {
    this.domain = domain;
    updateFlags();
  }

  public void setAuth(boolean auth)
  {
    this.auth = auth;
    updateFlags();
  }

  private void updateFlags()
  {
    flags = calculateFlags(srcSubId, dstSubId, priority, timestamp, network, session, domain, auth);
  }

  private static int calculateFlags(boolean srcSubId,
          boolean dstSubId,
          boolean priority,
          boolean timestamp,
          boolean network,
          boolean session,
          boolean domain,
          boolean auth)
  {
    int flags = 0;
    flags = srcSubId ? (flags | 0x80) : flags;
    flags = dstSubId ? (flags | 0x40) : flags;
    flags = priority ? (flags | 0x20) : flags;
    flags = timestamp ? (flags | 0x10) : flags;
    flags = network ? (flags | 0x08) : flags;
    flags = session ? (flags | 0x04) : flags;
    flags = domain ? (flags | 0x02) : flags;
    return auth ? (flags | 0x01) : flags;
  }
}
