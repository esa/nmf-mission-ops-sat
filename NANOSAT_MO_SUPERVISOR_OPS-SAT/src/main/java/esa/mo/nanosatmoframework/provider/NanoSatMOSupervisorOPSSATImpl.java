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
package esa.mo.nanosatmoframework.provider;

import esa.mo.com.impl.util.COMServicesProvider;
import esa.mo.com.impl.util.GMVServicesConsumer;
import esa.mo.nanosatmoframework.MonitorAndControlNMFAdapter;
import esa.mo.nanosatmoframework.nanosatmosupervisor.NanoSatMOSupervisor;
import esa.mo.nmf.packager.PackageManagementBackendNMFPackage;
import esa.mo.platform.impl.util.PlatformServicesConsumer;
import esa.mo.platform.impl.util.PlatformServicesProviderOPSSAT;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.platform.camera.consumer.CameraAdapter;
import org.ccsds.moims.mo.platform.camera.structures.PictureFormat;
import org.ccsds.moims.mo.platform.camera.structures.PixelResolution;

/**
 * A Provider of MO services composed by COM, M&C and Platform services. Selects
 * the transport layer based on the selected values of the properties file and
 * initializes all services automatically. Provides configuration persistence,
 * therefore the last state of the configuration of the MO services will be kept
 * upon restart. Additionally, the NanoSat MO Framework implements an
 * abstraction layer over the Back-End of some MO services to facilitate the
 * monitoring of the business logic of the app using the NanoSat MO Framework.
 *
 * @author Cesar Coelho
 */
public class NanoSatMOSupervisorOPSSATImpl extends NanoSatMOSupervisor {

    private PlatformServicesProviderOPSSAT platformServicesOPSSAT;
    private GMVServicesConsumer gmvServicesConsumer;

    private final static int IMAGE_LENGTH = 2048;
    private final static int IMAGE_WIDTH = 1944;

    /**
     * NanoSat MO Supervisor for OPS-SAT
     *
     */
    public NanoSatMOSupervisorOPSSATImpl() {
        super(new MCOPSSATAdapter(),
                new PlatformServicesConsumer(),
                new PackageManagementBackendNMFPackage());

        DataReceivedAdapter adapter = new DataReceivedAdapter(new Long(0));
        PixelResolution resolution = new PixelResolution(new UInteger(IMAGE_LENGTH), new UInteger(IMAGE_WIDTH));
        try {
            super.getPlatformServices().getCameraService().takePicture(resolution, PictureFormat.RAW, new Duration(), adapter);

            // Set the transport layer
            // To be done...
            /*
            Logger rootLog = Logger.getLogger("");
            rootLog.setLevel( Level.FINEST );
            rootLog.getHandlers()[0].setLevel( Level.FINEST ); // Default console handler
             */
        } catch (MALInteractionException ex) {
            Logger.getLogger(NanoSatMOSupervisorOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MALException ex) {
            Logger.getLogger(NanoSatMOSupervisorOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void initPlatformServices(COMServicesProvider comServices) {

        /*
        // Initialize the consumers to the Nanomind
        gmvServicesConsumer = new GMVServicesConsumer();
        gmvServicesConsumer.init();

        try {
            platformServicesOPSSAT = new PlatformServicesProviderOPSSAT();
            platformServicesOPSSAT.init(comServices, gmvServicesConsumer);
        } catch (MALException ex) {
            Logger.getLogger(NanoSatMOSupervisorOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         */
    }

    /**
     * Main command line entry point.
     *
     * @param args the command line arguments
     * @throws java.lang.Exception If there is an error
     */
    public static void main(final String args[]) throws Exception {
        NanoSatMOSupervisorOPSSATImpl supervisor = new NanoSatMOSupervisorOPSSATImpl();
    }

    public class DataReceivedAdapter extends CameraAdapter {

        private final int STAGE_ACK = 1;
        private final int STAGE_RSP = 2;
        private final int TOTAL_STAGES = 2;
        private final Long actionInstanceObjId;

        DataReceivedAdapter(Long actionInstanceObjId) {
            this.actionInstanceObjId = actionInstanceObjId;
        }

        @Override
        public void takePictureAckReceived(org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader, java.util.Map qosProperties) {
        }

        @Override
        public void takePictureResponseReceived(org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader, org.ccsds.moims.mo.platform.camera.structures.Picture picture, java.util.Map qosProperties) {
            // The picture was received!

            // Store it in a file!
            if (picture.getFormat().equals(PictureFormat.RAW)) {
                try {
                    FileOutputStream fos = new FileOutputStream("myFirstPicture.raw");
                    fos.write(picture.getContent().getValue());
                    fos.flush();
                    fos.close();
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(NanoSatMOSupervisorOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(NanoSatMOSupervisorOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
                } catch (MALException ex) {
                    Logger.getLogger(NanoSatMOSupervisorOPSSATImpl.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            /*            
            BufferedImage img = null;

            try {
                ByteArrayInputStream byteArrayIS = new ByteArrayInputStream(picture.getContent().getValue());

                try {
                    BufferedImage image = new BufferedImage((int) picture.getDimension().getWidth().getValue(),
                            (int) picture.getDimension().getHeight().getValue(),
                            BufferedImage.TYPE_BYTE_BINARY);

                    WritableRaster raster = (WritableRaster) image.getData();

                    
                    raster.setPixels(0, 0, width, height, picture.getContent().getValue());

                    File outputfile = new File("saved.png");
                    ImageIO.write(image, "png", outputfile);
                    
                    /*
                   img = ImageIO.read(byteArrayIS);
                    File outputfile = new File("image.jpg");
                    ImageIO.write(img, "bmp", outputfile);
                } catch (IOException ex) {
                    Logger.getLogger(MCSnapNMFAdapter.class.getName()).log(Level.SEVERE, null, ex);
                }

            } catch (MALException ex) {
                Logger.getLogger(MCSnapNMFAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
             */
        }

        @Override
        public void takePictureAckErrorReceived(org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader, org.ccsds.moims.mo.mal.MALStandardError error, java.util.Map qosProperties) {
        }

        @Override
        public void takePictureResponseErrorReceived(org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader, org.ccsds.moims.mo.mal.MALStandardError error, java.util.Map qosProperties) {
        }

    }

}
