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

import esa.mo.mal.encoder.spp.SPPTimeHandler.TimeInputStream;
import java.math.BigInteger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.FineTime;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.ULong;
import org.ccsds.moims.mo.mal.structures.URI;

/**
 * Implements the MALDecoder interface for a SPP binary encoding.
 */
public class SPPVarBinaryDecoder extends esa.mo.mal.encoder.binary.BinaryDecoder
{
  private final SPPTimeHandler timeHandler;
  private final SPPTimeInputStream timeInputStream = new SPPTimeInputStream();

  /**
   * Constructor.
   *
   * @param src Byte array to read from.
   */
  public SPPVarBinaryDecoder(final byte[] src, final SPPTimeHandler timeHandler)
  {
    super(new SPPVarBufferHolder(null, src, 0, src.length));

    this.timeHandler = timeHandler;
  }

  /**
   * Constructor.
   *
   * @param is Input stream to read from.
   */
  public SPPVarBinaryDecoder(final java.io.InputStream is, final SPPTimeHandler timeHandler)
  {
    super(new SPPVarBufferHolder(is, null, 0, 0));

    this.timeHandler = timeHandler;
  }

  /**
   * Constructor.
   *
   * @param src Byte array to read from.
   * @param offset index in array to start reading from.
   */
  public SPPVarBinaryDecoder(final byte[] src, final int offset, final SPPTimeHandler timeHandler)
  {
    super(new SPPVarBufferHolder(null, src, offset, src.length));

    this.timeHandler = timeHandler;
  }

  /**
   * Constructor.
   *
   */
  public SPPVarBinaryDecoder(SPPFixedBinaryDecoder fixedDecoder)
  {
    super(new SPPVarBufferHolder((BinaryBufferHolder) fixedDecoder.getSourceBuffer()));

    this.timeHandler = fixedDecoder.getTimeHandler();
  }

  /**
   * Constructor.
   *
   * @param src Source buffer holder to use.
   */
  protected SPPVarBinaryDecoder(final BufferHolder src, final SPPTimeHandler timeHandler)
  {
    super(src);

    this.timeHandler = timeHandler;
  }

  @Override
  public org.ccsds.moims.mo.mal.MALListDecoder createListDecoder(final java.util.List list) throws MALException
  {
    return new SPPVarBinaryListDecoder(list, sourceBuffer, timeHandler);
  }

  @Override
  public Boolean decodeNullableBoolean() throws MALException
  {
    if (sourceBuffer.isNotNull())
    {
      return decodeBoolean();
    }

    return null;
  }

  @Override
  public Blob decodeNullableBlob() throws MALException
  {
    if (sourceBuffer.isNotNull())
    {
      return decodeBlob();
    }

    return null;
  }

  @Override
  public ULong decodeULong() throws MALException
  {
    byte[] buf =
    {
      0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    System.arraycopy(sourceBuffer.directGetBytes(8), 0, buf, 1, 8);

    return new ULong(new BigInteger(buf));
  }

  @Override
  public ULong decodeNullableULong() throws MALException
  {
    if (sourceBuffer.getBool())
    {
      return decodeULong();
    }

    return null;
  }

  @Override
  public Time decodeTime() throws MALException
  {
    return timeHandler.decodeTime(timeInputStream);
  }

  @Override
  public FineTime decodeFineTime() throws MALException
  {
    return timeHandler.decodeFineTime(timeInputStream);
  }

  @Override
  public Duration decodeDuration() throws MALException
  {
    return timeHandler.decodeDuration(timeInputStream);
  }

  @Override
  public String decodeNullableString() throws MALException
  {
    if (sourceBuffer.isNotNull())
    {
      return decodeString();
    }

    return null;
  }

  @Override
  public URI decodeNullableURI() throws MALException
  {
    if (sourceBuffer.isNotNull())
    {
      return decodeURI();
    }

    return null;
  }

  @Override
  public Identifier decodeNullableIdentifier() throws MALException
  {
    if (sourceBuffer.isNotNull())
    {
      return decodeIdentifier();
    }

    return null;
  }

  @Override
  public Long decodeAbstractElementType(boolean withNull) throws MALException
  {
    if (!withNull || sourceBuffer.isNotNull())
    {
      return java.nio.ByteBuffer.wrap(sourceBuffer.directGetBytes(8)).getLong();
    }

    return null;
  }

  @Override
  public int internalDecodeAttributeType(byte value) throws MALException
  {
    return value + 1;
  }

  /**
   * Extends the binary buffer holder to wrap an existing buffer holder and use their existing buffer.
   */
  public static class SPPVarBufferHolder extends BinaryBufferHolder
  {
    /**
     * Constructor.
     *
     * @param is Input stream to read from.
     * @param buf Source buffer to use.
     * @param offset Buffer offset to read from next.
     * @param length Length of readable data held in the array, which may be larger.
     */
    public SPPVarBufferHolder(final java.io.InputStream is, final byte[] buf, final int offset, final int length)
    {
      super(is, buf, offset, length);
    }

    /**
     * Constructor.
     *
     * @param buffer Source buffer to use.
     */
    public SPPVarBufferHolder(final BinaryBufferHolder buffer)
    {
      super(buffer.getBuf());
    }

    @Override
    public String getString() throws MALException
    {
      final int len = getUnsignedInt();

      if (len >= 0)
      {
        buf.checkBuffer(len);

        final String s = new String(buf.getBuf(), buf.getOffset(), len, UTF8_CHARSET);
        buf.shiftOffsetAndReturnPrevious(len);
        return s;
      }
      return null;
    }

    @Override
    public float getFloat() throws MALException
    {
      return Float.intBitsToFloat(java.nio.ByteBuffer.wrap(directGetBytes(4)).getInt());
    }

    @Override
    public double getDouble() throws MALException
    {
      return Double.longBitsToDouble(java.nio.ByteBuffer.wrap(directGetBytes(8)).getLong());
    }

    @Override
    public BigInteger getBigInteger() throws MALException
    {
      BigInteger value = BigInteger.ZERO;
      int i = 0;
      byte b;
      while (((b = get8()) & 0x80) != 0)
      {
        value = value.or(BigInteger.valueOf(b & 0x7f).shiftLeft(i));
        i += 7;
      }

      return value.or(BigInteger.valueOf(b).shiftLeft(i));
    }

    @Override
    public short getUnsignedShort8() throws MALException
    {
      return (short) (get8() & 0xFF);
    }

    @Override
    public byte[] getBytes() throws MALException
    {
      return directGetBytes(getUnsignedInt());
    }
  }

  private final class SPPTimeInputStream implements TimeInputStream
  {
    public byte[] directGetBytes(int length) throws MALException
    {
      return sourceBuffer.directGetBytes(length);
    }
  }
}
