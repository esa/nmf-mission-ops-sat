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
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;
import org.ccsds.moims.mo.mal.MALDecoder;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListDecoder;
import org.ccsds.moims.mo.mal.structures.*;

/**
 * The implementation of the MALDecoder interface for the line encoding.
 */
public class LineDecoder implements MALDecoder
{
  static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
  private static final String STR_DELIM = "\n";
  private static final String STR_NULL = "_";
  private static final int BLOCK_SIZE = 65536;
  private final java.io.InputStream inputStream;
  private final BufferHolder sourceBuffer;

  /**
   * Constructor.
   *
   * @param src Source string to read from.
   */
  public LineDecoder(final String src)
  {
    inputStream = null;
    sourceBuffer = new BufferHolder(src, 0);
  }

  /**
   * Constructor.
   *
   * @param is Source stream to read from.
   */
  public LineDecoder(final java.io.InputStream is)
  {
    inputStream = is;
    sourceBuffer = new BufferHolder(null, 0);
  }

  /**
   * Constructor.
   *
   * @param is Source stream to read from.
   * @param src Source buffer holder to use..
   */
  protected LineDecoder(final java.io.InputStream is, final BufferHolder src)
  {
    inputStream = is;
    sourceBuffer = src;
  }

  @Override
  public MALListDecoder createListDecoder(final List list) throws MALException
  {
    return new LineListDecoder(list, inputStream, sourceBuffer);
  }

  @Override
  public Identifier decodeIdentifier() throws MALException
  {
    return new Identifier(removeFirst());
  }

  @Override
  public Identifier decodeNullableIdentifier() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return new Identifier(strVal);
    }

    return null;
  }

  @Override
  public String decodeString() throws MALException
  {
    return removeFirst();
  }

  @Override
  public String decodeNullableString() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return strVal;
    }

    return null;
  }

  @Override
  public Integer decodeInteger() throws MALException
  {
    return internalDecodeInteger(removeFirst());
  }

  @Override
  public Integer decodeNullableInteger() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeInteger(strVal);
    }

    return null;
  }

  private Integer internalDecodeInteger(final String strVal) throws MALException
  {
    try
    {
      return Integer.valueOf(strVal);
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public Boolean decodeBoolean() throws MALException
  {
    return internalDecodeBoolean(removeFirst());
  }

  @Override
  public Boolean decodeNullableBoolean() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeBoolean(strVal);
    }

    return null;
  }

  private Boolean internalDecodeBoolean(final String strVal) throws MALException
  {
    try
    {
      return Boolean.valueOf(strVal);
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public Time decodeTime() throws MALException
  {
    return internalDecodeTime(removeFirst());
  }

  @Override
  public Time decodeNullableTime() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeTime(strVal);
    }

    return null;
  }

  private Time internalDecodeTime(final String strVal) throws MALException
  {
    try
    {
      return new Time(Long.parseLong(strVal));
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public FineTime decodeFineTime() throws MALException
  {
    return internalDecodeFineTime(removeFirst());
  }

  @Override
  public FineTime decodeNullableFineTime() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeFineTime(strVal);
    }

    return null;
  }

  private FineTime internalDecodeFineTime(final String strVal) throws MALException
  {
    try
    {
      return new FineTime(Long.parseLong(strVal));
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public Blob decodeBlob() throws MALException
  {
    return new Blob(hexStringToByteArray(removeFirst()));
  }

  @Override
  public Blob decodeNullableBlob() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return new Blob(hexStringToByteArray(strVal));
    }

    return null;
  }

  @Override
  public Duration decodeDuration() throws MALException
  {
    return internalDecodeDuration(removeFirst());
  }

  @Override
  public Duration decodeNullableDuration() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeDuration(strVal);
    }

    return null;
  }

  private Duration internalDecodeDuration(final String strVal) throws MALException
  {
    try
    {
      return new Duration(Double.parseDouble(strVal));
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public Float decodeFloat() throws MALException
  {
    return internalDecodeFloat(removeFirst());
  }

  @Override
  public Float decodeNullableFloat() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeFloat(strVal);
    }

    return null;
  }

  private Float internalDecodeFloat(final String strVal) throws MALException
  {
    try
    {
      return Float.valueOf(strVal);
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public Double decodeDouble() throws MALException
  {
    return internalDecodeDouble(removeFirst());
  }

  @Override
  public Double decodeNullableDouble() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeDouble(strVal);
    }

    return null;
  }

  private Double internalDecodeDouble(final String strVal) throws MALException
  {
    try
    {
      return Double.valueOf(strVal);
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public Long decodeLong() throws MALException
  {
    return internalDecodeLong(removeFirst());
  }

  @Override
  public Long decodeNullableLong() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeLong(strVal);
    }

    return null;
  }

  private Long internalDecodeLong(final String strVal) throws MALException
  {
    try
    {
      return Long.valueOf(strVal);
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public Byte decodeOctet() throws MALException
  {
    return internalDecodeOctet(removeFirst());
  }

  @Override
  public Byte decodeNullableOctet() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeOctet(strVal);
    }

    return null;
  }

  private Byte internalDecodeOctet(final String strVal) throws MALException
  {
    try
    {
      return Byte.valueOf(strVal);
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public Short decodeShort() throws MALException
  {
    return internalDecodeShort(removeFirst());
  }

  @Override
  public Short decodeNullableShort() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeShort(strVal);
    }

    return null;
  }

  private Short internalDecodeShort(final String strVal) throws MALException
  {
    try
    {
      return Short.valueOf(strVal);
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public UInteger decodeUInteger() throws MALException
  {
    return internalDecodeUInteger(removeFirst());
  }

  @Override
  public UInteger decodeNullableUInteger() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeUInteger(strVal);
    }

    return null;
  }

  private UInteger internalDecodeUInteger(final String strVal) throws MALException
  {
    try
    {
      return new UInteger(Long.parseLong(strVal));
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public ULong decodeULong() throws MALException
  {
    return internalDecodeULong(removeFirst());
  }

  @Override
  public ULong decodeNullableULong() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeULong(strVal);
    }

    return null;
  }

  private ULong internalDecodeULong(final String strVal) throws MALException
  {
    try
    {
      return new ULong(new BigInteger(strVal));
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public UOctet decodeUOctet() throws MALException
  {
    return internalDecodeUOctet(removeFirst());
  }

  @Override
  public UOctet decodeNullableUOctet() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeUOctet(strVal);
    }

    return null;
  }

  private UOctet internalDecodeUOctet(final String strVal) throws MALException
  {
    try
    {
      return new UOctet(Short.parseShort(strVal));
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public UShort decodeUShort() throws MALException
  {
    return internalDecodeUShort(removeFirst());
  }

  @Override
  public UShort decodeNullableUShort() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeUShort(strVal);
    }

    return null;
  }

  private UShort internalDecodeUShort(final String strVal) throws MALException
  {
    try
    {
      return new UShort(Integer.parseInt(strVal));
    }
    catch (NumberFormatException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public URI decodeURI() throws MALException
  {
    return new URI(removeFirst());
  }

  @Override
  public URI decodeNullableURI() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return new URI(strVal);
    }

    return null;
  }

  @Override
  public Attribute decodeAttribute() throws MALException
  {
    return internalDecodeAttribute(removeFirst());
  }

  @Override
  public Attribute decodeNullableAttribute() throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      return internalDecodeAttribute(strVal);
    }

    return null;
  }

  private Attribute internalDecodeAttribute(final String strVal) throws MALException
  {
    final int typeval = internalDecodeOctet(strVal);

    // we should really precheck the area and service parts of the long before splitting out the type part
    // ToDo

    switch (typeval)
    {
      case Attribute._BLOB_TYPE_SHORT_FORM:
        return decodeBlob();
      case Attribute._BOOLEAN_TYPE_SHORT_FORM:
        return new Union(decodeBoolean());
      case Attribute._DURATION_TYPE_SHORT_FORM:
        return decodeDuration();
      case Attribute._FLOAT_TYPE_SHORT_FORM:
        return new Union(decodeFloat());
      case Attribute._DOUBLE_TYPE_SHORT_FORM:
        return new Union(decodeDouble());
      case Attribute._IDENTIFIER_TYPE_SHORT_FORM:
        return decodeIdentifier();
      case Attribute._OCTET_TYPE_SHORT_FORM:
        return new Union(decodeOctet());
      case Attribute._UOCTET_TYPE_SHORT_FORM:
        return decodeUOctet();
      case Attribute._SHORT_TYPE_SHORT_FORM:
        return new Union(decodeShort());
      case Attribute._USHORT_TYPE_SHORT_FORM:
        return decodeUShort();
      case Attribute._INTEGER_TYPE_SHORT_FORM:
        return new Union(decodeInteger());
      case Attribute._UINTEGER_TYPE_SHORT_FORM:
        return decodeUInteger();
      case Attribute._LONG_TYPE_SHORT_FORM:
        return new Union(decodeLong());
      case Attribute._ULONG_TYPE_SHORT_FORM:
        return decodeULong();
      case Attribute._STRING_TYPE_SHORT_FORM:
        return new Union(decodeString());
      case Attribute._TIME_TYPE_SHORT_FORM:
        return decodeTime();
      case Attribute._FINETIME_TYPE_SHORT_FORM:
        return decodeFineTime();
      case Attribute._URI_TYPE_SHORT_FORM:
        return decodeURI();
      default:
        throw new MALException("Unknown attribute type received: " + strVal);
    }
  }

  @Override
  public Element decodeElement(final Element element) throws IllegalArgumentException, MALException
  {
    return element.decode(this);
  }

  @Override
  public Element decodeNullableElement(final Element element) throws MALException
  {
    final String strVal = removeFirst();

    // Check if object is not null...
    if (!strVal.equals(STR_NULL))
    {
      pushBack(strVal);
      return element.decode(this);
    }

    return null;
  }

  private void pushBack(String value)
  {
    sourceBuffer.head = value;
  }

  private String removeFirst() throws MALException
  {
    String rv;

    if (null == sourceBuffer.head)
    {
      final int index = findNextOffset();

      // No more chars
      if (-1 == index)
      {
        rv = sourceBuffer.buf.substring(sourceBuffer.offset, sourceBuffer.buf.length());
        sourceBuffer.offset = sourceBuffer.buf.length();
      }
      else
      {
        rv = sourceBuffer.buf.substring(sourceBuffer.offset, index);
        sourceBuffer.offset = index + 1;
      }

      // trim off initial property set part
      if (null != rv)
      {
        final int eindex = rv.indexOf('=');

        if (-1 != eindex)
        {
          rv = rv.substring(eindex + 1);
        }
      }
    }
    else
    {
      rv = sourceBuffer.head;
      sourceBuffer.head = null;
    }
    
    return rv;
  }

  private int findNextOffset() throws MALException
  {
    preLoadBuffer();
    int index = sourceBuffer.buf.indexOf(STR_DELIM, sourceBuffer.offset);

    // ensure that we have loaded enough buffer from the input stream (if we are stream based) for the next read
    if (-1 == index)
    {
      boolean needMore = true;
      while (needMore)
      {
        final boolean haveMore = loadExtraBuffer();

        index = sourceBuffer.buf.indexOf(STR_DELIM, sourceBuffer.offset);

        needMore = haveMore && (-1 == index);
      }
    }

    return index;
  }

  private void preLoadBuffer() throws MALException
  {
    if ((null != inputStream) && (null == sourceBuffer.buf))
    {
      // need to load in some
      final byte[] tbuf = new byte[BLOCK_SIZE];

      try
      {
        final int length = inputStream.read(tbuf, 0, tbuf.length);
        sourceBuffer.buf = new String(tbuf, 0, length, UTF8_CHARSET);
        sourceBuffer.offset = 0;
      }
      catch (IOException ex)
      {
        throw new MALException("Unable to read required amount from source stream", ex);
      }
    }
  }

  private boolean loadExtraBuffer() throws MALException
  {
    boolean moreAvailable = false;

    if (null != inputStream)
    {
      // need to load in some
      final byte[] tbuf = new byte[BLOCK_SIZE];

      try
      {
        final int length = inputStream.read(tbuf, 0, tbuf.length);
        sourceBuffer.buf += new String(tbuf, 0, length, UTF8_CHARSET);
        moreAvailable = 0 != inputStream.available();
      }
      catch (IOException ex)
      {
        throw new MALException("Unable to read required amount from source stream", ex);
      }
    }

    return moreAvailable;
  }

  private static byte[] hexStringToByteArray(final String s)
  {
    final int len = s.length();
    final byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2)
    {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  /**
   *
   */
  protected static class BufferHolder
  {
    private String buf;
    private int offset;
    private String head = null;

    /**
     *
     * @param buf
     * @param offset
     */
    public BufferHolder(final String buf, final int offset)
    {
      this.buf = buf;
      this.offset = offset;
    }
  }
}
