/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO Fixed Length Binary encoder
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
package esa.mo.mal.encoder.binary.fixed;

/**
 * Implements the MALElementOutputStream interface for a fixed length binary encoding.
 */
public class FixedBinaryElementOutputStream extends esa.mo.mal.encoder.binary.BinaryElementOutputStream
{
  /**
   * Constructor.
   *
   * @param os Output stream to write to.
   */
  public FixedBinaryElementOutputStream(final java.io.OutputStream os)
  {
    super(os);
  }

  @Override
  protected esa.mo.mal.encoder.gen.GENEncoder createEncoder(java.io.OutputStream os)
  {
    return new FixedBinaryEncoder(os);
  }
}
