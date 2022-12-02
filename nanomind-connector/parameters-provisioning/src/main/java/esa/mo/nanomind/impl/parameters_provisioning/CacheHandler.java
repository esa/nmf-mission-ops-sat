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
import java.util.HashMap;
import java.util.Map;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;
import esa.mo.nmf.nanosatmosupervisor.parameter.OBSWParameter;
import esa.mo.nmf.nanosatmosupervisor.parameter.OBSWParameterValuesProvider;

/**
 * Provides OBSW parameter values through a caching mechanism. For a given parameter, it only
 * returns a non-null value if a value for this parameter was previously cached.
 *
 * @author Tanguy Soto
 */
class CacheHandler extends OBSWParameterValuesProvider {

    /**
     * Map of OBSW parameter value by parameter name acting as our cache storage.
     */
    private final Map<Identifier, TimedAttributeValue> cache;

    /*
     * Cache configuration settings
     */

    /**
     * Maximum time a parameter value should stay in the cache in milliseconds.
     */
    private int cachingTime = 10000;

    /**
     * Creates a new instance of CacheHandler.
     * 
     * @param parameterMap
     */
    public CacheHandler(HashMap<Identifier, OBSWParameter> parameterMap) {
        super(parameterMap);
        cache = new HashMap<>();
    }

    /**
     * Sets the maximum time a parameter value should stay in the cache in milliseconds.
     * 
     * @param cachingTime the time
     */
    public void setCachingTime(int cachingTime) {
        this.cachingTime = cachingTime;
    }

    /**
     * Returns true if the cached value of this parameter has to be refreshed according to the cache
     * settings.
     *
     * @param identifier Name of the parameter
     * @return A boolean
     */
    public synchronized boolean mustRefreshValue(Identifier identifier) {
        // Value for this parameter has never been cached
        if (!cache.containsKey(identifier)) {
            return true;
        }

        long now = System.nanoTime()/1000000; //@TODO ok here ?

        // This parameter value is outdated
        return now - cache.get(identifier).getLastUpdateTime().getTime() > cachingTime;

        // No need to refresh, cached value is still valid.
    }

    /**
     * Updates the last request time of this parameter to the time of the call.
     * 
     * @param identifier Name of the parameter
     */
    public synchronized void updateLastRequestTime(Identifier identifier) {
        if (!cache.containsKey(identifier)) {
            return;
        }
        cache.get(identifier).updateLastRequestTime();
    }

    /**
     * Returns the date and time at which the parameter was last requested.
     *
     * @param identifier Name of the parameter
     * @return A Date object or null if the parameter was never requested before
     */
    public synchronized Date getLastRequestTime(Identifier identifier) {
        if (!cache.containsKey(identifier)) {
            return null;
        }
        return cache.get(identifier).getLastRequestTime();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Attribute getValue(Identifier identifier) {
        if (!cache.containsKey(identifier)) {
            return null;
        }
        return cache.get(identifier).getValue();
    }

    /**
     * Caches a value for a given OBSW parameter name
     *
     * @param value Value to cache
     * @param identifier Name of the parameter
     */
    public synchronized void cacheValue(Attribute value, Identifier identifier) {
        if (!cache.containsKey(identifier)) {
            cache.put(identifier, new TimedAttributeValue(value));
        } else {
            cache.get(identifier).setValue(value);
        }
    }

    @Override
    public Boolean setValue(Attribute value, Identifier identifier) {
        return false;
    }
}
