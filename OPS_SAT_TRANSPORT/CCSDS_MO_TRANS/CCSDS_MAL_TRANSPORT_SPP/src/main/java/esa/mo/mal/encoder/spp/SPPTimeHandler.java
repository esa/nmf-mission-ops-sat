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

import static esa.mo.mal.encoder.spp.SPPFixedBinaryStreamFactory.SECONDS_FROM_CCSDS_TO_UNIX_EPOCH;
import static esa.mo.mal.encoder.spp.SPPFixedBinaryStreamFactory.TIME_PFIELD_PROPERTY;
import java.io.IOException;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.FineTime;
import org.ccsds.moims.mo.mal.structures.Time;

/**
 * Small class for handling time encoding/decoding.
 */
public class SPPTimeHandler
{
  private final boolean timeScaleIsUTC;
  private final boolean timeEpoch;
  private final int timeMajorUnitFieldLength;
  private final int timeMinorUnitFieldLength;
  private final boolean fineTimeScaleIsUTC;
  private final boolean fineTimeEpoch;
  private final int fineTimeMajorUnitFieldLength;
  private final int fineTimeMinorUnitFieldLength;

  protected SPPTimeHandler(final Map properties) throws IllegalArgumentException
  {
    boolean ltimeScaleIsUTC = true;
    boolean ltimeEpoch = true;
    int ltimeMajorUnitFieldLength = 4;
    int ltimeMinorUnitFieldLength = 3;
    boolean lfineTimeScaleIsUTC = true;
    boolean lfineTimeEpoch = true;
    int lfineTimeMajorUnitFieldLength = 4;
    int lfineTimeMinorUnitFieldLength = 5;

    if (null != properties)
    {
      if (properties.containsKey(TIME_PFIELD_PROPERTY))
      {
        String hexStr = properties.get(TIME_PFIELD_PROPERTY).toString();
        long bits = Long.parseLong(hexStr, 16);
        int tcf;
        int maj;
        int min;

        // check to see if two byte P-Field
        if (0 != (bits & 0x8000))
        {
          // 16bit
          tcf = (int) ((bits & 0x7000) >> 12);
          maj = (int) ((bits & 0x0C00) >> 10) + (int) ((bits & 0x60) >> 5) + 1;
          min = (int) ((bits & 0x0300) >> 8) + (int) ((bits & 0x1C) >> 2);
        }
        else
        {
          // 8bit
          tcf = (int) ((bits & 0x70) >> 4);
          maj = (int) ((bits & 0x0C) >> 8) + 1;
          min = (int) ((bits & 0x03));
        }

        if (1 == tcf)
        {
          ltimeEpoch = true;
          ltimeScaleIsUTC = false;
        }
        else
        {
          ltimeEpoch = true;
          ltimeScaleIsUTC = true;
        }

        ltimeMajorUnitFieldLength = maj;
        ltimeMinorUnitFieldLength = min;
      }
    }

    timeScaleIsUTC = ltimeScaleIsUTC;
    timeEpoch = ltimeEpoch;
    timeMajorUnitFieldLength = ltimeMajorUnitFieldLength;
    timeMinorUnitFieldLength = ltimeMinorUnitFieldLength;
    fineTimeScaleIsUTC = lfineTimeScaleIsUTC;
    fineTimeEpoch = lfineTimeEpoch;
    fineTimeMajorUnitFieldLength = lfineTimeMajorUnitFieldLength;
    fineTimeMinorUnitFieldLength = lfineTimeMinorUnitFieldLength;
  }

  public Duration decodeDuration(final TimeInputStream sourceBuffer) throws MALException
  {
    long s = getIntFromBytes(sourceBuffer, 4) * 1000;
    byte[] ss = sourceBuffer.directGetBytes(3);

    byte[] b = new byte[4];
    b[0] = 0;
    b[1] = ss[0];
    b[2] = ss[1];
    b[3] = ss[2];
    int ms = java.nio.ByteBuffer.wrap(b).getInt();

    s += ms;
    return new Duration(((double) s) / 1000.0);
  }

  public void encodeDuration(final TimeOutputStream outputStream, final Duration value) throws IOException
  {
    long tm = (long) (value.getValue() * 1000);

    int ms = (int) (tm % 1000);
    int s = (int) (tm / 1000);

    outputStream.directAdd(java.nio.ByteBuffer.allocate(4).putInt(s).array(), 0, 4);
    outputStream.directAdd(java.nio.ByteBuffer.allocate(4).putInt(ms).array(), 1, 3);
  }

  public Time decodeTime(final TimeInputStream sourceBuffer) throws MALException
  {
    long s = getIntFromBytes(sourceBuffer, timeMajorUnitFieldLength);
    byte[] fs = sourceBuffer.directGetBytes(timeMinorUnitFieldLength);

    double subseconds = 0;
    for (int i = fs.length - 1; i >= 0; --i)
    {
      subseconds = (subseconds + (fs[i] & 0xFF)) / 256;
    }

    if (!timeScaleIsUTC)
    {
      // TAI scale is 10s out from UTC
      s -= 10;
    }

    if (timeEpoch)
    {
      // CCSDS time epoch is 1/1/1958
      s -= SECONDS_FROM_CCSDS_TO_UNIX_EPOCH;
    }

    s *= 1000;
    s += Math.round(subseconds * 1000.0);

    return new Time(s);
  }

  public void encodeTime(final TimeOutputStream outputStream, final Time value) throws IOException
  {
    long tm = value.getValue();

    int ms = (int) (tm % 1000);
    int s = (int) (tm / 1000);

    if (!timeScaleIsUTC)
    {
      // TAI scale is 10s out from UTC
      s += 10;
    }

    if (timeEpoch)
    {
      // CCSDS time epoch is 1/1/1958
      s += SECONDS_FROM_CCSDS_TO_UNIX_EPOCH;
    }

    int ff = Math.min(4, timeMajorUnitFieldLength);
    outputStream.directAdd(java.nio.ByteBuffer.allocate(4).putInt(s).array(), 4 - ff, ff);

    double subseconds = ((double) ms) / 1000.0;
    for (int i = 0; i < timeMinorUnitFieldLength; ++i)
    {
      subseconds = subseconds * 256.0;
      outputStream.directAdd((byte) subseconds);
    }
  }

  public FineTime decodeFineTime(final TimeInputStream sourceBuffer) throws MALException
  {
    long s = getIntFromBytes(sourceBuffer, fineTimeMajorUnitFieldLength);
    byte[] fs = sourceBuffer.directGetBytes(fineTimeMinorUnitFieldLength);

    double subseconds = 0;
    for (int i = fs.length - 1; i >= 0; --i)
    {
      subseconds = (subseconds + (fs[i] & 0xFF)) / 256;
    }

    if (!fineTimeScaleIsUTC)
    {
      // TAI scale is 10s out from UTC
      s -= 10;
    }

    if (fineTimeEpoch)
    {
      // CCSDS time epoch is 1/1/1958
      //s -= SECONDS_FROM_CCSDS_TO_UNIX_EPOCH;
    }

    s *= 1000000000000L;
    s += Math.round(subseconds * 1000000000000.0);

    return new FineTime(s);
  }

  public void encodeFineTime(final TimeOutputStream outputStream, final FineTime value) throws IOException
  {
    long tm = value.getValue();

    long ms = (long) (tm % 1000000000000L);
    long s = (long) (tm / 1000000000000L);

    if (!fineTimeScaleIsUTC)
    {
      // TAI scale is 10s out from UTC at 1970
      s += 10;
    }

    if (fineTimeEpoch)
    {
      // CCSDS time epoch is 1/1/1958
      //s -= FINETIME_EPOCH;
    }

    int ff = Math.min(4, fineTimeMajorUnitFieldLength);
    outputStream.directAdd(java.nio.ByteBuffer.allocate(4).putInt((int) s).array(), 4 - ff, ff);

    double subseconds = ((double) ms) / 1000000000000.0;
    for (int i = 0; i < fineTimeMinorUnitFieldLength; ++i)
    {
      subseconds = subseconds * 256.0;
      outputStream.directAdd((byte) subseconds);
    }
  }

  private int getIntFromBytes(final TimeInputStream sourceBuffer, int countToRead) throws MALException
  {
    byte[] fs = sourceBuffer.directGetBytes(countToRead);

    byte[] b = new byte[4];
    b[0] = 0;
    b[1] = 0;
    b[2] = 0;
    b[3] = 0;

    System.arraycopy(fs, 0, b, 4 - countToRead, countToRead);
    return java.nio.ByteBuffer.wrap(b).getInt();
  }

  protected interface TimeInputStream
  {
    /**
     * Gets a byte array from the incoming stream.
     *
     * @param length The number of bytes to retrieve
     * @return the extracted byte.
     * @throws MALException If there is a problem with the decoding.
     */
    public abstract byte[] directGetBytes(int length) throws MALException;
  }

  protected interface TimeOutputStream
  {
    /**
     * Low level byte array write to the output stream.
     *
     * @param value the value to encode.
     * @param os offset into array.
     * @param ln length to add.
     * @throws IOException is there is a problem adding the value to the stream.
     */
    void directAdd(final byte[] value, int os, int ln) throws IOException;

    /**
     * Low level byte write to the output stream.
     *
     * @param value the value to encode.
     * @throws IOException is there is a problem adding the value to the stream.
     */
    void directAdd(final byte value) throws IOException;
  }
}
