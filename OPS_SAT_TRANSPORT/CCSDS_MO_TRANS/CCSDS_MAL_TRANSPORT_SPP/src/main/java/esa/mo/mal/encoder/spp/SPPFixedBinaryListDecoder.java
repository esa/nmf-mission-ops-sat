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
package esa.mo.mal.encoder.spp;

/**
 * Implements the MALListDecoder interface for a SPP binary encoding.
 */
public class SPPFixedBinaryListDecoder extends SPPFixedBinaryDecoder implements org.ccsds.moims.mo.mal.MALListDecoder
{
  private final int size;
  private final java.util.List list;

  /**
   * Constructor.
   *
   * @param list List to decode into.
   * @param srcBuffer Buffer to manage.
   * @param smallLengthField True if length field is 16bits, otherwise assumed to be 32bits.
   * @throws org.ccsds.moims.mo.mal.MALException If cannot decode list size.
   */
  public SPPFixedBinaryListDecoder(final java.util.List list, final BufferHolder srcBuffer, final boolean smallLengthField,
          final SPPTimeHandler timeHandler) throws org.ccsds.moims.mo.mal.MALException
  {
    super(srcBuffer, smallLengthField, timeHandler);

    this.list = list;

    if (smallLengthField)
    {
      size = srcBuffer.getUnsignedShort();
    }
    else
    {
      size = srcBuffer.getUnsignedInt();
    }
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
