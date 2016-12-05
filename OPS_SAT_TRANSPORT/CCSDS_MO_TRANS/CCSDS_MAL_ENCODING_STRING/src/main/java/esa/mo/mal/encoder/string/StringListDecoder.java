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

import java.util.List;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListDecoder;

/**
 * The implementation of the MALListDecoder interfaces for the String encoding.
 */
public class StringListDecoder extends StringDecoder implements MALListDecoder
{
  private final int size;
  private final List list;

  /**
   * Constructor.
   *
   * @param list List to decode into.
   * @param srcBuffer Buffer to manage.
   * @throws MALException If cannot decode size of list.
   */
  public StringListDecoder(final List list, final BufferHolder srcBuffer)
          throws MALException
  {
    super(srcBuffer);

    this.list = list;
    size = sourceBuffer.getSignedInt();
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
