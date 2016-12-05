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
package esa.mo.mal.transport.spp;

/**
 * Small class that implements a simple SSC.
 */
public class SPPSourceSequenceCounterSimple implements SPPSourceSequenceCounter
{
  private int sequenceCount = 0;

  @Override
  public int getNextSourceSequenceCount()
  {
      int i;

    synchronized (this)
    {
      i = sequenceCount++;
      if (sequenceCount > 16383)
      {
        sequenceCount = 0;
      }
    }

    return i;
  }
}
