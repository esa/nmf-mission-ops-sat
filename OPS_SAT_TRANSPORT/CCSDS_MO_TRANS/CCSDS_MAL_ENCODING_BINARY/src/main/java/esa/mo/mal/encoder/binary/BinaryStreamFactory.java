/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO Binary encoder
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
package esa.mo.mal.encoder.binary;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Blob;

/**
 * Implements the MALElementStreamFactory interface for a binary encoding.
 */
public class BinaryStreamFactory extends MALElementStreamFactory
{
  @Override
  protected void init(final String protocol, final Map properties) throws IllegalArgumentException, MALException
  {
    // nothing required here
  }

  @Override
  public MALElementInputStream createInputStream(final byte[] bytes, final int offset)
  {
    return new BinaryElementInputStream(bytes, offset);
  }

  @Override
  public MALElementInputStream createInputStream(final InputStream is) throws MALException
  {
    return new BinaryElementInputStream(is);
  }

  @Override
  public MALElementOutputStream createOutputStream(final OutputStream os) throws MALException
  {
    return new BinaryElementOutputStream(os);
  }

  @Override
  public Blob encode(final Object[] elements, final MALEncodingContext ctx) throws MALException
  {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    final MALElementOutputStream os = createOutputStream(baos);

    for (int i = 0; i < elements.length; i++)
    {
      os.writeElement(elements[i], ctx);
    }

    os.flush();

    return new Blob(baos.toByteArray());
  }
}
