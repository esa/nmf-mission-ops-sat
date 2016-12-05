/* ----------------------------------------------------------------------------
 * Copyright (C) 2014      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO Line encoder framework
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
package esa.mo.mal.encoder.line;

import java.io.InputStream;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Element;

/**
 * Implements the MALElementInputStream interface for the line encodings.
 */
public class LineElementInputStream implements MALElementInputStream
{
  private final LineDecoder dec;

  /**
   * Constructor.
   * @param is Input stream to read from.
   */
  public LineElementInputStream(final InputStream is)
  {
    dec = new LineDecoder(is);
  }

  @Override
  public Object readElement(final Object element, final MALEncodingContext ctx)
          throws IllegalArgumentException, MALException
  {
    return dec.decodeNullableElement((Element) element);
  }

  @Override
  public void close() throws MALException
  {
    // nothing to do here
  }
}
