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

import java.util.Map;
import org.ccsds.moims.mo.mal.MALException;

/**
 * Implements the MALElementStreamFactory interface for a SPP binary encoding.
 */
public class SPPFixedBinaryStreamFactory extends esa.mo.mal.encoder.binary.fixed.FixedBinaryStreamFactory
{
  public static final String SMALL_LENGTH_FIELD = "esa.mo.mal.encoding.spp.smallLengthField";
  public static final String TIME_PFIELD_PROPERTY = "org.ccsds.moims.mo.malspp.timePfield";
  public static final String TIME_EPOCH_PROPERTY = "org.ccsds.moims.mo.malspp.timeEpoch";
  public static final String TIME_SCALE_PROPERTY = "org.ccsds.moims.mo.malspp.timeScale";
  public static final String FINETIME_PFIELD_PROPERTY = "org.ccsds.moims.mo.malspp.fineTimePfield";
  public static final String FINETIME_EPOCH_PROPERTY = "org.ccsds.moims.mo.malspp.fineTimeEpoch";
  public static final String FINETIME_SCALE_PROPERTY = "org.ccsds.moims.mo.malspp.fineTimeScale";
  public static int SECONDS_FROM_CCSDS_TO_UNIX_EPOCH   = 378691208;
  public static long FINETIME_EPOCH   = 9223372036854775807L;
  
  private boolean smallLengthField = false;
  private SPPTimeHandler timeHandler = null;

  @Override
  protected void init(final String protocol, final Map properties) throws IllegalArgumentException, MALException
  {
    super.init(protocol, properties);

    timeHandler = new SPPTimeHandler(properties);

    if (null != properties)
    {
      if (properties.containsKey(SMALL_LENGTH_FIELD)
              && Boolean.parseBoolean(properties.get(SMALL_LENGTH_FIELD).toString()))
      {
        smallLengthField = true;
      }
    }
  }

  @Override
  public org.ccsds.moims.mo.mal.encoding.MALElementInputStream createInputStream(final byte[] bytes, final int offset)
  {
    return new SPPFixedBinaryElementInputStream(bytes, offset, smallLengthField, timeHandler);
  }

  @Override
  public org.ccsds.moims.mo.mal.encoding.MALElementInputStream createInputStream(final java.io.InputStream is)
          throws org.ccsds.moims.mo.mal.MALException
  {
    return new SPPFixedBinaryElementInputStream(is, smallLengthField, timeHandler);
  }

  @Override
  public org.ccsds.moims.mo.mal.encoding.MALElementOutputStream createOutputStream(final java.io.OutputStream os)
          throws org.ccsds.moims.mo.mal.MALException
  {
    return new SPPFixedBinaryElementOutputStream(os, smallLengthField, timeHandler);
  }
}
