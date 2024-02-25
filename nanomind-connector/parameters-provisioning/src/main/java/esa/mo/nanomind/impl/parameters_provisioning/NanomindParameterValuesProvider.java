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
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.FineTime;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.ULong;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.structures.Union;

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
     * Converts most MAL Attribute data types to their Long bits representation.
     * This method is designed to preserve the bit pattern of the original value where applicable,
     * facilitating conversions between different numerical and data types.
     *
     * @param in The MAL Attribute data type to be converted.
     * @return The converted Long bits representation.
     * @throws IllegalArgumentException If the input is an unsupported MAL Attribute type,
     *         indicating that the conversion cannot be performed. Unsupported types might include
     *         complex objects or specific numerical ranges that cannot be accurately represented in a Long.
     */
    private Long attributeToLongBits(Attribute in) throws IllegalArgumentException {
        if (in == null) {
            throw new IllegalArgumentException("The given value is null");
        }

        // Handle Union types with various forms
        if (in instanceof Union) {
            Union union = (Union) in;
            if (union.getTypeShortForm().equals(Union.BOOLEAN_TYPE_SHORT_FORM)) {
                return (long) (union.getBooleanValue() ? 1 : 0);
            } else if (union.getTypeShortForm().equals(Union.FLOAT_TYPE_SHORT_FORM)) {
                // Preserve the bit pattern, including the sign, of the float value
                return (long) (Float.floatToIntBits(union.getFloatValue()) & 0xFFFFFFFFL);
            } else if (union.getTypeShortForm().equals(Union.INTEGER_TYPE_SHORT_FORM)) {
                return (long) union.getIntegerValue();
            } else if (union.getTypeShortForm().equals(Union.SHORT_TYPE_SHORT_FORM)) {
                return (long) union.getShortValue();
            } else if (union.getTypeShortForm().equals(Union.LONG_TYPE_SHORT_FORM)) {
                return union.getLongValue();
            } else if (union.getTypeShortForm().equals(Union.OCTET_TYPE_SHORT_FORM)) {
                return (long) union.getOctetValue();
            } else if (union.getTypeShortForm().equals(Union.DOUBLE_TYPE_SHORT_FORM)) {
                return Double.doubleToLongBits(union.getDoubleValue());
            } else if (union.getTypeShortForm().equals(Union.STRING_TYPE_SHORT_FORM)) {
                throw new IllegalArgumentException("String values cannot be converted to a Long bit representation");
            } else {
                throw new IllegalArgumentException("The given value cannot be converted to a Long bit representation");
            }
        }
        // Other specified types handling
        else if (in instanceof Duration) {
            return Double.doubleToLongBits(((Duration) in).getValue());
        } else if (in instanceof Time) {
            return ((Time) in).getValue();
        } else if (in instanceof FineTime) {
            return ((FineTime) in).getValue();
        } else if (in instanceof UOctet) {
            return (long) ((UOctet) in).getValue();
        } else if (in instanceof UShort) {
            return (long) ((UShort) in).getValue();
        } else if (in instanceof UInteger) {
            return ((UInteger) in).getValue();
        } else if (in instanceof ULong) {
            try {
                return ((ULong) in).getValue().longValueExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("The ULong value cannot fit in a Long representation", e);
            }
        } else if (in instanceof Blob) {
            throw new IllegalArgumentException("The Blob value cannot fit in a Long representation");
        } else if (in instanceof Identifier) {
            throw new IllegalArgumentException("The Identifier value cannot fit in a Long representation");
        } else if (in instanceof URI) {
            throw new IllegalArgumentException("The URI value cannot fit in a Long representation");
        } else {
            throw new IllegalArgumentException("The given value could not be processed");
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
            LOGGER.log(Level.FINE, "getValue({0}) finished", identifier);
        }
    }

    @Override
    public Boolean setValue(Attribute rawValue, Identifier identifier) {

        Long longValue = null;
        Long longParameterID = null;

        try {
            longValue = this.attributeToLongBits(rawValue);
            longParameterID = parameterMap.get(identifier).getId();
            LOGGER.log(Level.INFO, "Prepare to set parameter with id {0} to value {1}", new Object[]{longParameterID,
                                                                                                     longValue});
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return false;
        }

        if (!acceptParameterID(longParameterID)) {
            return false;
        } else {
            // Prepare the attributes for the setValue action
            UInteger idUInt = new UInteger(longParameterID);
            Attribute id = (Attribute) HelperAttributes.javaType2Attribute(idUInt);
            AttributeValue parameterID = new AttributeValue(id);

            UInteger valueUInt = new UInteger(longValue);
            Attribute val = (Attribute) HelperAttributes.javaType2Attribute(valueUInt);
            AttributeValue parameterValue = new AttributeValue(val);

            AttributeValueList argList = new AttributeValueList();
            argList.add(parameterID);
            argList.add(parameterValue);

            // Invoke the setValue action
            actionInstDetails = new ActionInstanceDetails(ACTION_SET_VALUE_ID, false, false, true, argList, null, null);

            try {
                actionServiceCns.getActionStub().submitAction(ACTION_SET_VALUE_ID, actionInstDetails);
                LOGGER.log(Level.INFO, "Set parameter value action submitted");
                return true;
            } catch (MALException | MALInteractionException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                return false;
            }
        }
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
            LOGGER.log(Level.INFO, "Checking if parameter id {0} is within range {1}", new Object[]{parameterID, i
                .toString()});
            if (i.acceptParameterID(parameterID)) {
                // Found a match
                return true;
            }
        }
        LOGGER.log(Level.INFO, "The parameter id {0} is not within range of writable parameters", parameterID);
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
                int minimum = Integer.parseInt(vals[0]);
                int maximum = Integer.parseInt(vals[1]);
                init(minimum, maximum);
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

        @Override
        public String toString() {
            return this.min + "-" + this.max;
        }
    }
}
