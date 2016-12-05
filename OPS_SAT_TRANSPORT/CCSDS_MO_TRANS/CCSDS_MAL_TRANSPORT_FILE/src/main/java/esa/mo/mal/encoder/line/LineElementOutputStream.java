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

import java.io.IOException;
import java.io.OutputStream;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Element;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;

/**
 * Implements the MALElementOutputStream interface for the line encodings.
 */
public class LineElementOutputStream implements MALElementOutputStream
{
  private final OutputStream dos;

  /**
   * Constructor.
   *
   * @param os Output stream to write to.
   */
  public LineElementOutputStream(final OutputStream os)
  {
    this.dos = os;
  }

  @Override
  public void writeElement(final Object element, final MALEncodingContext ctx) throws MALException
  {
    final LineEncoder enc = new LineEncoder();

    if (element instanceof MALMessageHeader)
    {
      enc.encodeTopLevelElement("Header", (Element) element);
    }
    else
    {
      enc.encodeTopLevelElement("Body", (Element) element);
    }

    try
    {
      dos.write(enc.toString().getBytes(LineDecoder.UTF8_CHARSET));
    }
    catch (Exception ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public void flush() throws MALException
  {
    try
    {
      dos.flush();
    }
    catch (IOException ex)
    {
      throw new MALException("IO exception flushing Element stream", ex);
    }
  }

  @Override
  public void close() throws MALException
  {
    try
    {
      dos.close();
    }
    catch (IOException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }
}
