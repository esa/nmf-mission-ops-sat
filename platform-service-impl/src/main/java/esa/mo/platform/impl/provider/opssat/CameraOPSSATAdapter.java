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
import esa.opssat.camera.processing.OPSSATCameraDebayering;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
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
  private static final float PREVIEW_GAIN = 10.f;
  private String blockDevice;
  private String serialPort;
  private boolean useWatchdog;
  private int nativeImageLength;
  private int nativeImageWidth;
  private bst_ims100_img_config_t imageConfig;
  private final PictureFormatList supportedFormats = new PictureFormatList();
  private boolean unitAvailable = false;

  private static final Logger LOGGER = Logger.getLogger(CameraOPSSATAdapter.class.getName());

  public CameraOPSSATAdapter()
  {
    supportedFormats.add(PictureFormat.RAW);
    supportedFormats.add(PictureFormat.RGB24);
    supportedFormats.add(PictureFormat.BMP);
    supportedFormats.add(PictureFormat.PNG);
    supportedFormats.add(PictureFormat.JPG);
    LOGGER.log(Level.INFO, "Initialisation");
    try {
      System.loadLibrary("ims100_api_jni");
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE,
          "Camera library could not be loaded!", ex);
      unitAvailable = false;
      return;
    }
    imageConfig = new bst_ims100_img_config_t();
    try {
      this.initBSTCamera();
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE,
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
    // FIXME: For now it always returns false?!?!?
    /*if (ret != bst_ret_t.BST_RETURN_SUCCESS) {
      throw new IOException("Failed to initialise BST camera (return: " + ret.toString() + ")");
    }*/
    ims100_api.bst_ims100_img_config_default(imageConfig);
    ims100_api.bst_ims100_set_img_config(imageConfig);
    ims100_api.bst_ims100_set_exp_time(imageConfig.getT_exp());
    nativeImageWidth = imageConfig.getCol_end() - imageConfig.getCol_start() + 1;
    nativeImageLength = imageConfig.getRow_end() - imageConfig.getRow_start() + 1;
  }

  private synchronized void dumpHKTelemetry()
  {
    bst_ims100_tele_std_t stdTM = new bst_ims100_tele_std_t();
    ims100_api.bst_ims100_get_tele_std(stdTM);
    LOGGER.log(Level.INFO,
        String.format("Dumping HK Telemetry...\n"
            + "Standard TM:\n"
            + "Version: %s\n"
            + "Temp: %d degC\n"
            + "Status byte: 0x%02X",
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
    availableResolutions.add(new PixelResolution(new UInteger(nativeImageWidth), new UInteger(
        nativeImageLength)));

    return availableResolutions;
  }

  @Override
  public synchronized Picture getPicturePreview() throws IOException
  {
    final PixelResolution resolution = new PixelResolution(new UInteger(nativeImageWidth),
        new UInteger(nativeImageLength));
    return takePicture(new CameraSettings(resolution, PictureFormat.RAW, PREVIEW_EXPOSURE_TIME,
        PREVIEW_GAIN, PREVIEW_GAIN, PREVIEW_GAIN));

  }

  @Override
  public synchronized Picture takePicture(CameraSettings settings) throws IOException
  {
    bst_ims100_img_t image = new bst_ims100_img_t();
    ims100_api.bst_ims100_img_config_default(imageConfig);
    // TODO this is not scaling but cropping the picture
    imageConfig.setCol_start(0);
    imageConfig.setCol_end((int) settings.getResolution().getWidth().getValue() - 1);
    imageConfig.setRow_start(0);
    imageConfig.setRow_end((int) settings.getResolution().getHeight().getValue() - 1);
    imageConfig.setT_exp((int) (settings.getExposureTime().getValue() * 1000));
    imageConfig.setG_red(settings.getGainRed().shortValue());
    imageConfig.setG_green(settings.getGainGreen().shortValue());
    imageConfig.setG_blue(settings.getGainBlue().shortValue());
    LOGGER.log(Level.INFO, String.format("Setting config"));
    ims100_api.bst_ims100_set_img_config(imageConfig);
    // Each pixel of raw image is encoded as uint16
    LOGGER.log(Level.INFO, String.format("Allocating native buffer"));
    int dataN
        = (int) (settings.getResolution().getHeight().getValue() * settings.getResolution().getWidth().getValue());
    ByteBuffer imageData = ByteBuffer.allocateDirect(
        (int) (dataN * 2));
    image.setData(imageData);
    image.setData_n(dataN);

    final Time timestamp = HelperTime.getTimestampMillis();
    LOGGER.log(Level.INFO, String.format("Acquiring image"));
    if (ims100_api.bst_ims100_get_img_n(image, 1, (short) 0) != bst_ret_t.BST_RETURN_SUCCESS) {
      throw new IOException("bst_ims100_get_img_n failed");
    }
    byte[] rawData = new byte[imageData.capacity()];

    LOGGER.log(Level.INFO, String.format("Copying from native buffer"));
    ((ByteBuffer) (imageData.duplicate().clear())).get(rawData);
    CameraSettings replySettings = new CameraSettings();
    replySettings.setResolution(settings.getResolution());
    replySettings.setExposureTime(settings.getExposureTime());
    replySettings.setGainRed(settings.getGainRed());
    replySettings.setGainGreen(settings.getGainGreen());
    replySettings.setGainBlue(settings.getGainBlue());
    if (settings.getFormat() != PictureFormat.RAW) {
      // Run debayering and possibly process further
      //TODO Use a native debayering acceleration
      LOGGER.log(Level.INFO, String.format(
            "Converting the image from RAW to " + settings.getFormat().toString()));
      rawData = convertImage(rawData, settings.getFormat());
    }
    replySettings.setFormat(settings.getFormat());
    return new Picture(timestamp, replySettings, new Blob(rawData));
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

  private byte[] convertImage(byte[] rawImage, final PictureFormat targetFormat) throws
      IOException
  {
    BufferedImage image = OPSSATCameraDebayering.getDebayeredImage(rawImage);
    byte[] ret = null;

    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    if (targetFormat.equals(PictureFormat.RGB24)) {
      int w = image.getWidth();
      int h = image.getHeight();
      int[] rgba = image.getRGB(0, 0, w, h, null, 0, w);
      ret = new byte[rgba.length * 3];
      for (int i = 0; i < rgba.length; ++i) {
        final int pixelval = rgba[i];
        ret[i * 3 + 0] = (byte) ((pixelval >> 16) & 0xFF); // R
        ret[i * 3 + 1] = (byte) ((pixelval >> 8) & 0xFF);  // G
        ret[i * 3 + 2] = (byte) ((pixelval) & 0xFF);       // B
        // Ignore Alpha channel
      }
    } else if (targetFormat.equals(PictureFormat.BMP)) {
      ImageIO.write(image, "BMP", stream);
      ret = stream.toByteArray();
      stream.close();
    } else if (targetFormat.equals(PictureFormat.PNG)) {
      ImageIO.write(image, "PNG", stream);
      ret = stream.toByteArray();
      stream.close();
    } else if (targetFormat.equals(PictureFormat.JPG)) {
      ImageIO.write(image, "JPEG", stream);
      ret = stream.toByteArray();
      stream.close();
    } else {
      throw new IOException(targetFormat.toString() + " format not supported.");
    }
    return ret;
  }
}
