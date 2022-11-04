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

import at.tugraz.ihf.opssat.ims100.bst_ims100_img_config_t;
import at.tugraz.ihf.opssat.ims100.bst_ims100_img_t;
import at.tugraz.ihf.opssat.ims100.bst_ret_t;
import at.tugraz.ihf.opssat.ims100.bst_ims100_tele_std_t;
import at.tugraz.ihf.opssat.ims100.ims100_api;
import esa.mo.helpertools.helpers.HelperTime;
import esa.mo.platform.impl.provider.gen.CameraAdapterInterface;
import esa.mo.platform.impl.provider.gen.PowerControlAdapterInterface;
import esa.opssat.camera.processing.OPSSATCameraDebayering;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.Buffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

import org.ccsds.moims.mo.common.configuration.ConfigurationHelper;
import org.ccsds.moims.mo.mal.MALException;
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
import org.ccsds.moims.mo.platform.powercontrol.structures.DeviceType;

/**
 *
 * @author Cesar Coelho
 */
public class CameraOPSSATAdapter implements CameraAdapterInterface
{

  private static final String SERIAL_PORT_ATTRIBUTE = "opssat.camera.port";
  private static final String SERIAL_PORT_DEFAULT = "/dev/cam_tty";
  private static final String BLOCK_DEVICE_ATTRIBUTE = "opssat.camera.blockdev";
  private static final String BLOCK_DEVICE_DEFAULT = "/dev/cam_sd";
  private static final String USE_WATCHDOG_ATTRIBUTE = "opssat.camera.usewatchdog";
  private static final String USE_WATCHDOG_DEFAULT = "true";
  private static final Duration PREVIEW_EXPOSURE_TIME = new Duration(0.002);
  private static final float MAX_EXPOSURE_TIME_S = 0.8f;
  private static final Duration MINIMUM_PERIOD = new Duration(1);
  private static final float PREVIEW_GAIN = 8.f;
  private static final String BITDEPTH_ATTRIBUTE = "opssat.camera.bitdepth";
  private static final String BITDEPTH_DEFAULT = "8";
  private String blockDevice;
  private String serialPort;
  private boolean useWatchdog;
  private int nativeImageHeight;
  private int nativeImageWidth;
  private int bitdepth;
  private bst_ims100_img_config_t imageConfig;
  private final PictureFormatList supportedFormats = new PictureFormatList();
  private final boolean unitAvailable;
  private final PowerControlAdapterInterface pcAdapter;

  private static final Logger LOGGER = Logger.getLogger(CameraOPSSATAdapter.class.getName());

  public CameraOPSSATAdapter(PowerControlAdapterInterface pcAdapter)
  {
    this.pcAdapter = pcAdapter;
    this.supportedFormats.add(PictureFormat.RAW);
    this.supportedFormats.add(PictureFormat.RGB24);
    this.supportedFormats.add(PictureFormat.BMP);
    this.supportedFormats.add(PictureFormat.PNG);
    this.supportedFormats.add(PictureFormat.JPG);
    LOGGER.log(Level.INFO, "Initialisation");
    try {
      System.loadLibrary("ims100_api_jni");
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE,
          "Camera library could not be loaded!", ex);
      this.unitAvailable = false;
      return;
    }
    // Mark it as available even if it is offline - might come up later
    this.unitAvailable = true;
    this.imageConfig = new bst_ims100_img_config_t();
    try {
      this.openCamera();
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE,
          "BST Camera could not be initialized for HK dump! It is possibly offline...", ex);
      this.closeCamera();
      return;
    }
    try {
      this.dumpHKTelemetry();
    } finally {
      this.closeCamera();
    }
  }

  @Override
  public boolean isUnitAvailable()
  {
    return this.unitAvailable && this.pcAdapter.isDeviceEnabled(DeviceType.CAMERA);
  }

  private void openCamera() throws IOException
  {
    synchronized(this) {
      serialPort = System.getProperty(SERIAL_PORT_ATTRIBUTE, SERIAL_PORT_DEFAULT);
      blockDevice = System.getProperty(BLOCK_DEVICE_ATTRIBUTE, BLOCK_DEVICE_DEFAULT);
      useWatchdog = Boolean.parseBoolean(System.getProperty(USE_WATCHDOG_ATTRIBUTE,
          USE_WATCHDOG_DEFAULT));
      bitdepth = Integer.parseInt(System.getProperty(BITDEPTH_ATTRIBUTE, BITDEPTH_DEFAULT));
      final bst_ret_t ret = ims100_api.bst_ims100_init(serialPort, blockDevice, useWatchdog ? 1 : 0);
      // FIXME: For now it always returns false?!?!?
      /*if (ret != bst_ret_t.BST_RETURN_SUCCESS) {
        throw new IOException("Failed to initialise BST camera (return: " + ret.toString() + ")");
      }*/
      ims100_api.bst_ims100_img_config_default(imageConfig);
      nativeImageWidth = imageConfig.getCol_end() - imageConfig.getCol_start() + 1;
      nativeImageHeight = imageConfig.getRow_end() - imageConfig.getRow_start() + 1;
    }
  }


  private void closeCamera()
  {
    synchronized(this) {
      ims100_api.bst_ims100_done();
    }
  }

  private synchronized void dumpHKTelemetry()
  {
    final bst_ims100_tele_std_t stdTM = new bst_ims100_tele_std_t();
    synchronized(this) {
      ims100_api.bst_ims100_get_tele_std(stdTM);
    }
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
    final PixelResolutionList availableResolutions = new PixelResolutionList();
    availableResolutions.add(new PixelResolution(new UInteger(nativeImageWidth), new UInteger(
        nativeImageHeight)));

    return availableResolutions;
  }

  @Override
  public synchronized Picture getPicturePreview() throws IOException
  {
    final PixelResolution resolution = new PixelResolution(new UInteger(nativeImageWidth),
        new UInteger(nativeImageHeight));
    return takePicture(new CameraSettings(resolution, PictureFormat.RAW, PREVIEW_EXPOSURE_TIME,
        PREVIEW_GAIN, PREVIEW_GAIN, PREVIEW_GAIN));

  }

  @Override
  public Picture takeAutoExposedPicture(final CameraSettings settings) throws IOException,
      MALException
  {
    final double F = 4;// f^2 value of ops-sat camera
    final double EV = Math.log(F / PREVIEW_EXPOSURE_TIME.getValue()) / Math.log(2);

    final CameraSettings tmpSettings = new CameraSettings(settings.getResolution(), PictureFormat.RAW,
      PREVIEW_EXPOSURE_TIME, PREVIEW_GAIN, PREVIEW_GAIN, PREVIEW_GAIN);
    LOGGER.log(Level.INFO, "Taking a sample picture");
    final bst_ims100_img_t image = innerTakePicture(tmpSettings);
    final byte[] rgbData = runNativeDebayering(image);
    BufferedImage bImage = rgbDataToBufferedImage(rgbData, (int)image.getAttr().getWidth(), (int)image.getAttr().getHeight());

    final int w = (int) settings.getResolution().getWidth().getValue();
    final int h = (int) settings.getResolution().getHeight().getValue();
    final int[] rgb = bImage.getRGB(0, 0, w, h, null, 0, w);

    double luminanceSum = 0;
    for (final int color : rgb) {
      final int red = (color >>> 16) & 0xFF;
      final int green = (color >>> 8) & 0xFF;
      final int blue = color & 0xFF; // shift by 0

      //calc luminance using sRGB luminance constants
      luminanceSum += (red * 0.2126 + green * 0.7152 + blue * 0.0722) / 255;
    }

    LOGGER.log(Level.INFO, "Luminance = {0}", luminanceSum);
    luminanceSum /= w * h;

    final double optimal_EV =
        EV
        + (Math.log(luminanceSum) / Math.log(2))
        - (Math.log(0.5) / Math.log(2));

    double optimalExposureTime = F * Math.pow(2, -optimal_EV);
    LOGGER.log(Level.INFO, "Normalised Luminance = {0}", luminanceSum);
    LOGGER.log(Level.INFO, "Calculated Exposure = {0}", optimalExposureTime);
    if (optimalExposureTime > MAX_EXPOSURE_TIME_S) {
      LOGGER.log(Level.INFO, "Exposure limited to {0}", MAX_EXPOSURE_TIME_S);
      optimalExposureTime = MAX_EXPOSURE_TIME_S;
    }

    tmpSettings.setFormat(settings.getFormat());
    tmpSettings.setExposureTime(new Duration(optimalExposureTime));
    return takePicture(tmpSettings);
  }

  private bst_ims100_img_t innerTakePicture(final CameraSettings settings) throws IOException
  {
    final bst_ims100_img_t image = new bst_ims100_img_t();
    synchronized(this) {
      this.openCamera();
      ims100_api.bst_ims100_img_config_default(imageConfig);
      // Note this is not scaling but cropping the picture
      imageConfig.setCol_start(0);
      imageConfig.setCol_end((int) settings.getResolution().getWidth().getValue() - 1);
      imageConfig.setRow_start(0);
      imageConfig.setRow_end((int) settings.getResolution().getHeight().getValue() - 1);
      imageConfig.setT_exp((int) (settings.getExposureTime().getValue() * 1000));
      imageConfig.setG_red(settings.getGainRed().shortValue());
      imageConfig.setG_green(settings.getGainGreen().shortValue());
      imageConfig.setG_blue(settings.getGainBlue().shortValue());
      LOGGER.log(Level.INFO, String.format("Setting config: %s", settings.toString()));
      ims100_api.bst_ims100_set_img_config(imageConfig);
      // Each pixel of raw image is encoded as uint16
      LOGGER.log(Level.FINE, String.format("Allocating native buffer"));
      final int dataN
          = (int) (settings.getResolution().getHeight().getValue() * settings.getResolution().getWidth().getValue());
      final ByteBuffer imageData = ByteBuffer.allocateDirect(
              dataN * 2);
      image.setData(imageData);
      image.setData_n(dataN);

      LOGGER.log(Level.INFO, String.format("Acquiring image"));
      if (ims100_api.bst_ims100_get_img_n(image, 1, (short) 0) != bst_ret_t.BST_RETURN_SUCCESS) {
        LOGGER.log(Level.WARNING, String.format("bst_ims100_get_img_n failed"));
        throw new IOException("bst_ims100_get_img_n failed");
      }
      this.closeCamera();
    }
    return image;
  }
  @Override
  public Picture takePicture(final CameraSettings settings) throws IOException
  {
    synchronized(this) {
      final Time timestamp = HelperTime.getTimestampMillis();
      final bst_ims100_img_t image = innerTakePicture(settings);

      byte[] rawData = null;
      final CameraSettings replySettings = new CameraSettings();
      replySettings.setResolution(settings.getResolution());
      replySettings.setExposureTime(settings.getExposureTime());
      replySettings.setGainRed(settings.getGainRed());
      replySettings.setGainGreen(settings.getGainGreen());
      replySettings.setGainBlue(settings.getGainBlue());
      if (settings.getFormat() != PictureFormat.RAW) {
        rawData = processRawCameraPicture(settings.getFormat(), image);
      } else if (settings.getFormat() == PictureFormat.RAW) {
        LOGGER.log(Level.FINE, String.format("Copying from native buffer"));
        rawData = new byte[image.getData().capacity()];
        ((ByteBuffer) (((Buffer)image.getData().duplicate()).clear())).get(rawData);
      }
      replySettings.setFormat(settings.getFormat());
      return new Picture(timestamp, replySettings, new Blob(rawData));
    }
  }

  public byte[] processRawCameraPicture(final PictureFormat targetFormat, final bst_ims100_img_t image)
      throws IOException {
    byte[] rgbData = runNativeDebayering(image);
    LOGGER.log(Level.INFO, String.format(
      "Converting the image from RGB to " + targetFormat.toString()));
    BufferedImage bImage = rgbDataToBufferedImage(rgbData, (int)image.getAttr().getWidth(), (int)image.getAttr().getHeight());
    return convertImage(bImage, targetFormat);
  }

  private byte[] runNativeDebayering(final bst_ims100_img_t image) throws IOException {
    // Run debayering and possibly process further
    LOGGER.log(Level.FINE, String.format("Allocating native buffer for debayered image"));
    final ByteBuffer debayeredImageData = ByteBuffer.allocateDirect(
          (int)image.getData_n() * 3 * bitdepth/8);
    byte[] rgbData = new byte[debayeredImageData.capacity()];
    LOGGER.log(Level.FINE, String.format("Debayering the image"));
    if (ims100_api.bst_ims100_img_debayer(image, debayeredImageData, 1, 1, 1, (short)bitdepth)
            != bst_ret_t.BST_RETURN_SUCCESS) {
      LOGGER.log(Level.WARNING, String.format("bst_ims100_img_debayer failed"));
      throw new IOException("bst_ims100_img_debayer failed");
    }
    ((ByteBuffer) (((Buffer)debayeredImageData).clear())).get(rgbData);
    return rgbData;
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

  private byte[] convertImage(BufferedImage image, final PictureFormat targetFormat) throws
      IOException
  {
    int i;
    byte[] ret = null;

    final ByteArrayOutputStream stream = new ByteArrayOutputStream();

    if (targetFormat.equals(PictureFormat.RGB24)) {
      final int w = image.getWidth();
      final int h = image.getHeight();
      final int[] rgba = image.getRGB(0, 0, w, h, null, 0, w);
      ret = new byte[rgba.length * 3];
      for (i = 0; i < rgba.length; ++i) {
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
      throw new IOException(targetFormat + " format not supported.");
    }
    return ret;
  }

  private BufferedImage rgbDataToBufferedImage(final byte[] rgbData, int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    int pixel;
    int bytesPerColor = bitdepth/8;
    int i = 0;
    for(int y = 0; y < height; y++) {
      for(int x = 0; x < width; x++) {
        /*
        * bitdepth = 8 --> bytesPerColor = 1
        * This is an 8-bit per channel RGB array so,
        * the rgbData array contains the data in {[R,G,B], [R,G,B], ...} format
        * so we get all indexes in multiples of 3
        *
        * bitdepth = 16 --> bytesPerColor = 2
        * This is a 16-bit per channel RGB array so,
        * the rgbData array contains the data in {[RR,GG,BB], [RR,GG,BB], ...} format in LittleEndian order
        * we add an offset of 1 to each channel index to get the MSB
        * i.e. R = rgbData[1]
        *      G = rgbData[3]
        *      B = rgbData[5]
        * and we get all indexes in multiples of 6 (because the array is twice the size)
        */
        pixel =
            (((int)rgbData[(1 * bytesPerColor - 1) + (3 * i * bytesPerColor)]) & 0xFF) << 16
          | (((int)rgbData[(2 * bytesPerColor - 1) + (3 * i * bytesPerColor)]) & 0xFF) << 8
          | (((int)rgbData[(3 * bytesPerColor - 1) + (3 * i * bytesPerColor)]) & 0xFF);
        ++i;
        image.setRGB(x, y, pixel);
      }
    }
    return image;
  }
}
