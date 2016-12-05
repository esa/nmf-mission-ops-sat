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

import org.ccsds.moims.mo.mal.structures.EntityKey;
import org.ccsds.moims.mo.mal.structures.Identifier;

/**
 * Simple class that represents a MAL update key.
 */
public final class JMSPublisherKey implements Comparable
{
  /**
   * Match all constant.
   */
  private static final String ALL_ID = "*";
  private static final Long ALL_NUMBER = 0L;
  private static final int HASH_MAGIC_NUMBER = 47;
  public final String key1;
  public final Long key2;
  public final Long key3;
  public final Long key4;

  /**
   * Constructor.
   * @param lst Entity key.
   */
  public JMSPublisherKey(final EntityKey lst)
  {
    super();

    this.key1 = getIdValue(lst.getFirstSubKey());
    this.key2 = lst.getSecondSubKey();
    this.key3 = lst.getThirdSubKey();
    this.key4 = lst.getFourthSubKey();
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    final JMSPublisherKey other = (JMSPublisherKey) obj;
    if ((this.key1 == null) ? (other.key1 != null) : !this.key1.equals(other.key1))
    {
      return false;
    }
    if ((this.key2 == null) ? (other.key2 != null) : !this.key2.equals(other.key2))
    {
      return false;
    }
    if ((this.key3 == null) ? (other.key3 != null) : !this.key3.equals(other.key3))
    {
      return false;
    }
    
    return !((this.key4 == null) ? (other.key4 != null) : !this.key4.equals(other.key4));
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = HASH_MAGIC_NUMBER * hash + (this.key1 != null ? this.key1.hashCode() : 0);
    hash = HASH_MAGIC_NUMBER * hash + (this.key2 != null ? this.key2.hashCode() : 0);
    hash = HASH_MAGIC_NUMBER * hash + (this.key3 != null ? this.key3.hashCode() : 0);
    hash = HASH_MAGIC_NUMBER * hash + (this.key4 != null ? this.key4.hashCode() : 0);
    return hash;
  }

  @Override
  public int compareTo(final Object o)
  {
    final JMSPublisherKey rhs = (JMSPublisherKey) o;
    int rv = compareSubkey(this.key1, rhs.key1);
    if (0 == rv)
    {
      rv = compareSubkey(this.key2, rhs.key2);
      if (0 == rv)
      {
        rv = compareSubkey(this.key3, rhs.key3);
        if (0 == rv)
        {
          rv = compareSubkey(this.key4, rhs.key4);
        }
      }
    }
    
    return rv;
  }

  /**
   * Returns true if this key matches supplied argument taking into account wildcards.
   * @param rhs Key to match against.
   * @return True if matches.
   */
  public boolean matches(final EntityKey rhs)
  {
    if (null != rhs)
    {
      boolean matched = matchedSubkey(key1, getIdValue(rhs.getFirstSubKey()));

      if (matched)
      {
        matched = matchedSubkey(key2, rhs.getSecondSubKey());
        if (matched)
        {
          matched = matchedSubkey(key3, rhs.getThirdSubKey());
          if (matched)
          {
            matched = matchedSubkey(key4, rhs.getFourthSubKey());
          }
        }
      }

      return matched;
    }

    return false;
  }

  private static int compareSubkey(final String myKeyPart, final String theirKeyPart)
  {
    if ((null == myKeyPart) || (null == theirKeyPart))
    {
      if ((null != myKeyPart) || (null != theirKeyPart))
      {
        if (null == myKeyPart)
        {
          return -1;
        }
        
        return 1;
      }
    }
    else
    {
      if (!myKeyPart.equals(theirKeyPart))
      {
        return myKeyPart.compareTo(theirKeyPart);
      }
    }
    
    return 0;
  }

  private static int compareSubkey(final Long myKeyPart, final Long theirKeyPart)
  {
    if ((null == myKeyPart) || (null == theirKeyPart))
    {
      if ((null != myKeyPart) || (null != theirKeyPart))
      {
        if (null == myKeyPart)
        {
          return -1;
        }

        return 1;
      }
    }
    else
    {
      if (!myKeyPart.equals(theirKeyPart))
      {
        return myKeyPart.compareTo(theirKeyPart);
      }
    }

    return 0;
  }

  private static boolean matchedSubkey(final String myKeyPart, final String theirKeyPart)
  {
    if (ALL_ID.equals(myKeyPart) || ALL_ID.equals(theirKeyPart))
    {
      return true;
    }
    
    if ((null == myKeyPart) || (null == theirKeyPart))
    {
      return (null == myKeyPart) && (null == theirKeyPart);
    }
    
    return myKeyPart.equals(theirKeyPart);
  }

  private static boolean matchedSubkey(final Long myKeyPart, final Long theirKeyPart)
  {
    if (ALL_NUMBER.equals(myKeyPart) || ALL_NUMBER.equals(theirKeyPart))
    {
      return true;
    }
    
    if ((null == myKeyPart) || (null == theirKeyPart))
    {
      return (null == myKeyPart) && (null == theirKeyPart);
    }
    
    return myKeyPart.equals(theirKeyPart);
  }

  private static String getIdValue(final Identifier id)
  {
    if ((null != id) && (null != id.getValue()))
    {
      return id.getValue();
    }

    return null;
  }

  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder();
    buf.append('[');
    buf.append(this.key1);
    buf.append('.');
    buf.append(this.key2);
    buf.append('.');
    buf.append(this.key3);
    buf.append('.');
    buf.append(this.key4);
    buf.append(']');
    return buf.toString();
  }  
}
