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

import esa.mo.mal.encoder.binary.BinaryEncoder.BinaryStreamHolder;
import esa.mo.mal.encoder.gen.GENEncoder;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListEncoder;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.FineTime;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.ULong;
import org.ccsds.moims.mo.mal.structures.URI;

/**
 * Implements the MALEncoder and MALListEncoder interfaces for a SPP binary encoding.
 */
public class SPPVarBinaryEncoder extends GENEncoder
{
  private final SPPTimeHandler timeHandler;
  private final SPPTimeOutputStream timeOutputStream = new SPPTimeOutputStream();

  /**
   * Constructor.
   *
   * @param os Output stream to write to.
   */
  public SPPVarBinaryEncoder(final OutputStream os, final SPPTimeHandler timeHandler)
  {
    super(new SPPVarStreamHolder(os));

    this.timeHandler = timeHandler;
  }

  @Override
  public MALListEncoder createListEncoder(List value) throws MALException
  {
    try
    {
      outputStream.addUnsignedInt((short) value.size());

      return this;
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public void encodeNullableBoolean(final Boolean value) throws MALException
  {
    try
    {
      if (null != value)
      {
        outputStream.addNotNull();
        encodeBoolean(value);
      }
      else
      {
        outputStream.addIsNull();
      }
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public void encodeNullableULong(final ULong value) throws MALException
  {
    try
    {
      if (null != value)
      {
        outputStream.addNotNull();
        encodeULong(value);
      }
      else
      {
        outputStream.addIsNull();
      }
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public void encodeDuration(Duration value) throws MALException
  {
    try
    {
      timeHandler.encodeDuration(timeOutputStream, value);
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public void encodeFineTime(FineTime value) throws MALException
  {
    try
    {
      timeHandler.encodeFineTime(timeOutputStream, value);
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public void encodeTime(Time value) throws MALException
  {
    try
    {
      timeHandler.encodeTime(timeOutputStream, value);
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public void encodeNullableBlob(Blob value) throws MALException
  {
    try
    {
      if ((null != value)
              && ((value.isURLBased() && (null != value.getURL()))
              || (!value.isURLBased() && (null != value.getValue()))))
      {
        outputStream.addNotNull();
        encodeBlob(value);
      }
      else
      {
        outputStream.addIsNull();
      }
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public void encodeNullableString(String value) throws MALException
  {
    try
    {
      if (null != value)
      {
        outputStream.addNotNull();
        encodeString(value);
      }
      else
      {
        outputStream.addIsNull();
      }
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public void encodeNullableIdentifier(Identifier value) throws MALException
  {
    try
    {
      if (null != value)
      {
        outputStream.addNotNull();
        encodeIdentifier(value);
      }
      else
      {
        outputStream.addIsNull();
      }
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public void encodeNullableURI(URI value) throws MALException
  {
    try
    {
      if (null != value)
      {
        outputStream.addNotNull();
        encodeURI(value);
      }
      else
      {
        outputStream.addIsNull();
      }
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public void encodeAbstractElementType(Long value, boolean withNull) throws MALException
  {
    try
    {
      if (withNull)
      {
        if (null != value)
        {
          outputStream.addNotNull();
          outputStream.directAdd(java.nio.ByteBuffer.allocate(8).putLong(value).array());
        }
        else
        {
          outputStream.addIsNull();
        }
      }
      else
      {
        outputStream.directAdd(java.nio.ByteBuffer.allocate(8).putLong(value).array());
      }
    }
    catch (IOException ex)
    {
      throw new MALException(ENCODING_EXCEPTION_STR, ex);
    }
  }

  @Override
  public byte internalEncodeAttributeType(byte value) throws MALException
  {
    return (byte) (value - 1);
  }

  /**
   * Extends the FixedStreamHolder class for handling SPP fields.
   */
  protected static class SPPVarStreamHolder extends BinaryStreamHolder
  {
    static final BigInteger ULONG_MASK_NOT_7F = BigInteger.valueOf(0x7f).not();
    static final BigInteger ULONG_MASK_7F = BigInteger.valueOf(0x7f);
    static final BigInteger ULONG_MASK_80 = BigInteger.valueOf(0x80);

    /**
     * Constructor.
     *
     * @param outputStream The output stream to encode into.
     */
    public SPPVarStreamHolder(OutputStream outputStream)
    {
      super(outputStream);
    }

    @Override
    public void addFloat(float value) throws IOException
    {
      directAdd(java.nio.ByteBuffer.allocate(4).putInt(Float.floatToRawIntBits(value)).array());
    }

    @Override
    public void addDouble(double value) throws IOException
    {
      directAdd(java.nio.ByteBuffer.allocate(8).putLong(Double.doubleToRawLongBits(value)).array());
    }

    @Override
    public void addBigInteger(BigInteger value) throws IOException
    {
      while (!(value.and(ULONG_MASK_NOT_7F)).equals(BigInteger.ZERO))
      {
        directAdd(value.and(ULONG_MASK_7F).or(ULONG_MASK_80).byteValue());
        value = value.shiftRight(7);
      }
      directAdd(value.byteValue());
    }

    @Override
    public void addUnsignedShort8(short value) throws IOException
    {
      directAdd((byte) value);
    }

    @Override
    public void addBytes(final byte[] value) throws IOException
    {
      addUnsignedInt(value.length);
      directAdd(value);
    }
  }

  private final class SPPTimeOutputStream implements SPPTimeHandler.TimeOutputStream
  {
    public void directAdd(byte[] value, int os, int ln) throws IOException
    {
      outputStream.directAdd(value, os, ln);
    }

    public void directAdd(byte value) throws IOException
    {
      outputStream.directAdd(value);
    }
  }
}
