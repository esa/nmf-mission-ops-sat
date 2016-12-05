/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO Fixed Length Binary encoder
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
package esa.mo.mal.encoder.binary.fixed;

import org.ccsds.moims.mo.mal.MALException;

/**
 * Implements the MALDecoder interface for a fixed length binary encoding.
 */
public class FixedBinaryDecoder extends esa.mo.mal.encoder.binary.BinaryDecoder
{
  /**
   * Constructor.
   *
   * @param src Byte array to read from.
   */
  public FixedBinaryDecoder(final byte[] src)
  {
    super(new FixedBufferHolder(null, src, 0, src.length));
  }

  /**
   * Constructor.
   *
   * @param is Input stream to read from.
   */
  public FixedBinaryDecoder(final java.io.InputStream is)
  {
    super(new FixedBufferHolder(is, null, 0, 0));
  }

  /**
   * Constructor.
   *
   * @param src Byte array to read from.
   * @param offset index in array to start reading from.
   */
  public FixedBinaryDecoder(final byte[] src, final int offset)
  {
    super(new FixedBufferHolder(null, src, offset, src.length));
  }

  /**
   * Constructor.
   *
   * @param src Source buffer holder to use.
   */
  protected FixedBinaryDecoder(final BufferHolder src)
  {
    super(src);
  }

  @Override
  public org.ccsds.moims.mo.mal.MALListDecoder createListDecoder(final java.util.List list) throws MALException
  {
    return new FixedBinaryListDecoder(list, sourceBuffer);
  }

  /**
   * Internal class that implements the fixed length field decoding.
   */
  protected static class FixedBufferHolder extends BinaryBufferHolder
  {
    /**
     * Constructor.
     *
     * @param is Input stream to read from.
     * @param buf Source buffer to use.
     * @param offset Buffer offset to read from next.
     * @param length Length of readable data held in the array, which may be larger.
     */
    public FixedBufferHolder(final java.io.InputStream is, final byte[] buf, final int offset, final int length)
    {
      super(is, buf, offset, length);
    }

    @Override
    public long getUnsignedLong() throws MALException
    {
      buf.checkBuffer(8);
      final int i = buf.shiftOffsetAndReturnPrevious(8);
      return java.nio.ByteBuffer.wrap(buf.getBuf(), i, 8).getLong();
    }

    @Override
    public long getUnsignedLong32() throws MALException
    {
      buf.checkBuffer(4);

      final int i = buf.shiftOffsetAndReturnPrevious(4);
      return java.nio.ByteBuffer.wrap(buf.getBuf(), i, 4).getInt() & 0xFFFFFFFFL;
    }

    @Override
    public int getUnsignedInt() throws MALException
    {
      buf.checkBuffer(4);
      final int i = buf.shiftOffsetAndReturnPrevious(4);
      return java.nio.ByteBuffer.wrap(buf.getBuf(), i, 4).getInt();
    }

    @Override
    public int getUnsignedInt16() throws MALException
    {
      buf.checkBuffer(2);
      final int i = buf.shiftOffsetAndReturnPrevious(2);
      return java.nio.ByteBuffer.wrap(buf.getBuf(), i, 2).getShort() & 0xFFFF;
    }

    @Override
    public int getUnsignedShort() throws MALException
    {
      buf.checkBuffer(2);
      final int i = buf.shiftOffsetAndReturnPrevious(2);
      return java.nio.ByteBuffer.wrap(buf.getBuf(), i, 2).getShort();
    }

    @Override
    public short getUnsignedShort8() throws MALException
    {
      return (short) (get8() & 0xFF);
    }

    @Override
    public short getSignedShort() throws MALException
    {
      return (short)getUnsignedShort();
    }

    @Override
    public int getSignedInt() throws MALException
    {
      return getUnsignedInt();
    }

    @Override
    public long getSignedLong() throws MALException
    {
      return getUnsignedLong();
    }
  }
}
