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

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.URI;

import esa.mo.helpertools.connections.SingleConnectionDetails;
import esa.mo.helpertools.helpers.HelperAttributes;
import esa.mo.nanomind.impl.consumer.ActionNanomindConsumerServiceImpl;
import esa.mo.nmf.nanosatmosupervisor.parameter.OBSWAggregation;
import esa.mo.nmf.nanosatmosupervisor.parameter.OBSWParameter;
import esa.mo.nmf.nanosatmosupervisor.parameter.OBSWParameterValuesProvider;
import esa.opssat.nanomind.mc.aggregation.structures.AggregationValue;
import esa.opssat.nanomind.mc.action.structures.ActionInstanceDetails;
import esa.opssat.nanomind.mc.structures.AttributeValue;
import esa.opssat.nanomind.mc.structures.AttributeValueList;

/**
 * Provides OBSW parameter values by consuming the Nanomind aggregation service. Fetched values are
 * placed in a cache and aggregation service is used with restrictions to avoid overloading the
 * Nanomind.
 *
 * @author Tanguy Soto
 */
public class NanomindParameterValuesProvider extends OBSWParameterValuesProvider {
    /**
     * The logger
     */
    private static final Logger LOGGER = Logger.getLogger(NanomindParameterValuesProvider.class.getName());

    /**
     * Time (milliseconds) a parameter value stays in the cache before requesting a new one from the
     * Nanomind
     */
    private int CACHING_TIME = 10000;

    /**
     * Interval (seconds) between attempts to clean aggregations definitions.
     */
    private int AGGREGATION_CLEANING_INTERVAL = 300;

    /**
     * OBSW Parameter-write allowed ID ranges
     */
    private String WRITEABLE_PARAMETERS;

    /**
     * Object handling caching of the values
     */
    private CacheHandler cacheHandler;

    /**
     * Nanomind aggregations handler.
     */
    private NanomindAggregationsHandler aggHandler;

    private static final String NANOMIND_APID = "10"; // Default Nanomind APID (on 13 June 2016)
    private static final String MAL_SPP_BINDINDING = "malspp"; // Use the SPP Implementation
    private static final String SOURCE_ID = "0"; // OBSW supports any value. By default it is set to 0

    private static final Long ACTION_SET_VALUE_ID = (long) 0x1101; // 0x1101 HEX -> 4353 DEC

    ActionNanomindConsumerServiceImpl actionServiceCns;
    ActionInstanceDetails actionInstDetails;

    /**
     * Allowed ranges of writeable OBSW parameter IDs
     */
    private ParameterIDRange parameterIDs[];

    /**
     * Main lock to synchronize cache and aggregations manipulations.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a new instance of CacheParameterValuesProvider.
     * 
     * @param parameterMap The map of OBSW parameters for which we have to provide values for
     */
    public NanomindParameterValuesProvider(HashMap<Identifier, OBSWParameter> parameterMap) {
        super(parameterMap);
        loadProperties();
        loadParameterIDs();
        initCacheHandler(parameterMap);
        try {
            initAggregationHandler();
            scheduleCleaners();
        } catch (MALException | MalformedURLException ex) {
            LOGGER.log(Level.SEVERE, "Couldn't initialize the nanomind aggregation handler", ex);
        }
        try {
            initActionService();
        } catch (MALException | MalformedURLException ex) {
            LOGGER.log(Level.SEVERE, "Couldn't initialize the nanomind action handler", ex);
        }
    }

    /**
     * Load the system properties that we need.
     */
    private void loadProperties() {
        // Caching time
        String cachingTimeProp = "nmf.supervisor.parameter.valuesprovider.nanomind.cachingTime";
        CACHING_TIME = ConfigurationHelper.getIntegerProperty(cachingTimeProp, CACHING_TIME);

        // Cleaning interval
        String cleaningIntervalProp = "nmf.supervisor.parameter.valuesprovider.nanomind.cleaningInterval";
        AGGREGATION_CLEANING_INTERVAL = ConfigurationHelper.getIntegerProperty(cleaningIntervalProp,
            AGGREGATION_CLEANING_INTERVAL);

        // OBSW Parameter-write allowed ID range
        String writeableParametersProp = "nmf.supervisor.parameter.valuesprovider.nanomind.writeableParameters";
        WRITEABLE_PARAMETERS = System.getProperty(writeableParametersProp);
    }

    /**
     * Initializes the cache handler.
     *
     * @param parameterMap The map of OBSW parameters for which we have to provide values for
     */
    private void initCacheHandler(HashMap<Identifier, OBSWParameter> parameterMap) {
        this.cacheHandler = new CacheHandler(parameterMap);
        this.cacheHandler.setCachingTime(CACHING_TIME);
    }

    /**
     * Initializes the Nanomind aggregation handler.
     */
    private void initAggregationHandler() throws MalformedURLException, MALException {
        aggHandler = new NanomindAggregationsHandler();
    }

    /**
     * Initializes the Nanomind action service.
     */
    private void initActionService() throws MalformedURLException, MALException {
        final SingleConnectionDetails details = new SingleConnectionDetails();
        IdentifierList domain = new IdentifierList();
        domain.add(new Identifier("OPSSAT"));
        details.setDomain(domain);
        URI brokerUri = null;
        details.setBrokerURI(brokerUri);
        details.setProviderURI(MAL_SPP_BINDINDING + ":247/" + NANOMIND_APID + "/" + SOURCE_ID);
        actionServiceCns = new ActionNanomindConsumerServiceImpl(details);
    }

    /**
     * Schedules a full clean of aggregations once only at startup and starts the periodic cleaning of
     * parameters.
     */
    private void scheduleCleaners() {
        // Full clean on startup
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                cleanAllAggregations();
            }
        }, 10000);

        // Periodic cleaning
        Timer timer2 = new Timer(true);
        timer2.schedule(new TimerTask() {
            @Override
            public void run() {
                cleanParametersFromAggregations();
            }
        }, AGGREGATION_CLEANING_INTERVAL * 1000, AGGREGATION_CLEANING_INTERVAL * 1000);
    }

    /**
     * Cleans all the aggregations that could have been defined by us in the Nanomind.
     */
    public void cleanAllAggregations() {
        lock.lock();
        aggHandler.cleanAllAggregations();
        lock.unlock();
    }

    /**
     * Cleans parameters that have not been queried for a while from the aggregations definitions in
     * the Nanomind.
     */
    private void cleanParametersFromAggregations() {
        lock.lock();
        aggHandler.cleanParametersFromAggregations(cacheHandler, AGGREGATION_CLEANING_INTERVAL);
        lock.unlock();
    }

    /**
     * Parses parameters values from an aggregation value and update the cached values for those
     * parameters. Also returns the value of the parameter the aggregation was originally requested
     * for.
     *
     * @param aggValue The aggregation value to parse
     * @param agg Information about the OBSW aggregation
     * @param identifier Name of the parameter the aggregation was requested for
     * @return Value of the parameter the aggregation was requested for, null if the aggregation value
     *         passed is null
     */
    private Attribute retrieveValueAndUpdateCache(AggregationValue aggValue, OBSWAggregation agg,
        Identifier identifier) {
        // An error occured when fetching the parameter's aggregation value
        if (aggValue == null) {
            return null;
        }

        Attribute paramValue = null;

        // Parameter values are in the same order as in the aggregation definition
        for (int i = 0; i < aggValue.getValues().size(); i++) {
            Attribute value = aggValue.getValues().get(i).getRawValue();
            Identifier paramName = new Identifier(agg.getParameters().get(i).getName());

            // Return the requested parameter and update its request time
            if (paramName.equals(identifier)) {
                paramValue = value;
            }
            // A whole aggregation is returned, take the chance to update every parameters
            cacheHandler.cacheValue(value, paramName);
            LOGGER.log(Level.FINE, String.format("Cached value %s for parameter %s", value, paramName));
        }

        return paramValue;
    }

    /**
     * Fetches a new value for the given parameter and updates it in the cache.
     * 
     * @param identifier The parameter name
     * @return The value or null if the parameter is unknown or a problem occurred while fetching the
     *         value
     */
    private Attribute getNewValue(Identifier identifier) {
        // Parameter is unknown
        if (!parameterMap.containsKey(identifier)) {
            return null;
        }

        // Get a new value
        OBSWParameter obswParam = parameterMap.get(identifier);
        AggregationValue aggValue = aggHandler.getNewValue(obswParam);
        return retrieveValueAndUpdateCache(aggValue, obswParam.getAggregation(), identifier);
    }

    @Override
    public Attribute getValue(Identifier identifier) {
        if (!aggHandler.isInitialized()) {
            return null;
        }

        lock.lock();
        try {
            Attribute value = null;
            if (cacheHandler.mustRefreshValue(identifier)) {
                value = getNewValue(identifier);
            }
            value = cacheHandler.getValue(identifier);
            cacheHandler.updateLastRequestTime(identifier);
            return value;
        } finally {
            lock.unlock();
            LOGGER.log(Level.FINE, "getValue(" + identifier + ") finished");
        }
    }

    @Override
    public Boolean setValue(Attribute rawValue, Identifier identifier) {

        Long longParameterID = parameterMap.get(identifier).getId();

        if (acceptParameterID(longParameterID)) {
            UInteger idUInt = new UInteger(longParameterID);
            Attribute id = (Attribute) HelperAttributes.javaType2Attribute(idUInt);

            AttributeValue parameterID = new AttributeValue(id);
            AttributeValue parameterValue = new AttributeValue(rawValue);

            AttributeValueList argList = new AttributeValueList();
            argList.add(parameterID);
            argList.add(parameterValue);

            actionInstDetails = new ActionInstanceDetails(ACTION_SET_VALUE_ID, false, false, true, argList, null, null);

            try {
                actionServiceCns.getActionStub().submitAction(ACTION_SET_VALUE_ID, actionInstDetails);
                LOGGER.log(Level.INFO, "Set parameter value action submitted");
                return true;
            } catch (MALException | MALInteractionException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
        }
        return false;
    }

    public ParameterIDRange[] getParameterIDRanges() {
        return parameterIDs;
    }

    private ParameterIDRange[] loadParameterIDs() {
        if (parameterIDs == null) {
            String sparameterIDs[] = WRITEABLE_PARAMETERS.split(",");
            parameterIDs = new ParameterIDRange[sparameterIDs.length];
            for (int i = 0; i < sparameterIDs.length; i++) {
                parameterIDs[i] = new ParameterIDRange(sparameterIDs[i]);
            }
        }
        return parameterIDs;
    }

    public boolean acceptParameterID(Long parameterID) {
        for (ParameterIDRange i : parameterIDs) {
            if (i.acceptParameterID(parameterID))//found a match
            {
                return true;
            }
        }
        return false;
    }

    protected static class ParameterIDRange {

        private int min;
        private int max;

        public ParameterIDRange(String range) {
            String[] vals = range.split("-");
            if (vals.length > 2) {
                throw new ArrayIndexOutOfBoundsException("Range includes more than two values");
            }

            if (vals.length == 2) {
                int min = Integer.parseInt(vals[0]);
                int max = Integer.parseInt(vals[1]);
                init(min, max);
            } else {//vals.length  = 1
                int minmax = Integer.parseInt(vals[0]);
                init(minmax, minmax);
            }
        }

        public ParameterIDRange(int min, int max) {
            init(min, max);
        }

        private void init(int min, int max) {
            if (min > max) {
                throw new ArrayIndexOutOfBoundsException("Parameter ID Range: Start ID is larger than end ID");
            }
            this.min = min;
            this.max = max;
        }

        public boolean acceptParameterID(Long parameterID) {
            return parameterID >= this.min && parameterID <= this.max;
        }
    }
}
