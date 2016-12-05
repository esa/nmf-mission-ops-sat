/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO Generic Transport Framework
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
package esa.mo.mal.transport.gen.util;

/**
 * Simple class for holding helper methods.
 */
public abstract class GENHelper
{
  private GENHelper()
  {
    // private contructor as not a real class but a place for static methods
  }
  
  /**
   * Creates a string version of byte buffer in hex.
   *
   * @param data the packet.
   * @return the string representation.
   */
  public static String byteArrayToHexString(final byte[] data)
  {
    final StringBuilder hexString = new StringBuilder();

    if (null != data)
    {
      for (int i = 0; i < data.length; i++)
      {
        final String hex = Integer.toHexString(0xFF & data[i]);
        if (hex.length() == 1)
        {
          // could use a for loop, but we're only dealing with a single byte
          hexString.append('0');
        }
        hexString.append(hex);
      }
    }

    return hexString.toString();
  }
  
  /**
   * Creates a string version of byte buffer in hex.
   *
   * @param data the packet.
   * @param offset the offset.
   * @param length the length.
   * @return the string representation.
   */
  public static String byteArrayToHexString(final byte[] data, int offset, int length)
  {
    final StringBuilder hexString = new StringBuilder();

    if (null != data)
    {
      final int end = offset + length;
      for (int i = offset; i < end; i++)
      {
        final String hex = Integer.toHexString(0xFF & data[i]);
        if (hex.length() == 1)
        {
          // could use a for loop, but we're only dealing with a single byte
          hexString.append('0');
        }
        hexString.append(hex);
      }
    }

    return hexString.toString();
  }
}
