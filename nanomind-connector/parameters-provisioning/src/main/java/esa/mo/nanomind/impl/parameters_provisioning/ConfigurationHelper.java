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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper functions to load configuration (system) properties.
 * 
 * @author Tangy Soto
 */
public class ConfigurationHelper {
  private static final Logger LOGGER = Logger.getLogger(ConfigurationHelper.class.getName());

  /**
   * Tries to get a system property and parse it as an Integer.
   * 
   * @param propertyKey The property key
   * @param defaultValue Default value to return if the property is not found
   * @return the parsed system property
   */
  public static int getIntegerProperty(String propertyKey, int defaultValue) {
    String propertyValue = System.getProperty(propertyKey);
    if (propertyValue != null) {
      try {
        return Integer.parseInt(propertyValue);
      } catch (NumberFormatException e) {
        LOGGER.log(Level.WARNING,
            String.format("Error parsing properties %s to Integer, defaulting to %d", propertyKey,
                defaultValue),
            e);
        return defaultValue;
      }
    }
    LOGGER.log(Level.WARNING,
        String.format("Properties %s not found, defaulting to %d", propertyKey, defaultValue));
    return defaultValue;
  }
}
