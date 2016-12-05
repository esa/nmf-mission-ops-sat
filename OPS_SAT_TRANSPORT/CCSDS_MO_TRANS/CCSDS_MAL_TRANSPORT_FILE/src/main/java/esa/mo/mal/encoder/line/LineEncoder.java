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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.structures.*;

/**
 * The implementation of the MALEncoder and MALListEncoder interfaces for the line encoding.
 */
public class LineEncoder
{
  private static final String STR_DELIM = "\n";
  private static final String STR_NULL = "_";
  private static final int HEX_MASK = 0xFF;
  private final StringBuilder buffer = new StringBuilder();
  private final Deque<String> nameStack = new LinkedList();

  /**
   * Encode the top level element.
   *
   * @param name field name
   * @param value field value
   * @throws MALException on error.
   */
  public void encodeTopLevelElement(final String name, final Element value) throws MALException
  {
    encodeField(name, value.getClass(), value);
  }

  /**
   * Encodes a specific field.
   *
   * @param name Field name
   * @param declaredType Declare field type
   * @param value field value
   * @throws MALException on error.
   */
  public void encodeField(final String name, final Class declaredType, final Object value) throws MALException
  {
    if (null != value)
    {
      boolean isAbstract = true;

      if (declaredType == value.getClass())
      {
        isAbstract = false;
      }

      internalEncodeField(isAbstract, name, declaredType, value);
    }
    else
    {
      add(name, STR_NULL);
    }
  }

  /**
   * Encodes a specific field.
   *
   * @param isAbstract Is Field abstract
   * @param name Field name
   * @param declaredType Declare field type
   * @param value field value
s   * @throws MALException on error.
   */
  public void internalEncodeField(final boolean isAbstract, final String name, final Class declaredType, final Object value) throws MALException
  {
    if (null != value)
    {
      if (value instanceof List)
      {
        encodeList(isAbstract, name, (List) value);
      }
      else if (value instanceof Enumeration)
      {
        encodeEnumeration(isAbstract, name, (Enumeration) value);
      }
      else if (value instanceof Composite)
      {
        encodeComposite(isAbstract, name, value.getClass(), (Composite) value);
      }
      else if (value instanceof Attribute)
      {
        encodeAttribute(isAbstract, name, (Attribute) value);
      }
      else if (value instanceof Boolean)
      {
        encodeBoolean(name, (Boolean) value);
      }
      else if (value instanceof Double)
      {
        encodeDouble(name, (Double) value);
      }
      else if (value instanceof Float)
      {
        encodeFloat(name, (Float) value);
      }
      else if (value instanceof Integer)
      {
        encodeInteger(name, (Integer) value);
      }
      else if (value instanceof Long)
      {
        encodeLong(name, (Long) value);
      }
      else if (value instanceof Byte)
      {
        encodeOctet(name, (Byte) value);
      }
      else if (value instanceof Short)
      {
        encodeShort(name, (Short) value);
      }
      else if (value instanceof String)
      {
        encodeString(name, (String) value);
      }
    }
    else
    {
      add(name, STR_NULL);
    }
  }

  @Override
  public String toString()
  {
    return buffer.toString();
  }

  private void encodeDouble(final String name, final Double value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = value.toString();
    }
    add(name, strVal);
  }

  private void encodeLong(final String name, final Long value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = value.toString();
    }
    add(name, strVal);
  }

  private void encodeOctet(final String name, final Byte value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = value.toString();
    }
    add(name, strVal);
  }

  private void encodeShort(final String name, final Short value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = value.toString();
    }
    add(name, strVal);
  }

  private void encodeUInteger(final String name, final UInteger value) throws IllegalArgumentException, MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = Long.toString(value.getValue());
    }
    add(name, strVal);
  }

  private void encodeULong(final String name, final ULong value) throws IllegalArgumentException, MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = value.getValue().toString();
    }
    add(name, strVal);
  }

  private void encodeUOctet(final String name, final UOctet value) throws IllegalArgumentException, MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = Short.toString(value.getValue());
    }
    add(name, strVal);
  }

  private void encodeUShort(final String name, final UShort value) throws IllegalArgumentException, MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = Integer.toString(value.getValue());
    }
    add(name, strVal);
  }

  private void encodeURI(final String name, final URI value) throws MALException
  {
    String strVal = STR_NULL;
    if ((null != value) && (null != value.getValue()))
    {
      strVal = value.getValue();
    }
    add(name, strVal);
  }

  private void encodeIdentifier(final String name, final Identifier value) throws MALException
  {
    String strVal = STR_NULL;
    if ((null != value) && (null != value.getValue()))
    {
      strVal = value.getValue();
    }
    add(name, strVal);
  }

  private void encodeString(final String name, final String value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = value;
    }
    add(name, strVal);
  }

  private void encodeInteger(final String name, final Integer value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = value.toString();
    }
    add(name, strVal);
  }

  private void encodeBoolean(final String name, final Boolean value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = value.toString();
    }
    add(name, strVal);
  }

  private void encodeTime(final String name, final Time value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = Long.toString(value.getValue());
    }
    add(name, strVal);
  }

  private void encodeFineTime(final String name, final FineTime value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = Long.toString(value.getValue());
    }
    add(name, strVal);
  }

  private void encodeBlob(final String name, final Blob value) throws MALException
  {
    //should encode to 64 bit char string
    if ((null == value)
            || (value.isURLBased() && (null == value.getURL()))
            || (!value.isURLBased() && (null == value.getValue())))
    {
      add(name, STR_NULL);
    }
    else
    {
      add(name, byteArrayToHexString(value.getValue()));
    }
  }

  private void encodeDuration(final String name, final Duration value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = Double.toString(value.getValue());
    }
    add(name, strVal);
  }

  private void encodeFloat(final String name, final Float value) throws MALException
  {
    String strVal = STR_NULL;
    if (null != value)
    {
      strVal = value.toString();
    }
    add(name, strVal);
  }

  private void encodeAttribute(final boolean isAbstract, final String name, final Attribute value) throws IllegalArgumentException, MALException
  {
    if (null != value)
    {
      if (isAbstract)
      {
        add(name + ".type", Byte.toString(value.getShortForm().byteValue()));
      }

      switch (value.getTypeShortForm())
      {
        case Attribute._BOOLEAN_TYPE_SHORT_FORM:
          encodeBoolean(name, ((Union) value).getBooleanValue());
          break;
        case Attribute._DOUBLE_TYPE_SHORT_FORM:
          encodeDouble(name, ((Union) value).getDoubleValue());
          break;
        case Attribute._FLOAT_TYPE_SHORT_FORM:
          encodeFloat(name, ((Union) value).getFloatValue());
          break;
        case Attribute._INTEGER_TYPE_SHORT_FORM:
          encodeInteger(name, ((Union) value).getIntegerValue());
          break;
        case Attribute._LONG_TYPE_SHORT_FORM:
          encodeLong(name, ((Union) value).getLongValue());
          break;
        case Attribute._OCTET_TYPE_SHORT_FORM:
          encodeOctet(name, ((Union) value).getOctetValue());
          break;
        case Attribute._SHORT_TYPE_SHORT_FORM:
          encodeShort(name, ((Union) value).getShortValue());
          break;
        case Attribute._STRING_TYPE_SHORT_FORM:
          encodeString(name, ((Union) value).getStringValue());
          break;
        case Attribute._BLOB_TYPE_SHORT_FORM:
          encodeBlob(name, (Blob) value);
          break;
        case Attribute._DURATION_TYPE_SHORT_FORM:
          encodeDuration(name, (Duration) value);
          break;
        case Attribute._IDENTIFIER_TYPE_SHORT_FORM:
          encodeIdentifier(name, (Identifier) value);
          break;
        case Attribute._UOCTET_TYPE_SHORT_FORM:
          encodeUOctet(name, (UOctet) value);
          break;
        case Attribute._USHORT_TYPE_SHORT_FORM:
          encodeUShort(name, (UShort) value);
          break;
        case Attribute._UINTEGER_TYPE_SHORT_FORM:
          encodeUInteger(name, (UInteger) value);
          break;
        case Attribute._ULONG_TYPE_SHORT_FORM:
          encodeULong(name, (ULong) value);
          break;
        case Attribute._TIME_TYPE_SHORT_FORM:
          encodeTime(name, (Time) value);
          break;
        case Attribute._FINETIME_TYPE_SHORT_FORM:
          encodeFineTime(name, (FineTime) value);
          break;
        case Attribute._URI_TYPE_SHORT_FORM:
          encodeURI(name, (URI) value);
          break;

        default:
      }
    }
    else
    {
      add(name, STR_NULL);
    }
  }

  private void encodeList(final boolean isAbstract, final String name, final List value) throws MALException
  {
    boolean namePushed = pushName(name);

    if (isAbstract)
    {
      add("type", String.valueOf(((Element) value).getShortForm()));
    }

    encodeInteger("listSize", value.size());

    for (int i = 0; i < value.size(); i++)
    {
      internalEncodeField(false, String.valueOf(i), List.class, value.get(i));
    }

    popName(namePushed);
  }

  private void encodeEnumeration(final boolean isAbstract, final String name, final Enumeration value) throws MALException
  {
    if (isAbstract)
    {
      add(name + ".type", String.valueOf(((Element) value).getShortForm()));
    }

    encodeInteger(name, value.getOrdinal());
  }

  private void encodeComposite(final boolean isAbstract, final String name, final Class cls, final Composite value) throws MALException
  {
    boolean namePushed = pushName(name);

    if (isAbstract)
    {
      add(name + ".type", String.valueOf(value.getShortForm()));
    }

    Class superCls = cls.getSuperclass();

    if (!"Object".equals(superCls.getSimpleName()))
    {
      encodeComposite(false, null, superCls, value);
    }

    Field[] fields = cls.getDeclaredFields();

    for (Field field : fields)
    {
      final int mods = field.getModifiers();
      if (!Modifier.isStatic(mods))
      {
        try
        {
          Method method = cls.getDeclaredMethod("get" + preCap(field.getName()));

          encodeField(field.getName(), field.getType(), method.invoke(value, (Object[]) null));
        }
        catch (Exception ex)
        {
          // nothing
        }
      }
    }

    popName(namePushed);
  }

  private static String preCap(String str)
  {
    if ((null != str) && (0 < str.length()))
    {
      str = String.valueOf(str.charAt(0)).toUpperCase() + str.substring(1);
    }

    return str;
  }

  private boolean pushName(String val)
  {
    if (null != val)
    {
      nameStack.addLast(val);
      return true;
    }
    return false;
  }

  private void popName(boolean namePushed)
  {
    if (namePushed)
    {
      nameStack.removeLast();
    }
  }

  private StringBuilder createName(final String lastName)
  {
    StringBuilder buf = new StringBuilder();

    for (String name : nameStack)
    {
      buf.append(name);
      buf.append('.');
    }

    buf.append(lastName);

    return buf;
  }

  private void add(final String name, final String val)
  {
    buffer.append(createName(name));
    buffer.append('=');
    buffer.append(val);
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
