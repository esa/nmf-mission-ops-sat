/**
 * 	This file is part of Kayak.
 *
 *	Kayak is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU Lesser General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	Kayak is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public License
 *	along with Kayak.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.github.kayak.core;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A RAWConnection extends the {@link SocketcandConnection} and adds methods
 * that bring a socketcand in BCM mode. Frames are delivered asynchronously
 * through an own thread.
 * @author Jan-Niklas Meier <dschanoeh@googlemail.com>
 *
 */
public class BCMConnection extends SocketcandConnection {

    private static final Logger logger = Logger.getLogger(BCMConnection.class.getName());

    private Socket socket;
    private OutputStreamWriter output;
//    private DataOutputStream output;
//    private OutputStream fastOut;
    private Thread thread;
    private InputStreamReader input;
    private Boolean connected = false;

    private Runnable runnable = new Runnable() {

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder(40);

            while (true) {
                if(Thread.interrupted())
                    return;

                try {
                    String frame = getElement();

                    String[] fields = frame.split("\\s");

                    /* We received a frame */
                    if (fields[1].equals("frame")) {
                        try {
                            sb.setLength(0);
                            for (int i = 4; i < fields.length-1; i++) {
                                sb.append(fields[i]);
                            }
                            Frame f;

                            if(fields[2].length() <= 3) {
                                f = new Frame(Integer.valueOf(fields[2], 16), false, Util.hexStringToByteArray(sb.toString()));
                            } else {
                                f = new Frame(Integer.valueOf(fields[2], 16), true, Util.hexStringToByteArray(sb.toString()));
                            }

                            int pos = 0;
                            for(;pos<fields[3].length();pos++) {
                                if(fields[3].charAt(pos) =='.')
                                    break;
                            }
                            long timestamp = 1000000 * Long.parseLong(fields[3].substring(0, pos)) + Long.parseLong(fields[3].substring(pos+1));
                            f.setTimestamp(timestamp);
                            FrameListener receiver = getListener();
                            if (receiver != null) {
                                receiver.newFrame(f);
                            }

                        } catch (Exception ex) {
                            logger.log(Level.WARNING, "Could not properly deliver CAN frame", ex);
                        }
                    } else if (fields[1].equals("error")) {
                        logger.log(Level.WARNING, "Received error from socketcand: {0}", frame);
                    }
                } catch(InterruptedException ex) {
                    logger.log(Level.WARNING, "Interrupted exception. Shutting down connection thread");
                    return;
                } catch (IOException ex) {
                    /*
                     * A read from the socket may time out if there are very few frames.
                     * this will cause an IOException. This is ok so we will ignore these
                     * exceptions
                     */
                }
            }
        }
    };

    public Boolean isConnected() {
        return connected;
    }

    public BCMConnection(BusURL url) {
        this.host = url.getHost();
        this.port = url.getPort();
        this.busName = url.getBus();
    }

    public void open() {
        InetSocketAddress address = new InetSocketAddress(host, port);

        try {
            socket = new Socket();
            socket.connect(address);
            socket.setSoTimeout(1000);
            socket.setTcpNoDelay(Boolean.TRUE);

            input = new InputStreamReader(socket.getInputStream(), "ASCII");
            setInput(input);
            output = new OutputStreamWriter(socket.getOutputStream(), "ASCII");
//            output = new DataOutputStream(socket.getOutputStream());
             
//            fastOut = socket.getOutputStream();

            String ret = getElement();
            if (!ret.equals("< hi >")) {
                logger.log(Level.SEVERE, "Did not receive greeting from host.");
                return;
            }

//            output.write("< open " + busName + " >");
//            output.flush();
            send("< open " + busName + " >");

            ret = getElement();
            if (!ret.equals("< ok >")) {
                logger.log(Level.SEVERE, "Could not open bus");
                return;
            }
            socket.setSoTimeout(100);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "IOException while creating the socket.", e);
            return;
        }

        /* Start worker thread for frame reception */
        thread = new Thread(runnable);
        thread.setName("BCMConnection thread");
        thread.start();
        connected = true;
    }

    public void close() {
        if (thread != null && thread.isAlive()) {
            try {
                thread.interrupt();
                thread.join();
            } catch (Exception e) {
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
        }
        connected = false;
    }

    private synchronized void send(String s) {
        try {
            output.write(s);
//            output.writeBytes(s);
            output.flush();
        } catch(IOException ex) {
            logger.log(Level.WARNING,"IOException while sending data.", ex);
        }
    }

    public void subscribeTo(int id, boolean extended, int sec, int usec) {
        StringBuilder sb = new StringBuilder(30);
        sb.append("< subscribe ");
        sb.append(String.valueOf(sec));
        sb.append(' ');
        sb.append(String.valueOf(usec));
        sb.append(' ');
        if(extended) {
            sb.append(String.format("%08x", id));
        } else {
            sb.append(String.format("%03x", id));
        }
        sb.append(" >");
        send(sb.toString());
    }

    public void unsubscribeFrom(int id, boolean extended) {
        StringBuilder sb = new StringBuilder(30);
        sb.append("< unsubscribe ");
        if(extended) {
            sb.append(String.format("%08x", id));
        } else {
            sb.append(String.format("%03x", id));
        }
        sb.append(" >");
        send(sb.toString());
    }

    public void addSendJob(int id, boolean extended, byte[]data, int sec, int usec) {
        StringBuilder sb = new StringBuilder(40);
        sb.append("< add ");
        sb.append(Integer.toString(sec));
        sb.append(' ');
        sb.append(Integer.toString(usec));
        sb.append(' ');
        if(extended) {
            sb.append(String.format("%08x", id));
        } else {
            sb.append(String.format("%03x", id));
        }
        sb.append(' ');
        sb.append(Integer.toString(data.length));
        sb.append(' ');
        sb.append(Util.byteArrayToHexString(data, true));
        sb.append(" >");
        send(sb.toString());
    }

    public void removeSendJob(int id, boolean extended) {
        StringBuilder sb = new StringBuilder(40);
        sb.append("< delete ");
        if(extended) {
            sb.append(String.format("%08x", id));
        } else {
            sb.append(String.format("%03x", id));
        }
        sb.append(" >");
        send(sb.toString());
    }

    public void sendFrame(Frame f) {
        
        // We can prepare the whole string!
        // Include a prepare method in the Frame class that does that
        // Here, we would just need to have a if condition to check if it was prepared
        // if so, then use the prepared 
        
        if(!f.isPrepared()){
            StringBuilder sb = new StringBuilder(50);
            sb.append("< send ");
            if(f.isExtended()) {
            sb.append(String.format("%08x", f.getIdentifier()));
            } else {
            sb.append(String.format("%03x", f.getIdentifier()));
            }
            sb.append(' ');
            sb.append(Integer.toString(f.getLength()));
            sb.append(' ');
            sb.append(Util.byteArrayToHexString(f.getData(), true));
            sb.append(" >");
            send(sb.toString());
        }else{
            send(f.getPreparedString());
        }
        
        
/*        
//        long start = System.nanoTime();
            StringBuilder sb = new StringBuilder(50);
//        long partial1 = System.nanoTime();
            sb.append("< send ");
//        long partial2 = System.nanoTime();
            if(f.isExtended()) {
            sb.append(String.format("%08x", f.getIdentifier()));
            } else {
            sb.append(String.format("%03x", f.getIdentifier()));
            }
//        long partial3 = System.nanoTime();
            sb.append(' ');
//        long partial4 = System.nanoTime();
            sb.append(Integer.toString(f.getLength()));
//        long partial5 = System.nanoTime();
            sb.append(' ');
            // Maybe we can delegate this part to the Frame class
            // This would force it to happen before and not during the send
            // of the Frame
//        long partial6 = System.nanoTime();
            sb.append(Util.byteArrayToHexString(f.getData(), true));
//        long partial7 = System.nanoTime();
            sb.append(" >");
            //        logger.log(Level.INFO, sb.toString());
//        long partial8 = System.nanoTime();
            send(sb.toString());
//        long partial9 = System.nanoTime();
   */         
/*
            logger.log(Level.INFO,
                    "Times:\n" + 
                    "1: " + (partial1-start) + "\n" +
                    "2: " + (partial2-start) + "\n" +
                    "3: " + (partial3-start) + "\n" +
                    "4: " + (partial4-start) + "\n" +
                    "5: " + (partial5-start) + "\n" +
                    "6: " + (partial6-start) + "\n" +
                    "7: " + (partial7-start) + "\n" +
                    "8: " + (partial8-start) + "\n" +
                    "9: " + (partial9-start) + "\n");
  */      
        
            /*
        try {
            output.writeBytes("< send ");
            if(f.isExtended()) {
                output.writeBytes(String.format("%08x", f.getIdentifier()));
            } else {
                output.writeBytes(String.format("%03x", f.getIdentifier()));
            }
            output.writeBytes(" ");
//            output.writeChar(' ');
            output.writeBytes(Integer.toString(f.getLength()));
            output.writeBytes(" ");
//            output.writeChar(' ');
//            output.flush();
//            fastOut.write(f.getData());
//            fastOut.flush();

            output.writeBytes(Util.byteArrayToHexString(f.getData(), true));
            output.writeBytes(" >");
            //        logger.log(Level.INFO, sb.toString());
//            send(sb.toString());

            output.flush();
        } catch (IOException ex) {
            Logger.getLogger(BCMConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
            */
        
    }

}
