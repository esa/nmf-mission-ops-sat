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

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mal.structures.URI;
import esa.mo.helpertools.connections.SingleConnectionDetails;
import esa.mo.nanomind.impl.parameters_provisioning.LimitedNanomindAggregationConsumer.QueryRateExceededException;
import esa.mo.nmf.nanosatmosupervisor.parameter.OBSWAggregation;
import esa.mo.nmf.nanosatmosupervisor.parameter.OBSWParameter;
import esa.opssat.nanomind.mc.aggregation.body.GetValueResponse;
import esa.opssat.nanomind.mc.aggregation.structures.AggregationCategory;
import esa.opssat.nanomind.mc.aggregation.structures.AggregationDefinition;
import esa.opssat.nanomind.mc.aggregation.structures.AggregationDefinitionList;
import esa.opssat.nanomind.mc.aggregation.structures.AggregationReference;
import esa.opssat.nanomind.mc.aggregation.structures.AggregationReferenceList;
import esa.opssat.nanomind.mc.aggregation.structures.AggregationValue;
import esa.opssat.nanomind.mc.aggregation.structures.AggregationValueList;
import esa.opssat.nanomind.mc.aggregation.structures.factory.AggregationCategoryFactory;

/**
 * Provides OBSW parameter values by consuming the Nanomind aggregation service. Aggregation service
 * is used with restrictions to avoid overloading the Nanomind.
 * 
 * @author Tanguy Soto
 *
 */
class NanomindAggregationsHandler {
  private static final Logger LOGGER =
      Logger.getLogger(NanomindAggregationsHandler.class.getName());

  /**
   * Maximum number of parameters in one aggregation
   */
  private int PARAMS_PER_AGGREGATION = 8;

  /*
   * Maximum number of aggregation that we can define in the Nanomind.
   */
  private int MAX_DEFINABLE_AGGREGATION = 100;

  /**
   * Nanomind aggregation service consumer
   */
  private LimitedNanomindAggregationConsumer aggServiceCns;

  private static final String NANOMIND_APID = "10"; // Default Nanomind APID (on 13 June 2016)
  private static final String MAL_SPP_BINDINDING = "malspp"; // Use the SPP Implementation
  private static final String SOURCE_ID = "0"; // OBSW supports any value. By default it is set to 0

  /**
   * Prefix for name of all aggregation definitions we generate.
   */
  private static final String AGG_DEF_NAME_PREFIX = "Z";

  /**
   * Identifier of cleaned aggregations that are available again after all their parameters where
   * removed from them because they were not used anymore.
   */
  private List<String> aggsToReUse = new ArrayList<>();

  /**
   * List of the aggregations currently defined by this class in the Nanomind.
   */
  private List<OBSWAggregation> nanomindDefinitions = new ArrayList<>();

  /**
   * False until we leaned potential leftovers aggregations definitions from the Nanomind.
   */
  private boolean isInitialized = false;

  public NanomindAggregationsHandler() throws MALException, MalformedURLException {
    loadProperties();
    initAggregationServiceConsumer();
  }

  /**
   * Load the system properties that we need.
   */
  private void loadProperties() {
    // Parameters per aggregation
    String paramsPerAggregationProp =
        "nmf.supervisor.parameter.valuesprovider.nanomind.paramsPerAggregation";
    PARAMS_PER_AGGREGATION =
        ConfigurationHelper.getIntegerProperty(paramsPerAggregationProp, PARAMS_PER_AGGREGATION);

    // Max definable aggregations
    String maxDefinableAggregationsProp =
        "nmf.supervisor.parameter.valuesprovider.nanomind.maxDefinableAggregations";
    MAX_DEFINABLE_AGGREGATION = ConfigurationHelper.getIntegerProperty(maxDefinableAggregationsProp,
        MAX_DEFINABLE_AGGREGATION);
  }

  /**
   * Initializes the Nanomind aggregation service consumer
   */
  private void initAggregationServiceConsumer() throws MALException, MalformedURLException {
    // Connection details to Nanomind aggregation service
    SingleConnectionDetails details = new SingleConnectionDetails();
    IdentifierList domain = new IdentifierList();
    domain.add(new Identifier("OPSSAT"));
    details.setDomain(domain);
    URI brokerUri = null;
    details.setBrokerURI(brokerUri);
    details.setProviderURI(MAL_SPP_BINDINDING + ":247/" + NANOMIND_APID + "/" + SOURCE_ID);

    aggServiceCns = new LimitedNanomindAggregationConsumer(details);
  }

  /**
   * Calls the getValue operation of the Nanomind aggregation service for the given aggregation.
   *
   * @param aggId The aggregation ID
   * @return the aggregation value or null in case a MAL exception was raised
   */
  private AggregationValue getNanomindAggregationValue(long aggId) {
    LongList aggIds = new LongList();
    aggIds.add(aggId);
    try {
      GetValueResponse valueResponse = aggServiceCns.getAggregationNanomindStub().getValue(aggIds);
      LOGGER.log(Level.FINE,
          String.format("Agg. value for agg. id %d fetched from Nanomind", aggId));

      AggregationValueList aggValueList = valueResponse.getBodyElement1();
      AggregationValue aggValue = aggValueList.get(0);
      return aggValue;
    } catch (MALInteractionException | MALException e) {
      LOGGER.log(Level.SEVERE,
          "Error while calling getValue operation of Nanomind aggregation service", e);
    } catch (QueryRateExceededException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }

    return null;
  }

  /**
   * Assign the given OBSW parameter to an aggregation in the Nanomind. If the limit of aggregations
   * definition is reached or a problem occured, the parameter aggregation's is set to null.
   *
   * @param obswParameter The parameter we want to add to an aggregation
   */
  private void assignAggregationToParameter(OBSWParameter obswParameter) {
    boolean isAssigned = false;
    OBSWAggregation obswAgg = findAvailableAggregation();

    // No existing aggregation can hold this parameter
    if (obswAgg == null) {
      obswAgg = createAggregation();
      // We had room for a new aggregation
      if (obswAgg != null) {
        obswAgg.getParameters().add(obswParameter);
        isAssigned = addDefinitionInNanomind(obswAgg);
        // we successfully re-used an aggregation, we remove it from availability list
        if (isAssigned && aggsToReUse.size() > 0) {
          aggsToReUse.remove(aggsToReUse.size() - 1);
        }
      }
    }
    // Update an existing aggregation
    else {
      obswAgg.getParameters().add(obswParameter);
      isAssigned = updateDefinitionInNanomind(obswAgg);
      // Update failed, don't leave the parameter in our local definition
      if (!isAssigned) {
        obswAgg.getParameters().remove(obswAgg.getParameters().size() - 1);
      }
    }

    if (isAssigned) {
      obswParameter.setAggregation(obswAgg);
    } else {
      obswParameter.setAggregation(null);
    }
  }

  /**
   * Iterates over the aggregation already defined in the Nanomind to see if one is empty enough to
   * include a new parameter.
   *
   * @return The aggregation or null if no aggregation is empty enough
   */
  private OBSWAggregation findAvailableAggregation() {
    for (OBSWAggregation agg : nanomindDefinitions) {
      if (agg.getParameters().size() < PARAMS_PER_AGGREGATION) {
        return agg;
      }
    }

    return null;
  }

  /**
   * Creates a new aggregation.
   *
   * @return The aggregation or null if we reached the limit of aggregations definitions
   */
  private OBSWAggregation createAggregation() {
    String aggIdentifier = nextAggregationIdentifier();
    if (aggIdentifier == null) {
      LOGGER.log(Level.WARNING,
          "Max number of aggregation definitions reached, can't fetch value of new parameter");
      return null;
    }

    OBSWAggregation newAggregation = new OBSWAggregation();
    long aggId = aggregationIdentifier2AggregationId(aggIdentifier);
    newAggregation.setId(aggId);
    newAggregation.setDynamic(false);
    newAggregation.setBuiltin(false);
    newAggregation.setName(aggIdentifier);
    newAggregation.setDescription("Gen. by supervisor");
    newAggregation.setCategory(new AggregationCategoryFactory().createElement().toString());
    newAggregation.setUpdateInterval(0);
    newAggregation.setGenerationEnabled(false);
    return newAggregation;
  }

  /**
   * Updates an existing aggregation definition in the Nanomind.
   *
   * @param updatedAggregation The new version of the aggregation
   * @return True if the update succeeded, false otherwise
   */
  private boolean updateDefinitionInNanomind(OBSWAggregation updatedAggregation) {
    LongList ids = new LongList(1);
    ids.add(updatedAggregation.getId());

    AggregationDefinitionList aggList = toAggregationDefinitionList(updatedAggregation);

    try {
      aggServiceCns.getAggregationNanomindStub().updateDefinition(ids, aggList);
      LOGGER.log(Level.FINE,
          String.format("Agg. definition %s updated in Nanomind", updatedAggregation.getName()));
      return true;
    } catch (MALInteractionException | MALException e) {
      // Aggregation couldn't be updated to the Nanomind
      LOGGER.log(Level.SEVERE,
          "Error while calling updateDefinition operation of Nanomind aggregation service", e);
    } catch (QueryRateExceededException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }

    return false;
  }

  /**
   * Registers the given aggregation to the aggregation definitions in the Nanomind.
   *
   * @param newAggregation The aggregation to register
   * @return True if the registration succeeded, false otherwise
   */
  private boolean addDefinitionInNanomind(OBSWAggregation newAggregation) {
    AggregationDefinitionList list = toAggregationDefinitionList(newAggregation);

    try {
      aggServiceCns.getAggregationNanomindStub().addDefinition(list);
      nanomindDefinitions.add(newAggregation);
      LOGGER.log(Level.FINE,
          String.format("Agg. definition %s added in Nanomind", newAggregation.getName()));
      return true;
    } catch (MALInteractionException | MALException e) {
      // Aggregation couldn't be added to the Nanomind
      LOGGER.log(Level.SEVERE,
          "Error while calling addDefinition operation of Nanomind aggregation service", e);
    } catch (QueryRateExceededException e) {
      LOGGER.log(Level.FINE, e.getMessage());
    }

    return false;
  }

  /**
   * Converts an OBSW aggregation object holder into an AggregationDefinitionList.
   *
   * @param obswAggregation The aggregation to convert
   * @return The aggregation definition list
   */
  private AggregationDefinitionList toAggregationDefinitionList(OBSWAggregation obswAggregation) {
    // Prepare list of parameters
    LongList paramIds = new LongList();
    for (OBSWParameter p : obswAggregation.getParameters()) {
      paramIds.add(p.getId());
    }

    // Aggregation reference
    AggregationReferenceList paramsSetList = new AggregationReferenceList();
    AggregationReference paramsSet =
        new AggregationReference(null, paramIds, new Duration(0), null);
    paramsSetList.add(paramsSet);

    // Aggregation definition
    AggregationDefinition def = new AggregationDefinition(new Identifier(obswAggregation.getName()),
        obswAggregation.getDescription(), AggregationCategory.GENERAL,
        obswAggregation.isGenerationEnabled(), new Duration(obswAggregation.getUpdateInterval()),
        false, new Duration(), paramsSetList);

    // Finally the list
    AggregationDefinitionList list = new AggregationDefinitionList();
    list.add(def);

    return list;
  }

  /**
   * Fetches a new value for the given parameter by querying the Nanomind aggregation service. We
   * define an aggregation including the parameter if not included in one yet and get the value of
   * this aggregation.
   * 
   * @param obswParam The parameter
   * @return The value of the containing aggregation or null if a problem occurred while fetching
   */
  public AggregationValue getNewValue(OBSWParameter obswParam) {
    // If parameter is not included in an aggregation we assign it to one
    if (obswParam.getAggregation() == null) {
      assignAggregationToParameter(obswParam);
    }
    // If assignment failed (nanomind rejected update or creation of aggregation), give up
    if (obswParam.getAggregation() == null) {
      return null;
    }

    // Query the nanomind using the aggregation service
    OBSWAggregation agg = obswParam.getAggregation();
    AggregationValue aggValue = getNanomindAggregationValue(agg.getId());
    return aggValue;
  }

  /**
   * @return The next aggregation identifier to use at the moment of the call or null if we reached
   *         the limit of aggregations definitions
   */
  private String nextAggregationIdentifier() {
    // re-use a cleaned aggregation first
    if (aggsToReUse.size() > 0) {
      return aggsToReUse.get(aggsToReUse.size() - 1);
    }
    // generate a new one if not full
    if (nanomindDefinitions.size() < MAX_DEFINABLE_AGGREGATION) {
      return getAggregationIdentifier(nanomindDefinitions.size());
    }
    return null;
  }

  /**
   * Generates a 4 letters aggregation identifier using from our local aggregation number.
   * 
   * @param i the local aggregation number between 0 and MAX_DEFINABLE_AGGREGATION - 1
   * @return the aggregation identifier
   */
  private String getAggregationIdentifier(int i) {
    return String.format(AGG_DEF_NAME_PREFIX + "%03d", i);
  }

  /**
   * Converts an 4 letters aggregation identifier to its corresponding aggregation ID in the
   * Nanomind. We know that on OPS-SAT Nanomind, an aggregation ID is equals to the long value
   * represented by the aggregation identifier's characters.
   * 
   * @param identifier the identifier to convert
   * @return the corresponding aggregation id
   */
  private long aggregationIdentifier2AggregationId(String identifier) {
    if (identifier == null || identifier.length() != 4) {
      LOGGER.log(Level.SEVERE, String.format(
          "Trying to convert a wrong aggregation identifier: %s, 0 is returned", identifier));
      return 0;
    }
    return new BigInteger(identifier.getBytes()).longValue();
  }

  /**
   * Cleans all the aggregations that could have been defined by us in the Nanomind.
   */
  public void cleanAllAggregations() {
    LOGGER.log(Level.INFO, "Cleaning aggregations definitions in Nanomind");
    try {
      // No query rate limit when cleaning
      aggServiceCns.disableRateLimiter();

      // Clean all possible aggregation definitions by batch of 10 definitions
      int step = 10;
      for (int i = 0; i < MAX_DEFINABLE_AGGREGATION + step; i += step) {
        // get their ids
        LongList aggIds = listDefinitionIds(i, i + step);
        if (aggIds != null) {
          // keep valid ones only
          LongList validAggIds = new LongList();
          for (Long id : aggIds) {
            if (id != null) {
              validAggIds.add(id);
            }
          }
          if (validAggIds.size() > 0) {
            aggServiceCns.getAggregationNanomindStub().removeDefinition(validAggIds);
          }
        }
      }
      isInitialized = true;
      nanomindDefinitions = new ArrayList<>();
      LOGGER.log(Level.INFO, "Cleaned aggregations definitions in Nanomind");
    } catch (QueryRateExceededException | MALInteractionException | MALException e) {
      LOGGER.log(Level.SEVERE, "Error cleaning Nanomind aggregation definitions", e);
    } finally {
      aggServiceCns.enableRateLimiter();
    }
  }

  /**
   * List aggregations definitions present in the Nanomind corresponding to the specified range of
   * our local aggregations number.
   * 
   * @param start The start of the range (included)
   * @param end The end of the range (excluded)
   * @return The list or null if an error happened
   */
  private LongList listDefinitionIds(int start, int end) {
    IdentifierList identifierList = new IdentifierList();
    for (int i = start; i < end; i++) {
      identifierList.add(new Identifier(getAggregationIdentifier(i)));
    }

    try {
      LongList idsList = aggServiceCns.getAggregationNanomindStub().listDefinition(identifierList);
      return idsList;
    } catch (QueryRateExceededException | MALInteractionException | MALException e) {
      LOGGER.log(Level.SEVERE,
          "Error while calling listDefinition operation of Nanomind aggregation service", e);
    }

    return null;
  }


  /**
   * Removes parameters that have not been requested for a while from the aggregations definitions
   * in the Nanomind.
   * 
   * @param cache The cache handler to determine if the parameter is still used
   * @param timeout The time (seconds) after which we consider a parameter is not used anymore
   */
  public void cleanParametersFromAggregations(CacheHandler cache, int timeout) {
    LOGGER.log(Level.INFO, "Cleaning unused parameters from aggregations");

    long now = System.currentTimeMillis();
    List<OBSWAggregation> newNanomindDefinitions = new ArrayList<>();

    try {
      // No query rate limit when cleaning
      aggServiceCns.disableRateLimiter();

      // for each aggregation
      for (OBSWAggregation agg : nanomindDefinitions) {
        List<OBSWParameter> removedParams = new ArrayList<>();
        List<OBSWParameter> keptParams = new ArrayList<>();

        List<OBSWParameter> originalParams = new ArrayList<>(agg.getParameters());

        // for each parameter
        for (OBSWParameter param : agg.getParameters()) {
          Identifier paramName = new Identifier(param.getName());
          // check if parameter has not been requested for a while
          Date lastRequestTime = cache.getLastRequestTime(paramName);
          if (lastRequestTime == null || now - lastRequestTime.getTime() > timeout * 1000) {
            removedParams.add(param);
            param.setAggregation(null);
          } else {
            keptParams.add(param);
          }
        }

        agg.getParameters().clear();
        boolean modifSuccess = true;

        // aggregation still has parameters
        if (keptParams.size() > 0) {
          // update it
          agg.getParameters().addAll(keptParams);
          if (removedParams.size() > 0) {
            modifSuccess = updateDefinitionInNanomind(agg);
          }
          newNanomindDefinitions.add(agg);
        }
        // aggregation is now empty
        else {
          LongList aggIdList = new LongList();
          aggIdList.add(agg.getId());
          try {
            aggServiceCns.getAggregationNanomindStub().removeDefinition(aggIdList);
            aggsToReUse.add(agg.getName());
            LOGGER.log(Level.FINE, String.format("Removed agg. definition %s", agg.getName()));
          } catch (QueryRateExceededException | MALInteractionException | MALException e) {
            LOGGER.log(Level.SEVERE, "Error cleaning Nanomind aggregation definition", e);
            modifSuccess = false;
            newNanomindDefinitions.add(agg);
          }
        }

        // re-construct the aggregation locally as it was if failure
        if (!modifSuccess) {
          for (OBSWParameter removedParam : removedParams) {
            removedParam.setAggregation(agg);
          }
          agg.getParameters().clear();
          agg.getParameters().addAll(originalParams);
        }
      }
    } finally {
      // logDefinedAggregations();
      aggServiceCns.enableRateLimiter();
    }

    // update final list of aggregations
    nanomindDefinitions.clear();
    nanomindDefinitions.addAll(newNanomindDefinitions);
  }

  /**
   * Logs aggregation definitions present locally and in the nanomind.
   */
  private void logDefinedAggregations() {
    String message = "Locally defined aggregations:\n";
    for (OBSWAggregation obswAgg : nanomindDefinitions) {
      message += (obswAgg.toString() + "\n");
      for (OBSWParameter p : obswAgg.getParameters()) {
        message += (p.toString() + "\n");
      }
    }
    LOGGER.log(Level.INFO, message);

    message = "Remotely (Nanomind) defined aggregations:\n";
    LongList idsList = listDefinitionIds(0, 5);
    if (idsList == null) {
      message += "";
    } else {
      for (Long id : idsList) {
        message += String.format("AggregationDefinition[ID=%s]\n", id);
      }
    }
    LOGGER.log(Level.INFO, message);
  }

  /**
   * @return the isInitialized
   */
  public boolean isInitialized() {
    return isInitialized;
  }
}
