/* ----------------------------------------------------------------------------
 * Copyright (C) 2018      European Space Agency
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
package esa.mo.platform.impl.provider.opssat;

import esa.mo.platform.impl.provider.gen.OpticalDataReceiverAdapterInterface;
import java.util.logging.Level;
import java.util.logging.Logger;

import esa.mo.platform.impl.provider.gen.PowerControlAdapterInterface;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.platform.powercontrol.structures.DeviceType;

import at.tugraz.ihf.opssat.opt_rx.SEPP_OPT_RX_API;

public class OpticalRxOPSSATAdapter implements OpticalDataReceiverAdapterInterface
{

  private static final Logger LOGGER = Logger.getLogger(OpticalRxOPSSATAdapter.class.getName());
  private SEPP_OPT_RX_API optRxApi;
  private final boolean apiLoaded;
  private boolean unitInitialized = false;

  private PowerControlAdapterInterface pcAdapter;
  private static final int OPTRX_WATCH_PERIOD_MS = 10 * 1000;
  private Thread watcherThread;

  public OpticalRxOPSSATAdapter(PowerControlAdapterInterface pcAdapter)
  {
    this.pcAdapter = pcAdapter;
    LOGGER.log(Level.INFO, "Initialisation");
    try {
      System.loadLibrary("opt_rx_api_jni");
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE,
          "OPTRX API could not be loaded!", ex);
      apiLoaded = false;
      return;
    }
    apiLoaded = true;
    watcherThread = new Thread(new OPTRXWatcher(), "OPTRX Watcher");
    watcherThread.start();
  }


  /**
   * Inits OPTRX API and puts it into default mode
   */
  private boolean initOPTRX() {
    try {
      synchronized(this) {
        optRxApi = new SEPP_OPT_RX_API();
      }
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, "OPTRX could not be initialised!", ex);
      return false;
    }
    return true;
  }
   /**
   * Monitors the OPTRXS offline->online transitions and configures it into default mode
   */
  private class OPTRXWatcher implements Runnable
  {
    public OPTRXWatcher()
    {
    }

    @Override
    public void run()
    {
      try {
        while (true) {
          Thread.sleep(OPTRX_WATCH_PERIOD_MS);
          boolean isAvailable = isUnitAvailableInternal();
          if (isAvailable && !unitInitialized) {
            LOGGER.log(Level.INFO, "OPTRX came online - attempting initialisation");
            if (initOPTRX()) {
              LOGGER.log(Level.INFO, "OPTRX initialised - marking available");
              unitInitialized = true;
            } else {
              LOGGER.log(Level.WARNING, "OPTRX init failed");
            }
          } else if (!isAvailable && unitInitialized) {
            LOGGER.log(Level.INFO, "OPTRX gone offline - marking unavailable");
            optRxApi = null;
            unitInitialized = false;
          }
        }
      } catch (InterruptedException ex) {
        return;
      }
    }
  }

  @Override
  public byte[] recordOpticalReceiverData(final Duration duration)
  {
    synchronized(this) {
      if (duration == null) {
        return null;
      }
      if (optRxApi == null) {
        return null;
      }
      LOGGER.log(Level.INFO, "Recording optical data for {0}s", duration);
      optRxApi.Set_SharedMemory_IF_Switch(1);
      try {
        Thread.sleep((long) (duration.getValue() * 1000));
      } catch (final InterruptedException e) {
        return null;
      }
      optRxApi.Set_SharedMemory_IF_Switch(0);
      // TODO capture data from the API
      return new byte[0];
    }
  }
  private boolean isUnitAvailableInternal()
  {
    return apiLoaded && pcAdapter.isDeviceEnabled(DeviceType.OPTRX);
  }

  @Override
  public boolean isUnitAvailable()
  {
    return isUnitAvailableInternal() && unitInitialized;
  }

}
