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

import esa.mo.mal.encoder.gen.GENEncoder;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigInteger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.structures.*;

/**
 * The implementation of the MALEncoder and MALListEncoder interfaces for the String encoding.
 */
public class StringEncoder extends GENEncoder
{
  public static final String STR_DELIM = "|";
  public static final String STR_NULL = "_";
  public static final String STR_ESC = "\\";
  public static final String STR_DELIM_ESC = STR_ESC + STR_DELIM;
  public static final String STR_NULL_ESC = STR_ESC + STR_NULL;
  public static final String STR_ESC_ESC = STR_ESC + STR_ESC;
  public static final int HEX_MASK = 0xFF;

  /**
   * Constructor.
   *
   * @param buffer The output stream to write to.
   */
  public StringEncoder(OutputStream buffer)
  {
    super(new StringStreamHolder(buffer));
  }

  @Override
  public void encodeNullableElement(final Element value) throws MALException
  {
    try
    {
      if (null != value)
      {
        // Initial delim to represent not-null
        outputStream.addString("");
        value.encode(this);
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

  /**
   * Internal class for accessing the string stream. Overridden by sub-classes to alter the low level encoding.
   */
  public static class StringStreamHolder extends StreamHolder
  {
    private final Writer buffer;

    /**
     * Constructor.
     *
     * @param outputStream the stream to encode in to.
     */
    public StringStreamHolder(OutputStream outputStream)
    {
      super(outputStream);
      this.buffer = new PrintWriter(outputStream, false);
    }

    @Override
    public void addBytes(byte[] value) throws IOException
    {
      add(byteArrayToHexString(value));
    }

    @Override
    public void addString(String value) throws IOException
    {
      add(value);
    }

    @Override
    public void addFloat(float value) throws IOException
    {
      add(Float.toString(value));
    }

    @Override
    public void addDouble(double value) throws IOException
    {
      add(Double.toString(value));
    }

    @Override
    public void addBigInteger(BigInteger value) throws IOException
    {
      add(value.toString());
    }

    @Override
    public void addSignedLong(long value) throws IOException
    {
      add(Long.toString(value));
    }

    @Override
    public void addSignedInt(int value) throws IOException
    {
      add(Integer.toString(value));
    }

    @Override
    public void addSignedShort(short value) throws IOException
    {
      add(Short.toString(value));
    }

    @Override
    public void addUnsignedLong(long value) throws IOException
    {
      add(Long.toString(value));
    }

    @Override
    public void addUnsignedLong32(long value) throws IOException
    {
      add(Long.toString(value));
    }

    @Override
    public void addUnsignedInt(int value) throws IOException
    {
      add(Integer.toString(value));
    }

    @Override
    public void addUnsignedInt16(int value) throws IOException
    {
      add(Integer.toString(value));
    }

    @Override
    public void addUnsignedShort(int value) throws IOException
    {
      add(Integer.toString(value));
    }

    @Override
    public void addUnsignedShort8(short value) throws IOException
    {
      add(Short.toString(value));
    }

    @Override
    public void addByte(byte value) throws IOException
    {
      add(Byte.toString(value));
    }

    @Override
    public void addBool(boolean value) throws IOException
    {
      add(Boolean.toString(value));
    }

    @Override
    public void addNotNull() throws IOException
    {
      // do nothing
    }

    @Override
    public void addIsNull() throws IOException
    {
      buffer.append(STR_NULL);
      buffer.append(STR_DELIM);
    }

    @Override
    public void close() throws IOException
    {
      buffer.flush();
      
      super.close();
    }

    private void add(final String val) throws IOException
    {
      buffer.append(val.replace(STR_ESC, STR_ESC_ESC).replace(STR_NULL, STR_NULL_ESC).replace(STR_DELIM, STR_DELIM_ESC));
      buffer.append(STR_DELIM);
    }

    private static String byteArrayToHexString(final byte[] data)
    {
      final StringBuilder hexString = new StringBuilder();
      for (int i = 0; i < data.length; i++)
      {
        final String hex = Integer.toHexString(HEX_MASK & data[i]);
        if (hex.length() == 1)
        {
          hexString.append('0');
        }
        hexString.append(hex);
      }

      return hexString.toString();
    }
  }
}
