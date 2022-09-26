package esa.nmf.test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.imageio.ImageIO;

import org.ccsds.moims.mo.platform.camera.structures.PictureFormat;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import at.tugraz.ihf.opssat.ims100.bst_ims100_img_attr_t;
import at.tugraz.ihf.opssat.ims100.bst_ims100_img_config_t;
import at.tugraz.ihf.opssat.ims100.bst_ims100_img_t;
import at.tugraz.ihf.opssat.ims100.ims100_api;
import esa.mo.platform.impl.provider.opssat.CameraOPSSATAdapter;
import esa.mo.platform.impl.provider.softsim.PowerControlSoftSimAdapter;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CameraOPSSATTest {

  private static String IN_RAW_FILE = "src/test/resources/img_msec_1654059420250_3.ims_rgb";
  private static String IN_REF_FILE = "src/test/resources/img_msec_1654059420250_3.png";
  private static String OUT_PNG_FILE = "src/test/resources/testout.png";
  private static float MIN_SIMILARITY = 99.9f;
  private static float SIMILARITY_THRESHOLD = 100.0f - MIN_SIMILARITY;
  private static int COLOR_SIMILARITY_THRESHOLD = 1; // 1/256 => just under 0.5% difference

  /**
   * Based on Stack Overflow post by Sandip Ganguli and Sireesha K:
   * https://stackoverflow.com/questions/8567905/how-to-compare-images-for-similarity-using-java
   * 
   * Licensed under CC BY-SA 3.0: https://creativecommons.org/licenses/by-sa/3.0/
   */
  private static float compareImage(File fileA, File fileB) {
    float percentage = 0;
    try {
      // take buffer data from both image files //
      BufferedImage biA = ImageIO.read(fileA);
      BufferedImage biB = ImageIO.read(fileB);
      int count = 0;
      // compare data-buffer objects //
      if (biA.getHeight() == biB.getHeight() && biA.getWidth() == biB.getWidth()) {
        for (int y = 0; y < biA.getHeight(); y++) {
          for (int x = 0; x < biA.getWidth(); x++) {
            Color colorA = new Color(biA.getRGB(x, y));
            Color colorB = new Color(biB.getRGB(x, y));
            if (Math.abs(colorA.getRed() - colorB.getRed()) <= COLOR_SIMILARITY_THRESHOLD
                && Math.abs(colorA.getGreen() - colorB.getGreen()) <= COLOR_SIMILARITY_THRESHOLD
                && Math.abs(colorA.getBlue() - colorB.getBlue()) <= COLOR_SIMILARITY_THRESHOLD) {
              count += 1;
            }
          }
        }
        percentage = (count * 100) / (biA.getHeight() * biA.getWidth());
      } else {
        System.out.println("Both the images are not of same size");
      }

    } catch (Exception e) {
      System.out.println("Failed to compare image files ...");
    }
    return percentage;
  }

  @Test
  public void testDebayer() throws Throwable {
    PowerControlSoftSimAdapter simAdapter = new PowerControlSoftSimAdapter();
    CameraOPSSATAdapter adapter = new CameraOPSSATAdapter(simAdapter);
    bst_ims100_img_attr_t imageAttr = new bst_ims100_img_attr_t();
    bst_ims100_img_config_t imageConfig = new bst_ims100_img_config_t();
    ims100_api.bst_ims100_img_config_default(imageConfig);
    int nativeImageWidth = imageConfig.getCol_end() - imageConfig.getCol_start() + 1;
    int nativeImageHeight = imageConfig.getRow_end() - imageConfig.getRow_start() + 1;
    imageAttr.setWidth(nativeImageWidth);
    imageAttr.setHeight(nativeImageHeight);
    imageAttr.setOffx(0);
    imageAttr.setOffy(0);
    final int dataN = (int) (nativeImageWidth * nativeImageHeight);
    final bst_ims100_img_t image = new bst_ims100_img_t();
    final ByteBuffer imageData = ByteBuffer.allocateDirect(dataN * 2);
    try (RandomAccessFile rawFile = new RandomAccessFile(IN_RAW_FILE, "r");
        FileChannel inChannel = rawFile.getChannel();) {
      System.out.println("Reading the testfile: " + IN_RAW_FILE);
      Assert.assertEquals(inChannel.read(imageData), dataN * 2);
      imageData.flip();
    }
    image.setAttr(imageAttr);
    image.setData(imageData);
    image.setData_n(dataN);
    System.out.println("Running conversion...");
    byte[] picOut = adapter.processRawCameraPicture(PictureFormat.PNG, image);
    System.out.println("Dumping the file content to: " + OUT_PNG_FILE);
    File fout = new File(OUT_PNG_FILE);
    try (FileOutputStream fos = new FileOutputStream(fout)) {
      fos.write(picOut);
    }
    float similarity = compareImage(new File(OUT_PNG_FILE), new File(IN_REF_FILE));
    Assert.assertEquals(100.0f, similarity, SIMILARITY_THRESHOLD);
    new File(OUT_PNG_FILE).delete();
  }
}