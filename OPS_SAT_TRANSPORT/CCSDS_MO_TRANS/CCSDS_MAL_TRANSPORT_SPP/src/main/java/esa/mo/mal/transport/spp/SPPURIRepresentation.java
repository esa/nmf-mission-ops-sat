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
 * Simple interface that allows the method of conversion of a URI to SPP API and subtype shorts to be delegated to the
 * actual transport used.
 */
public interface SPPURIRepresentation
{
  /**
   * Returns the APID to encode for the supplied URI
   *
   * @param uri The URI to interrogate.
   * @return the SPP APID to use
   */
  short getApid(URI uri);

  /**
   * Returns true if the supplied URI contains an APID qualifier
   *
   * @param uri The URI to interrogate
   * @return True if there is an APID qualifier.
   */
  boolean hasQualifier(URI uri);

  /**
   * Returns the APID qualifier to encode for the supplied URI
   *
   * @param uri The URI to interrogate
   * @return the SPP APID qualifier to use.
   */
  int getQualifier(URI uri);

  /**
   * Returns true if the supplied URI contains a subId
   *
   * @param uri The URI to interrogate
   * @return True if there is a subId.
   */
  boolean hasSubId(URI uri);

  /**
   * Returns the subject id to encode for the supplied URI
   *
   * @param uri The URI to interrogate
   * @return the SPP SubId to use.
   */
  short getSubId(URI uri);

  /**
   * Returns the URI from the APID and the subject id
   *
   * @param qualifier The APID qualifier, null if not needed.
   * @param apid The APID
   * @param subId the subid. NULL if no subId present.
   * @return the URI to use.
   */
  URI getURI(Integer qualifier, short apid, Short subId);
}
