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

import esa.mo.helpertools.helpers.HelperTime;
import esa.mo.platform.impl.provider.gen.CameraAdapterInterface;
import esa.mo.platform.impl.util.CameraSerialPortOPSSAT;
import esa.opssat.camera.processing.OPSSATCameraDebayering;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.platform.camera.structures.Picture;
import org.ccsds.moims.mo.platform.camera.structures.PictureFormat;
import org.ccsds.moims.mo.platform.camera.structures.PixelResolution;
import org.ccsds.moims.mo.platform.camera.structures.PixelResolutionList;

/**
 *
 * @author Cesar Coelho
 */
public class CameraOPSSATAdapter implements CameraAdapterInterface {

    private final static Duration MINIMUM_DURATION = new Duration(10); // 10 seconds for now...
    private final static int IMAGE_LENGTH = 2048;
    private final static int IMAGE_WIDTH = 1944;
    private final CameraSerialPortOPSSAT bstCamera;

    public CameraOPSSATAdapter() throws IOException {
        bstCamera = new CameraSerialPortOPSSAT();
        bstCamera.init();
    }

    @Override
    public String getExtraInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PixelResolutionList getAvailableResolutions() {
        PixelResolutionList availableResolutions = new PixelResolutionList();
        availableResolutions.add(new PixelResolution(new UInteger(IMAGE_LENGTH), new UInteger(IMAGE_WIDTH)));

        return availableResolutions;
    }

    @Override
    public synchronized Picture getPicturePreview() {
        /*
        // Some data neds to go here:
        byte[] data = null;

        ImageIcon image = new ImageIcon(data);
        PixelResolution dimension = new PixelResolution();

        dimension.setHeight(new UInteger(image.getIconHeight()));
        dimension.setWidth(new UInteger(image.getIconWidth()));
         */

        final PixelResolution resolution = new PixelResolution(new UInteger(IMAGE_LENGTH), new UInteger(IMAGE_WIDTH));

        byte[] data = null;

        try {
            data = bstCamera.takePiture();
        } catch (IOException ex) {
            Logger.getLogger(CameraOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }

        Picture picture = new Picture();
        picture.setCreationDate(HelperTime.getTimestampMillis());
        picture.setContent(new Blob(data));
        picture.setDimension(resolution);
        picture.setFormat(PictureFormat.RAW);

        return picture;
    }

    @Override
    public synchronized Picture takePicture(final PixelResolution dimensions,
            final PictureFormat format, final Duration exposureTime) throws IOException {
        final Picture picture = new Picture();
        picture.setCreationDate(HelperTime.getTimestampMillis());
        byte[] data = bstCamera.takePiture();
        picture.setContent(new Blob(data));
        picture.setDimension(dimensions);
        picture.setFormat(PictureFormat.RAW);

        return picture;
    }

    @Override
    public BufferedImage getBufferedImageFromRaw(byte[] rawImage) {
        return OPSSATCameraDebayering.getBufferedImageFromBytes(rawImage);
    }

    @Override
    public Duration getMinimumPeriod() {
        return MINIMUM_DURATION;
    }

}
