/* ----------------------------------------------------------------------------
 * Copyright (C) 2021      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under European Space Agency Public License (ESA-PL) Weak Copyleft – v2.4
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
package esa.mo.nanomind.impl.parameters_provisioning;

import java.util.Date;
import org.ccsds.moims.mo.mal.structures.Attribute;

/**
 * Wrapper around an Attribute and the times at which it was last updated and requested.
 *
 * @author Tanguy Soto
 */
class TimedAttributeValue {
  /**
   * The latest value
   */
  private Attribute value;

  /**
   * The latest update time of value
   */
  private Date lastUpdateTime;

  /**
   * The latest request time of value
   */
  private Date lastRequestTime;

  /**
   * Creates a new instance of TimedAttributeValue and sets the last update time to now.
   *
   * @param value The value
   */
  public TimedAttributeValue(Attribute value) {
    this.value = value;
    lastUpdateTime = new Date();
    lastRequestTime = lastUpdateTime;
  }

  /**
   * @return The latest value
   */
  public Attribute getValue() {
    return value;
  }

  /**
   * Sets the value and updates the latest update time.
   * 
   * @param value the new value to set
   */
  public void setValue(Attribute value) {
    this.value = value;
    lastUpdateTime = new Date();
  }

  /**
   * Returns the latest update time of this value.
   * 
   * @return a Date containing the latest update time
   */
  public Date getLastUpdateTime() {
    return lastUpdateTime;
  }

  /*
   * Updates the last request time of this value to the time of the call.
   */
  public void updateLastRequestTime() {
    lastRequestTime = new Date();
  }

  /**
   * Returns the latest request time of this value.
   * 
   * @return a Date containing the latest request time
   */
  public Date getLastRequestTime() {
    return lastRequestTime;
  }
}
