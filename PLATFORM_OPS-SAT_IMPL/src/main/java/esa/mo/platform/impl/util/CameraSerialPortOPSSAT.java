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
package esa.mo.platform.impl.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import purejavacomm.CommPort;
import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;

/**
 *
 *
 */
public class CameraSerialPortOPSSAT {

    private final static int TIMEOUT = 500; // milliseconds

    private SerialPort serialPort;
//    private static final int BAUD_RATE = 115200;
    private static final String PORT_NAME = "ttyACM0";
    private boolean ready = false;

    private BufferedReader reader = null;
    private BufferedWriter writer = null;

    private static final String CMD_SHOW_VERSION = "show version";
    private static final String CMD_SHOW_TEMPERATURE = "show temperature";
    private static final String CMD_SHOW_STATUS = "show status";

    private static final String CMD_IMAGE_CLEAR_ALL = "imager clear all";
    private static final String CMD_IMAGE_EXPOSURE_TIME = "imager set exposure-time";
    private static final String CMD_IMAGE_SHOOT = "imager shoot";
    private static final String CMD_IMAGE_START_SHOOTING = "#";
    private static final String CMD_IMAGE_LIST = "imager list";

    private static final String CMD_RECONFIGURE = "reconfigure";

    public void init() {
        boolean isAvailable = this.checkIfAvailable();
        Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "1 ");

        if (!isAvailable) {
            // Please check if the Camera is turned on!
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.SEVERE,
                    "The port " + PORT_NAME + " is not available! Please check if the Camera is turned on.");
            return;
        }

        try {
            this.connect();
        } catch (PortInUseException ex) {
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.SEVERE, "The Port is already in use!", ex);
            return;
        } catch (NoSuchPortException ex) {
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.SEVERE, "The Port does not exist!", ex);
            return;
        }

        this.initIOStream();
        ready = true;
    }

    public boolean checkIfAvailable() {
        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();

        CommPortIdentifier portId = null;  // will be set if port found
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();

            if (pid.getPortType() == CommPortIdentifier.PORT_SERIAL
                    && pid.getName().equals(PORT_NAME)) {
                portId = pid;
                break;
            }
        }

        return portId != null;
    }

    public ArrayList<String> getSerialPortNamesAvailable() {
        final ArrayList<String> out = new ArrayList<String>();
        final Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();

        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();

            if (pid.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                out.add(pid.getName());
            }
        }

        return out;
    }

    private void connect() throws PortInUseException, NoSuchPortException {
        CommPortIdentifier id = CommPortIdentifier.getPortIdentifier(PORT_NAME);
        CommPort commPort = id.open("CameraController", TIMEOUT);

        //the CommPort object can be casted to a SerialPort object
        serialPort = (SerialPort) commPort;
    }

    private boolean initIOStream() {
        //return value for whether opening the streams is successful or not
        try {
            InputStream input = serialPort.getInputStream();
            OutputStream output = serialPort.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(input));
            writer = new BufferedWriter(new OutputStreamWriter(output));

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void disconnect() throws IOException {
        //close the serial port
        serialPort.close();
        reader.close();
        writer.close();
        ready = false;
    }

    private synchronized void writeData(final String str) {
        try {
            writer.write(str);
            writer.newLine();
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private synchronized String readData() {
        try {
            String str = reader.readLine();
            while(true){
                if (reader.ready()){
                    str += "\n" + reader.readLine();
                }else{
                    break;
                }
            }
            
            return str;
        } catch (IOException ex) {
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private boolean checkIfReady() throws IOException {
        if (ready) {
            return true;
        } else {
            throw new IOException("Camera is not ready!");
        }
    }

    public String getVersion() throws IOException {
        this.checkIfReady();
        this.writeData(CMD_SHOW_VERSION);
        return this.readData();
    }

    public String getTemperature() throws IOException {
        this.checkIfReady();
        this.writeData(CMD_SHOW_TEMPERATURE);
        return this.readData();
    }

    public String getStatus() throws IOException {
        this.checkIfReady();
        this.writeData(CMD_SHOW_STATUS);
        return this.readData();
    }

    public String clearAllImages() throws IOException {
        this.checkIfReady();
        this.writeData(CMD_IMAGE_CLEAR_ALL);
        return this.readData();
    }

    public String setExposureTime(final int time) throws IOException {
        this.checkIfReady();
        this.writeData(CMD_IMAGE_EXPOSURE_TIME + " " + String.valueOf(time));
        return this.readData();
    }

    public String reconfigure() throws IOException {
        this.checkIfReady();
        this.writeData(CMD_RECONFIGURE);
        return this.readData();
    }

    public String shoot() throws IOException {
        this.checkIfReady();

        int id = 5;
        int n_img = 5;

        this.writeData(CMD_IMAGE_SHOOT
                + " " + String.valueOf(id)
                + " " + String.valueOf(n_img));
        return this.readData();
    }

    public String startShooting() throws IOException {
        this.checkIfReady();
        this.writeData(CMD_IMAGE_START_SHOOTING);
        return this.readData();
    }

    public String listImages() throws IOException {
        this.checkIfReady();
        this.writeData(CMD_IMAGE_LIST);
        return this.readData();
    }

    private byte[] copyImage() {

        File file = new File("/dev/sda");
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            
            int bs = 4096;
            int off = 69632*bs;
            int len = 1944*bs;

            byte[] data = new byte[len];
            
            bis.skip(off);
            bis.read(data, 0, len);
            
            return data;

/*                    
            int content;
            while ((content = fis.read()) != -1) {
                // convert to char and display it
                System.out.print((char) content);
            }
*/
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        return null;
    }

    public byte[] takePiture() throws IOException {

        try {
            String out1 = this.clearAllImages();
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Out1: " + out1);
            
            Thread.sleep(3000);
            
            String out2 = this.setExposureTime(1);
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Out2: " + out2);
            
            Thread.sleep(3000);

            String out3 = this.reconfigure();
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Out3: " + out3);
            
            Thread.sleep(3000);

            String out4 = this.shoot();
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Out4: " + out4);
            
            Thread.sleep(3000);

            String out5 = this.startShooting();
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Out5: " + out5);
            
            Thread.sleep(5000);

            String out6 = this.listImages();
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Out6: " + out6);
            
            Thread.sleep(6000);

            return this.copyImage();
        } catch (InterruptedException ex) {
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }

}
