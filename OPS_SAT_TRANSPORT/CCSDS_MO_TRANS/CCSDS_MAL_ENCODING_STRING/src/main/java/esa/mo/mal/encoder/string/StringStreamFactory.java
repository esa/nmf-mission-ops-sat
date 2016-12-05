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

import java.io.*;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Blob;

/**
 * Implementation of the MALElementStreamFactory interface for the String encoding.
 */
public class StringStreamFactory extends MALElementStreamFactory
{
  @Override
  protected void init(final String protocol, final Map properties) throws IllegalArgumentException, MALException
  {
  }

  @Override
  public MALElementInputStream createInputStream(final byte[] bytes, final int offset)
  {
    return new StringElementInputStream(new ByteArrayInputStream(bytes, offset, bytes.length - offset));
  }

  @Override
  public MALElementInputStream createInputStream(final InputStream is) throws MALException
  {
    return new StringElementInputStream(is);
  }

  @Override
  public MALElementOutputStream createOutputStream(final OutputStream os) throws MALException
  {
    return new StringElementOutputStream(os);
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
