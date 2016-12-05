/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO String encoder
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
package esa.mo.mal.encoder.string;

import esa.mo.mal.encoder.gen.GENElementOutputStream;
import esa.mo.mal.encoder.gen.GENEncoder;
import java.io.OutputStream;

/**
 * Implements the MALElementOutputStream interface for String encodings.
 */
public class StringElementOutputStream extends GENElementOutputStream
{
  /**
   * Constructor.
   *
   * @param os Output stream to write to.
   */
  public StringElementOutputStream(final OutputStream os)
  {
    super(os);
  }

  @Override
  protected GENEncoder createEncoder(OutputStream os)
  {
    return new StringEncoder(os);
  }
}
