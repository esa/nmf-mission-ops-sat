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

import java.util.List;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListDecoder;

/**
 * The implementation of the MALListDecoder interfaces for the line encoding.
 */
public class LineListDecoder extends LineDecoder implements MALListDecoder
{
  private final int size;
  private final List list;

  /**
   * Constructor.
   *
   * @param list List to decode into.
   * @param inputStream Input stream to read from.
   * @param srcBuffer Buffer to manage.
   * @throws MALException If cannot decode size of list.
   */
  public LineListDecoder(final List list, final java.io.InputStream inputStream, final BufferHolder srcBuffer)
          throws MALException
  {
    super(inputStream, srcBuffer);

    this.list = list;
    size = decodeInteger();
  }

  @Override
  public boolean hasNext()
  {
    return list.size() < size;
  }

  @Override
  public int size()
  {
    return size;
  }
}
