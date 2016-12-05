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
package esa.mo.mal.transport.jms.util;

import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;


public abstract class StructureHelper
{
  /**
   * Converts a identifier list version of a domain name to a single, dot delimited, String.
   *
   * @param domain The list of identifiers to concatenate.
   * @return The dot delimited version of the domain name.
   */
  public static String domainToString(final IdentifierList domain)
  {
    return domainToString(domain, 0);
  }

  /**
   * Converts a identifier list version of a domain name to a single, dot delimited, String.
   *
   * @param domain The list of identifiers to concatenate.
   * @param truncateCount The number of elements of the list to ignore off the end.
   * @return The dot delimited version of the domain name.
   */
  public static String domainToString(final IdentifierList domain, final int truncateCount)
  {
    String retVal = null;

    if (null != domain)
    {
      final StringBuilder buf = new StringBuilder();
      int i = 0;
      final int e = domain.size() - truncateCount;
      while (i < e)
      {
        if (0 < i)
        {
          buf.append('.');
        }

        buf.append((Identifier) domain.get(i));

        ++i;
      }

      retVal = buf.toString();
    }

    return retVal;
  }

  /**
   * Converts a String based, dot delimited, domain identifier to an identifier list version.
   *
   * @param domain The delimited version of the domain name.
   * @return The list of identifiers to concatenate.
   */
  public static IdentifierList stringToDomain(final String domain)
  {
    IdentifierList rv = null;

    if (null != domain)
    {
      final String[] parts = domain.split("\\.");

      rv = new IdentifierList(parts.length);
      for (int i = 0; i < parts.length; i++)
      {
        rv.add(new Identifier(parts[i]));
      }
    }

    return rv;
  }

  /**
   * Determines if one domain is a sub-domain, or the same domain, of another. For example, a.b.c is a sub-domain of a.b
   *
   * @param srcDomain The main domain.
   * @param testDomain The sub-domain.
   * @return True if tesDomain is a sub-domain of srcDomain, else false.
   */
  public static boolean isSubDomainOf(final IdentifierList srcDomain, final IdentifierList testDomain)
  {
    if ((null != srcDomain) && (null != testDomain))
    {
      if (srcDomain.size() <= testDomain.size())
      {
        int i = 0;
        final int e = srcDomain.size();
        while (i < e)
        {
          final Identifier sId = srcDomain.get(i);
          final Identifier tId = testDomain.get(i);

          if ((sId == null) ? (tId != null) : !sId.equals(tId))
          {
            return false;
          }

          ++i;
        }

        return true;
      }
    }
    else
    {
      if ((null == srcDomain) && (null == testDomain))
      {
        return true;
      }
    }

    return false;
  }
}
