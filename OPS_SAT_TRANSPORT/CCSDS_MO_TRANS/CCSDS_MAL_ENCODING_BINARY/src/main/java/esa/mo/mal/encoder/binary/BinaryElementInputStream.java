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


/**
 * Implements the MALElementInputStream interface for a binary encoding.
 */
public class BinaryElementInputStream extends esa.mo.mal.encoder.gen.GENElementInputStream
{
  /**
   * Constructor.
   *
   * @param is Input stream to read from.
   */
  public BinaryElementInputStream(final java.io.InputStream is)
  {
    super(new BinaryDecoder(is));
  }

  /**
   * Constructor.
   *
   * @param buf Byte buffer to read from.
   * @param offset Offset into buffer to start from.
   */
  public BinaryElementInputStream(final byte[] buf, final int offset)
  {
    super(new BinaryDecoder(buf, offset));
  }

  /**
   * Sub class constructor.
   *
   * @param dec Decoder to use.
   */
  protected BinaryElementInputStream(BinaryDecoder dec)
  {
    super(dec);
  }
}
