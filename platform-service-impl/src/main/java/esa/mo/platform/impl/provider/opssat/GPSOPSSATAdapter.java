/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.platform.gps.structures.TwoLineElementSet;
import org.ccsds.moims.mo.platform.powercontrol.structures.DeviceType;
import org.orekit.propagation.analytical.tle.TLE;

import esa.mo.nanomind.impl.util.NanomindServicesConsumer;
import esa.mo.platform.impl.provider.gen.GPSNMEAonlyAdapter;
import esa.mo.platform.impl.provider.gen.PowerControlAdapterInterface;
import esa.opssat.nanomind.opssat_pf.gps.consumer.GPSAdapter;

/**
 *
 * @author Cesar Coelho
 */
public class GPSOPSSATAdapter extends GPSNMEAonlyAdapter {
  private static final int GET_GPS_TIMEOUT_MS = 4000;
  private static final Logger LOGGER = Logger.getLogger(GPSOPSSATAdapter.class.getName());
  private final NanomindServicesConsumer obcServicesConsumer;
  private static final String TLE_LOCATION = File.separator + "etc" + File.separator + "tle";
  private String currentTleSentence = "";
  private long tleLastModified = -1;
  private PowerControlAdapterInterface pcAdapter;


  public GPSOPSSATAdapter(PowerControlAdapterInterface pcAdapter) {
    this.obcServicesConsumer = NanomindServicesConsumer.getInstance();
    this.pcAdapter = pcAdapter;
  }

  @Override
  public synchronized String getNMEASentence(String identifier) throws IOException {
    LOGGER.log(Level.FINE, "run getNMEASentence with \"{0}\"", identifier.trim());
    GPSHandler gpsHandler = new GPSHandler();
    try {
      obcServicesConsumer.getGPSNanomindService().getGPSNanomindStub().getGPSData(new Blob(identifier.getBytes()),
          gpsHandler);
    } catch (MALInteractionException | MALException ex) {
      throw new IOException("Error when retrieving GPS NMEA response from Nanomind", ex);
    }
    try {
      if(gpsHandler.lock.tryAcquire(GET_GPS_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
        return gpsHandler.response;
      } else {
        throw new IOException("Internal timeout when retrieving GPS NMEA response from Nanomind");
      }
    } catch (InterruptedException e) {
      throw new IOException("Error when retrieving GPS NMEA response from Nanomind", e);
    }
  }

  @Override
  public boolean isUnitAvailable()
  {
    return pcAdapter.isDeviceEnabled(DeviceType.GNSS);
  }

  public String getTLESentence() throws IOException
  {
    // read TLE from file
    File file = new File(TLE_LOCATION);

    // check if cached version is still accurate
    if (file.lastModified() == this.tleLastModified) {
      return this.currentTleSentence;
    }

    // if cached version is outdated, load from file
    this.tleLastModified = file.lastModified();
    this.currentTleSentence = new String(Files.readAllBytes(file.toPath()));
    return this.currentTleSentence;
  }

  @Override
  public TLE getTLE()
  {
    String content = "";
    try {
      content = this.getTLESentence();
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    String[] lines = content.split(System.lineSeparator());
    String line1;
    String line2;

    //split TLE into two main lines
    switch (lines.length) {
      case 3:
        //if header line exists, discard it
        line1 = lines[1];
        line2 = lines[2];
        break;
      case 2:
        line1 = lines[0];
        line2 = lines[1];
        break;
      default:
        LOGGER.log(Level.SEVERE,
            "TLE is empty or wrongly formated. TLE:{0}{1}", new Object[]{System.lineSeparator(),
              Arrays.toString(lines)});
        return null;
    }

    return new TLE(line1, line2);
  }

  private class GPSHandler extends GPSAdapter
  {
    final Semaphore lock = new Semaphore(0);
    String response = "";

    @Override
    public void getGPSDataResponseReceived(
        org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader,
        org.ccsds.moims.mo.mal.structures.Blob data, java.util.Map qosProperties)
    {
      try {
        response = new String(data.getValue());
        LOGGER.log(Level.FINE, "getNMEASentence answered with \"{0}\"", response);
      } catch (MALException ex) {
        Logger.getLogger(GPSHandler.class.getName()).log(Level.SEVERE, null, ex);
      }
      lock.release();
    }
  }
}
