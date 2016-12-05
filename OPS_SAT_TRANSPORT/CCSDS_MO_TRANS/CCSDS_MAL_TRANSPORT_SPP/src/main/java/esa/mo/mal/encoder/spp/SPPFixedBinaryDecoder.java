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
public class SPPFixedBinaryDecoder extends esa.mo.mal.encoder.binary.fixed.FixedBinaryDecoder
{
  private final boolean smallLengthField;
  private final SPPTimeHandler timeHandler;
  private final SPPTimeInputStream timeInputStream = new SPPTimeInputStream();

  /**
   * Constructor.
   *
   * @param src Byte array to read from.
   * @param smallLengthField True if length field is 16bits, otherwise assumed to be 32bits.
   */
  public SPPFixedBinaryDecoder(final byte[] src, final boolean smallLengthField,
          final SPPTimeHandler timeHandler)
  {
    super(new SPPBufferHolder(null, src, 0, src.length, smallLengthField));

    this.smallLengthField = smallLengthField;
    this.timeHandler = timeHandler;
  }

  /**
   * Constructor.
   *
   * @param is Input stream to read from.
   * @param smallLengthField True if length field is 16bits, otherwise assumed to be 32bits.
   */
  public SPPFixedBinaryDecoder(final java.io.InputStream is, final boolean smallLengthField,
          final SPPTimeHandler timeHandler)
  {
    super(new SPPBufferHolder(is, null, 0, 0, smallLengthField));

    this.smallLengthField = smallLengthField;
    this.timeHandler = timeHandler;
  }

  /**
   * Constructor.
   *
   * @param src Byte array to read from.
   * @param offset index in array to start reading from.
   * @param smallLengthField True if length field is 16bits, otherwise assumed to be 32bits.
   */
  public SPPFixedBinaryDecoder(final byte[] src, final int offset, final boolean smallLengthField,
          final SPPTimeHandler timeHandler)
  {
    super(new SPPBufferHolder(null, src, offset, src.length, smallLengthField));

    this.smallLengthField = smallLengthField;
    this.timeHandler = timeHandler;
  }

  /**
   * Constructor.
   *
   * @param src Source buffer holder to use.
   * @param smallLengthField True if length field is 16bits, otherwise assumed to be 32bits.
   */
  protected SPPFixedBinaryDecoder(final BufferHolder src, final boolean smallLengthField,
          final SPPTimeHandler timeHandler)
  {
    super(src);

    this.smallLengthField = smallLengthField;
    this.timeHandler = timeHandler;
  }

  @Override
  public org.ccsds.moims.mo.mal.MALListDecoder createListDecoder(final java.util.List list) throws MALException
  {
    return new SPPFixedBinaryListDecoder(list, sourceBuffer, smallLengthField, timeHandler);
  }

  @Override
  public Boolean decodeNullableBoolean() throws MALException
  {
    if (sourceBuffer.getBool())
    {
      return decodeBoolean();
    }

    return null;
  }

  @Override
  public Blob decodeBlob() throws MALException
  {
    if (smallLengthField)
    {
      return new Blob(sourceBuffer.directGetBytes(sourceBuffer.getSignedShort()));
    }

    return super.decodeBlob();
  }

  @Override
  public Blob decodeNullableBlob() throws MALException
  {
    if (sourceBuffer.getBool())
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
  public Time decodeNullableTime() throws MALException
  {
    if (sourceBuffer.getBool())
    {
      return decodeTime();
    }

    return null;
  }

  @Override
  public FineTime decodeFineTime() throws MALException
  {
    return timeHandler.decodeFineTime(timeInputStream);
  }

  @Override
  public FineTime decodeNullableFineTime() throws MALException
  {
    if (sourceBuffer.getBool())
    {
      return decodeFineTime();
    }

    return null;
  }

  @Override
  public Duration decodeDuration() throws MALException
  {
    return timeHandler.decodeDuration(timeInputStream);
  }

  @Override
  public Duration decodeNullableDuration() throws MALException
  {
    if (sourceBuffer.getBool())
    {
      return decodeDuration();
    }

    return null;
  }

  @Override
  public String decodeNullableString() throws MALException
  {
    if (sourceBuffer.getBool())
    {
      return decodeString();
    }

    return null;
  }

  @Override
  public URI decodeNullableURI() throws MALException
  {
    if (sourceBuffer.getBool())
    {
      return decodeURI();
    }

    return null;
  }

  @Override
  public Identifier decodeNullableIdentifier() throws MALException
  {
    if (sourceBuffer.getBool())
    {
      return decodeIdentifier();
    }

    return null;
  }

  @Override
  public int internalDecodeAttributeType(byte value) throws MALException
  {
    return value + 1;
  }

  public BufferHolder getSourceBuffer()
  {
    return sourceBuffer;
  }

  public SPPTimeHandler getTimeHandler()
  {
    return timeHandler;
  }

  /**
   * Extends the fixed length internal buffer holder to cope with the smaller size of the size field for Strings in SPP
   * packets.
   */
  protected static class SPPBufferHolder extends FixedBufferHolder
  {
    private final boolean smallLengthField;

    /**
     * Constructor.
     *
     * @param is Input stream to read from.
     * @param buf Source buffer to use.
     * @param offset Buffer offset to read from next.
     * @param length Length of readable data held in the array, which may be larger.
     * @param smallLengthField True if length field is 16bits, otherwise assumed to be 32bits.
     */
    public SPPBufferHolder(final java.io.InputStream is,
            final byte[] buf,
            final int offset,
            final int length,
            final boolean smallLengthField)
    {
      super(is, buf, offset, length);

      this.smallLengthField = smallLengthField;
    }

    @Override
    public String getString() throws MALException
    {
      if (smallLengthField)
      {
        final int len = getSignedShort();

        if (len >= 0)
        {
          buf.checkBuffer(len);

          final String s = new String(buf.getBuf(), buf.getOffset(), len, UTF8_CHARSET);
          buf.shiftOffsetAndReturnPrevious(len);
          return s;
        }

        return null;
      }
      else
      {
        return super.getString();
      }
    }
  }
  
  private final class SPPTimeInputStream implements SPPTimeHandler.TimeInputStream
  {
    public byte[] directGetBytes(int length) throws MALException
    {
      return sourceBuffer.directGetBytes(length);
    }
  }
}
