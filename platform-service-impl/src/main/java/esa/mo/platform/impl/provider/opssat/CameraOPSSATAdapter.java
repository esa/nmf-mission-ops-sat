/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
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
package esa.mo.platform.impl.provider.opssat;

import at.tugraz.ihf.opssat.ims100.bst_ims100_img_config_t;
import at.tugraz.ihf.opssat.ims100.bst_ims100_img_t;
import at.tugraz.ihf.opssat.ims100.bst_ret_t;
import at.tugraz.ihf.opssat.ims100.bst_ims100_tele_std_t;
import at.tugraz.ihf.opssat.ims100.ims100_api;
import esa.mo.helpertools.helpers.HelperTime;
import esa.mo.platform.impl.provider.gen.CameraAdapterInterface;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.platform.camera.structures.CameraSettings;
import org.ccsds.moims.mo.platform.camera.structures.Picture;
import org.ccsds.moims.mo.platform.camera.structures.PictureFormat;
import org.ccsds.moims.mo.platform.camera.structures.PictureFormatList;
import org.ccsds.moims.mo.platform.camera.structures.PixelResolution;
import org.ccsds.moims.mo.platform.camera.structures.PixelResolutionList;

/**
 *
 * @author Cesar Coelho
 */
public class CameraOPSSATAdapter implements CameraAdapterInterface
{

  private static final String SERIAL_PORT_ATTRIBUTE = "opssat.camera.port";
  private static final String SERIAL_PORT_DEFAULT = "/dev/ttyACM0";
  private static final String BLOCK_DEVICE_ATTRIBUTE = "opssat.camera.blockdev";
  private static final String BLOCK_DEVICE_DEFAULT = "/dev/sda";
  private static final String USE_WATCHDOG_ATTRIBUTE = "opssat.camera.usewatchdog";
  private static final String USE_WATCHDOG_DEFAULT = "true";
  private static final Duration PREVIEW_EXPOSURE_TIME = new Duration(0.100); // 100ms
  private static final Duration MINIMUM_PERIOD = new Duration(1); // 1 second for now...
  private String blockDevice;
  private String serialPort;
  private boolean useWatchdog;
  private int nativeImageLength;
  private int nativeImageWidth;
  private bst_ims100_img_config_t imageConfig = new bst_ims100_img_config_t();
  private final PictureFormatList supportedFormats = new PictureFormatList();
  private boolean unitAvailable = false;

  public CameraOPSSATAdapter()
  {
    supportedFormats.add(PictureFormat.RAW);
    Logger.getLogger(CameraOPSSATAdapter.class.getName()).log(Level.INFO, "Initialisation");
    try {
      System.loadLibrary("ims100_api_jni");
    } catch (Exception ex) {
      Logger.getLogger(CameraOPSSATAdapter.class.getName()).log(Level.SEVERE,
          "Camera library could not be loaded!", ex);
      unitAvailable = false;
      return;
    }
    try {
      this.initBSTCamera();
    } catch (Exception ex) {
      Logger.getLogger(CameraOPSSATAdapter.class.getName()).log(Level.SEVERE,
          "BST Camera adapter could not be initialized!", ex);
      unitAvailable = false;
      return;
    }
    dumpHKTelemetry();
    unitAvailable = true;
  }

  @Override
  public boolean isUnitAvailable()
  {
    return unitAvailable;
  }

  private void initBSTCamera() throws IOException
  {
    serialPort = System.getProperty(SERIAL_PORT_ATTRIBUTE, SERIAL_PORT_DEFAULT);
    blockDevice = System.getProperty(BLOCK_DEVICE_ATTRIBUTE, BLOCK_DEVICE_DEFAULT);
    useWatchdog = Boolean.parseBoolean(System.getProperty(USE_WATCHDOG_ATTRIBUTE,
        USE_WATCHDOG_DEFAULT));
    bst_ret_t ret = ims100_api.bst_ims100_init(serialPort, blockDevice, useWatchdog ? 1 : 0);
    if (ret != bst_ret_t.BST_RETURN_SUCCESS) {
      throw new IOException("Failed to initialise BST camera");
    }
    dumpHKTelemetry();
    ims100_api.bst_ims100_img_config_default(imageConfig);
    ims100_api.bst_ims100_set_img_config(imageConfig);
    ims100_api.bst_ims100_set_exp_time(imageConfig.getT_exp());
    nativeImageLength = imageConfig.getCol_end() - imageConfig.getCol_start() + 1;
    nativeImageWidth = imageConfig.getRow_end() - imageConfig.getRow_start() + 1;
  }

  private synchronized void dumpHKTelemetry()
  {
    bst_ims100_tele_std_t stdTM = new bst_ims100_tele_std_t();
    ims100_api.bst_ims100_get_tele_std(stdTM);
    Logger.getLogger(CameraOPSSATAdapter.class.getName()).log(Level.INFO, "Dumping HK Telemetry...");
    Logger.getLogger(CameraOPSSATAdapter.class.getName()).log(Level.INFO,
        String.format("Standard TM:\n"
            + "Version: %s\n"
            + "Temp: %d degC\n"
            + "Status byte: 0x%02X\n",
            stdTM.getVersion(),
            (int) stdTM.getTemp(),
            stdTM.getStatus()));
  }

  @Override
  public String getExtraInfo()
  {
    return "";
  }

  @Override
  public PixelResolutionList getAvailableResolutions()
  {
    PixelResolutionList availableResolutions = new PixelResolutionList();
    availableResolutions.add(new PixelResolution(new UInteger(nativeImageLength), new UInteger(
        nativeImageWidth)));

    return availableResolutions;
  }

  @Override
  public synchronized Picture getPicturePreview() throws IOException
  {
    final PixelResolution resolution = new PixelResolution(new UInteger(nativeImageWidth),
        new UInteger(nativeImageLength));
    return takePicture(resolution, PictureFormat.RAW, PREVIEW_EXPOSURE_TIME);

  }

  @Override
  public synchronized Picture takePicture(final PixelResolution resolution,
      final PictureFormat format, final Duration exposureTime) throws IOException
  {
    bst_ims100_img_t image = new bst_ims100_img_t();
    ims100_api.bst_ims100_img_config_default(imageConfig);
    // TODO this is not scaling but cropping the picture
    imageConfig.setCol_end((int) resolution.getHeight().getValue() - 1);
    imageConfig.setRow_end((int) resolution.getWidth().getValue() - 1);
    imageConfig.setT_exp((int) (exposureTime.getValue() * 1000));
    ims100_api.bst_ims100_set_img_config(imageConfig);
    ims100_api.bst_ims100_set_exp_time(imageConfig.getT_exp());
    // Each pixel of raw image is encoded as uint16
    ByteBuffer imageData = ByteBuffer.allocateDirect(
        (int) (resolution.getHeight().getValue() * resolution.getWidth().getValue() * 2));
    image.setData(imageData);

    final Time timestamp = HelperTime.getTimestampMillis();
    if (ims100_api.bst_ims100_get_img_n(image, 1, (short) 0) != bst_ret_t.BST_RETURN_SUCCESS) {
      throw new IOException("bst_ims100_get_img_n failed");
    }
    if (format == PictureFormat.RAW) {
      byte[] rawData = new byte[imageData.capacity()];
      ((ByteBuffer) (imageData.duplicate().clear())).get(rawData);

      CameraSettings pictureSettings = new CameraSettings();
      pictureSettings.setResolution(resolution);
      pictureSettings.setFormat(PictureFormat.RAW);
      pictureSettings.setExposureTime(exposureTime);
      Picture picture = new Picture(timestamp, pictureSettings, new Blob(rawData));
      return picture;
    } else {
      // Run debayering and possibly process further
      if (format == PictureFormat.RGB24) {
        throw new IOException("RGB24 format not supported.");
      } else if (format == PictureFormat.BMP) {
        throw new IOException("BMP format not supported.");
      } else if (format == PictureFormat.PNG) {
        throw new IOException("PNG format not supported.");
      } else if (format == PictureFormat.JPG) {
        throw new IOException("JPG format not supported.");
      }
      throw new IOException(format.toString() + " format not supported.");
    }
  }

  @Override
  public Duration getMinimumPeriod()
  {
    return MINIMUM_PERIOD;
  }

  @Override
  public PictureFormatList getAvailableFormats()
  {
    return supportedFormats;
  }
}
