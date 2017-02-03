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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;
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

    private static final int SINGLE_IMAGE_ID = 1;
    private static final String CMD_SHOW_VERSION = "show version";
    private static final String CMD_SHOW_TEMPERATURE = "show temperature";
    private static final String CMD_SHOW_STATUS = "show status";

    private static final String CMD_IMAGE_CLEAR_ALL = "imager clear all";
    private static final String CMD_IMAGE_EXPOSURE_TIME = "imager set exposure-time";
    private static final String CMD_IMAGE_SHOOT = "imager shoot";
    private static final String CMD_IMAGE_START_SHOOTING = "#";
    private static final String CMD_IMAGE_LIST = "imager list";

    private static final String CMD_RECONFIGURE = "reconfigure";

    public void init() throws IOException {
        boolean isAvailable = this.checkIfAvailable();

        if (!isAvailable) {
            // Please check if the Camera is turned on!
            throw new IOException("The port " + PORT_NAME + " is not available! Please check if the Camera is turned on.");
        }

        try {
            this.connect();
        } catch (PortInUseException ex) {
            throw new IOException("The Port is already in use!");
        } catch (NoSuchPortException ex) {
            throw new IOException("The Port does not exist!");
        }

        this.initIOStream();
        ready = true;
    }

    public boolean checkIfAvailable() {
        final Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();

//        CommPortIdentifier portId = null;  // will be set if port found
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();

            if (pid.getPortType() == CommPortIdentifier.PORT_SERIAL
                    && pid.getName().equals(PORT_NAME)) {
//                portId = pid; // Found!
//                break;
                return true;
            }
        }
        
        return false;
//        return portId != null;
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
            while (true) {
                if (reader.ready()) {
                    str += "\n" + reader.readLine();
                } else {
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

        int id = SINGLE_IMAGE_ID;
        int n_img = 1;

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

    private long getImageOffset() throws IOException {
        this.checkIfReady();
        this.writeData(CMD_IMAGE_LIST);
        String output = this.readData();

        final Scanner scanner = new Scanner(output);

        long off = 0;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            // process the line
            String[] fields = line.split("offset");

            if (fields.length == 2) {
                String[] remainings = fields[0].split(" ");

                if (remainings.length > 4 && Integer.parseInt(remainings[1], 16) == SINGLE_IMAGE_ID) {
                    String pos_hex_value = remainings[4];
                    off = Long.parseLong(pos_hex_value, 16);
                    break;
                }
            }
        }

        scanner.close();

        return off;
    }

    private byte[] copyImage(long off) {
        try {
            RandomAccessFile file = new RandomAccessFile("/dev/sda", "r");

            try {
                file.seek(off * 4096);
            } catch (IOException ex) {
                Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.SEVERE, null, ex);
            }

            int bytesPerPixel = 2;
            int len = 2048 * 1944 * bytesPerPixel;
            byte[] data = new byte[len];

            int readNBytes = file.read(data, 0, len);

            /*
            for (int i = 0; i < 30; i++) {
                int intData = (int)data[i];
                System.out.print("Image raw pixel values are: " + intData + " "); // Use for debug only
            }
             */
            if (readNBytes != len) {
                throw new IOException("Didn't fully read the image!");
            }

            file.close();

            return data;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.SEVERE, null, ex);
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

            long offset = this.getImageOffset();
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Offset: " + offset);

            Thread.sleep(6000);

            return this.copyImage(offset);
        } catch (InterruptedException ex) {
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

}
