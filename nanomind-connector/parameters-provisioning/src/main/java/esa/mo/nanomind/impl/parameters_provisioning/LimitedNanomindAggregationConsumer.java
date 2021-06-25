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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ccsds.moims.mo.mal.MALException;
import esa.mo.helpertools.connections.SingleConnectionDetails;
import esa.mo.nanomind.impl.consumer.AggregationNanomindConsumerServiceImpl;
import esa.opssat.nanomind.mc.aggregation.consumer.AggregationStub;

/**
 * Decorator for the AggregationNanomindConsumerServiceImpl to limit the rate at which it can be
 * queried.
 * 
 * @author Tanguy Soto
 */
class LimitedNanomindAggregationConsumer {
  /**
   * Maximum requests (TC sent to the Nanomind) per seconds.
   */
  private int MAX_QUERY_RATE = 7;

  /**
   * Our sliding window query rate limiter.
   */
  private SlidingWindowRateLimiter rateLimiter;

  /**
   * Whether we are currently limiting the query rate or not.
   */
  private boolean limitRate;

  AggregationNanomindConsumerServiceImpl aggConsumerServiceImpl;

  public LimitedNanomindAggregationConsumer(SingleConnectionDetails connectionDetails)
      throws MALException, MalformedURLException {
    aggConsumerServiceImpl = new AggregationNanomindConsumerServiceImpl(connectionDetails);
    loadProperties();
    initRateLimiter();
  }

  /**
   * Load the system properties that we need.
   */
  private void loadProperties() {
    //  Query rate
    String queryRateProp = "nmf.supervisor.parameter.valuesprovider.nanomind.maxQueryRate";
    MAX_QUERY_RATE = ConfigurationHelper.getIntegerProperty(queryRateProp, MAX_QUERY_RATE);
  }

  /**
   * Initializes our rate limiter.
   */
  private void initRateLimiter() {
    limitRate = true;
    rateLimiter = new SlidingWindowRateLimiter(MAX_QUERY_RATE, 1000, 100);
  }

  /**
   * Disable the rate limiter until enabled again.
   */
  public void enableRateLimiter() {
    limitRate = true;
  }

  /**
   * Enable the rate limiter until disabled again.
   */
  public void disableRateLimiter() {
    limitRate = false;
  }

  /**
   * @return The nanomind aggregation stub
   * @throws QueryRateExceededException if the rate limiter is enabled and we are currently over the
   *         limit
   */
  public AggregationStub getAggregationNanomindStub() throws QueryRateExceededException {
    if (limitRate && !rateLimiter.allowQuery()) {
      throw new QueryRateExceededException(
          String.format("Rate of %d query/s exceeded", MAX_QUERY_RATE));
    }
    return aggConsumerServiceImpl.getAggregationNanomindStub();
  }

  /**
   * Exception raised when the rate limiter is enabled and we are over the limit.
   * 
   * @author Tanguy Soto
   */
  public class QueryRateExceededException extends Exception {
    private static final long serialVersionUID = 6011021055757437002L;

    public QueryRateExceededException(String errorMessage) {
      super(errorMessage);
    }
  }

  /**
   * Simple rate limiter using a sliding window.
   * 
   * @author Tanguy Soto
   */
  private class SlidingWindowRateLimiter {

    /**
     * Number of queries allowed per time window
     */
    private final int MAX_QUERIES;

    /**
     * The time window (in milliseconds)
     */
    private final int TIME_WINDOW_MS;

    /**
     * Size of the intervals (in milliseconds) at which the windows slides
     */
    private final int INTERVAL_MS;

    /**
     * Queries count for each interval of the time window
     */
    private final Map<Long, Integer> countsPerInterval;

    /**
     * Total queries count of the current time window
     */
    private int totalCounts;

    /**
     * Creates a new SlidingWindowRateLimiter.
     * 
     * @param maxQueries Number of queries allowed per time window
     * @param timeWindowMs The time window (in milliseconds)
     * @param intervalMs Size of the intervals (in milliseconds) at which the windows slides.
     *        Smaller intervals provide better accuracy in rate limiting but consume more memory.
     */
    public SlidingWindowRateLimiter(int maxQueries, int timeWindowMs, int intervalMs) {
      this.MAX_QUERIES = maxQueries;
      this.TIME_WINDOW_MS = timeWindowMs;
      this.INTERVAL_MS = intervalMs;

      this.countsPerInterval = new HashMap<>();
      this.totalCounts = 0;
    }

    /**
     * @return true if a query is allowed at the time of call, false if the query rate is already
     *         over the limit
     */
    public synchronized boolean allowQuery() {
      long currentTimestamp = System.currentTimeMillis();

      // update current interval queries count
      long currentInterval = getInterval(currentTimestamp);
      Integer currentCount = countsPerInterval.get(currentInterval);
      if (currentCount == null) {
        currentCount = 0;
      }
      countsPerInterval.put(currentInterval, currentCount + 1);

      // update total window queries count
      cleanOlderIntervals(currentTimestamp);
      totalCounts++;

      // return depending on total window queries count
      return totalCounts <= MAX_QUERIES;
    }

    /**
     * @param timestamp The timestamp
     * @return The interval in which the timestamp belongs to
     */
    private long getInterval(long timestamp) {
      return (timestamp / INTERVAL_MS) * INTERVAL_MS;
    }

    /**
     * Removes intervals that slid out of the window at the current timestamp.
     * 
     * @param currentTimestamp The timestamp
     */
    private void cleanOlderIntervals(long currentTimestamp) {
      long oldestValidInterval = getInterval(currentTimestamp - this.TIME_WINDOW_MS);

      List<Long> intervalsToDelete = countsPerInterval.keySet().stream()
          .filter(interval -> interval < oldestValidInterval).collect(Collectors.toList());

      for (long interval : intervalsToDelete) {
        totalCounts -= this.countsPerInterval.get(interval);
        countsPerInterval.remove(interval);
      }
    }
  }
}
