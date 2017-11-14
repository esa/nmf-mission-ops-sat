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
package esa.mo.nmf.provider;

import esa.mo.com.impl.util.GMVServicesConsumer;
import esa.mo.helpertools.helpers.HelperAttributes;
import esa.mo.nmf.MCRegistration;
import esa.mo.nmf.MonitorAndControlNMFAdapter;
import esa.mo.sm.impl.util.ShellCommander;
import esa.mo.transport.can.opssat.CANReceiveInterface;
import esa.mo.transport.can.opssat.CFPFrameHandler;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.provider.MALInteraction;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.structures.Union;
import org.ccsds.moims.mo.mc.action.structures.ActionDefinitionDetails;
import org.ccsds.moims.mo.mc.action.structures.ActionDefinitionDetailsList;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetails;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetailsList;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterRawValueList;
import org.ccsds.moims.mo.mc.structures.ArgumentDefinitionDetails;
import org.ccsds.moims.mo.mc.structures.ArgumentDefinitionDetailsList;
import org.ccsds.moims.mo.mc.structures.AttributeValue;
import org.ccsds.moims.mo.mc.structures.AttributeValueList;
import org.ccsds.moims.mo.mc.structures.ConditionalConversionList;
import org.ccsds.moims.mo.opssat_pf.gps.consumer.GPSAdapter;
import org.ccsds.opssat.gmv.mc.aggregation.body.GetValueResponse;
import org.ccsds.opssat.gmv.mc.aggregation.structures.AggregationValueList;

// Specific OPS-SAT Monitoring and Control
public class MCOPSSATAdapter extends MonitorAndControlNMFAdapter {

    private CFPFrameHandler handler;

    private final static String DATE_PATTERN = "dd MMM yyyy HH:mm:ss.SSS";

    private static final String PARAMETER_CURRENT_PARTITION = "CurrentPartition";
    private static final String CMD_CURRENT_PARTITION = "mount -l | grep \"on / \" | grep -o 'mmc.*[0-9]p[0-9]'";

    private static final String PARAMETER_LINUX_VERSION = "LinuxVersion";
    private static final String CMD_LINUX_VERSION = "uname -a";
    private static final String CMD_LINUX_REBOOT = "reboot";

    private static final String PARAMETER_CAN_RATE = "CANDataRate";

    private static final String ACTION_GPS_SENTENCE = "GPS_Sentence";
    private static final String ACTION_REBOOT = "Reboot_MityArm";
    private static final String ACTION_CLOCK_SET_TIME = "Clock.setTimeUsingDeltaMilliseconds";

    private final ShellCommander shellCommander = new ShellCommander();

    private GMVServicesConsumer gmvServicesConsumer;

    @Override
    public void initialRegistrations(MCRegistration registration) {
        registration.setMode(MCRegistration.RegistrationMode.DONT_UPDATE_IF_EXISTS);

        // ------------------ Parameters ------------------
        ParameterDefinitionDetailsList defs = new ParameterDefinitionDetailsList();
        IdentifierList paramIdentifiers = new IdentifierList();

        defs.add(new ParameterDefinitionDetails(
                "The Current partition where the OS is running.",
                Union.STRING_SHORT_FORM.byteValue(),
                "",
                false,
                new Duration(10),
                null,
                null
        ));
        paramIdentifiers.add(new Identifier(PARAMETER_CURRENT_PARTITION));

        defs.add(new ParameterDefinitionDetails(
                "The version of the software.",
                Union.STRING_SHORT_FORM.byteValue(),
                "",
                false,
                new Duration(10),
                null,
                null
        ));
        paramIdentifiers.add(new Identifier(PARAMETER_LINUX_VERSION));

        defs.add(new ParameterDefinitionDetails(
                "The data rate on the can bus.",
                Union.DOUBLE_SHORT_FORM.byteValue(),
                "Messages/sec",
                false,
                new Duration(4),
                null,
                null
        ));
        paramIdentifiers.add(new Identifier(PARAMETER_CAN_RATE));

        try {
            handler = new CFPFrameHandler(new CANReceiveInterface() {
                @Override
                public void receive(byte[] message) {
                    // Do nothing
                }

            });
            handler.init();
        } catch (IOException ex) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }

        registration.registerParameters(paramIdentifiers, defs);

        ActionDefinitionDetailsList actionDefs = new ActionDefinitionDetailsList();
        IdentifierList actionIdentifiers = new IdentifierList();

        ArgumentDefinitionDetailsList arguments1 = new ArgumentDefinitionDetailsList();
        {
            Byte rawType = Attribute._STRING_TYPE_SHORT_FORM;
            String rawUnit = "NMEA sentence identifier";
            ConditionalConversionList conditionalConversions = null;
            Byte convertedType = null;
            String convertedUnit = null;

            arguments1.add(new ArgumentDefinitionDetails(rawType, rawUnit, conditionalConversions, convertedType, convertedUnit));
        }

        ActionDefinitionDetails actionDef1 = new ActionDefinitionDetails(
                "Injects the NMEA sentence identifier into the CAN bus.",
                new UOctet((short) 0),
                new UShort(0),
                arguments1,
                null
        );

        ArgumentDefinitionDetailsList arguments2 = new ArgumentDefinitionDetailsList();
        {
            Byte rawType = Attribute._LONG_TYPE_SHORT_FORM;
            String rawUnit = "milliseconds";
            ConditionalConversionList conditionalConversions = null;
            Byte convertedType = null;
            String convertedUnit = null;

            arguments2.add(new ArgumentDefinitionDetails(rawType, rawUnit, conditionalConversions, convertedType, convertedUnit));
        }

        ActionDefinitionDetails actionDef2 = new ActionDefinitionDetails(
                "Sets the clock using a diff between the on-board time and the desired time.",
                new UOctet((short) 0),
                new UShort(0),
                arguments2,
                null
        );

        ActionDefinitionDetails actionDef3 = new ActionDefinitionDetails(
                "Reboots the mityArm.",
                new UOctet((short) 0),
                new UShort(0),
                new ArgumentDefinitionDetailsList(),
                null
        );

        actionDefs.add(actionDef1);
        actionDefs.add(actionDef2);
        actionDefs.add(actionDef3);
        actionIdentifiers.add(new Identifier(ACTION_GPS_SENTENCE));
        actionIdentifiers.add(new Identifier(ACTION_CLOCK_SET_TIME));
        actionIdentifiers.add(new Identifier(ACTION_REBOOT));

        LongList actionObjIds = registration.registerActions(actionIdentifiers, actionDefs);

        // Start the GMV consumer
        gmvServicesConsumer = new GMVServicesConsumer();
        gmvServicesConsumer.init();

        LongList ids = new LongList();
        ids.add((long) 1);
        try {
            GetValueResponse values = gmvServicesConsumer.getAggregationNanomindService().getAggregationNanomindStub().getValue(ids);
            AggregationValueList agValues = values.getBodyElement1();
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, "Values: " + agValues.toString());
        } catch (MALInteractionException ex) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MALException ex) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*
            long startTime = System.currentTimeMillis();
            CameraSerialPortOPSSAT camera = new CameraSerialPortOPSSAT();
            long delta1 = System.currentTimeMillis() - startTime;
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Time 1: " + delta1);
            camera.init();
            long delta2 = System.currentTimeMillis() - startTime - delta1;
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Time 2: " + delta2);
            
            try {
            String version = camera.getVersion();
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Version: " + version);
            
            long delta3 = System.currentTimeMillis() - startTime - delta2;
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Time 3: " + delta3);
            
            String status = camera.getStatus();
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Status: " + status);
            
            long delta4 = System.currentTimeMillis() - startTime - delta3;
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Time 4: " + delta4);
            
            String temperature = camera.getTemperature();
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Temperature: " + temperature);
            
            long delta5 = System.currentTimeMillis() - startTime - delta4;
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Time 5: " + delta5);
            
            
            byte[] raw = camera.takePiture();
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "The picture has been taken!");
            
            long delta6 = System.currentTimeMillis() - startTime - delta5;
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Time 6: " + delta6);
            
            FileOutputStream fos = new FileOutputStream("myFirstPicture.raw");
            fos.write(raw);
            fos.flush();
            fos.close();
            
            long delta7 = System.currentTimeMillis() - startTime - delta6;
            Logger.getLogger(CameraSerialPortOPSSAT.class.getName()).log(Level.INFO, "Time 7: " + delta7);
            
            } catch (IOException ex) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
            
         */
 /*
            int value1 = 0x1A04CFB8;
            int value2 = 0x15049065;
            int value3 = 0x150803DF;
            
            CFPFrameIdentifier aaaa1 = new CFPFrameIdentifier(value1);
            CFPFrameIdentifier aaaa2 = new CFPFrameIdentifier(value2);
            CFPFrameIdentifier aaaa3 = new CFPFrameIdentifier(value3);
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, "Check 1: " + aaaa1.toString());
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, "Check 2: " + aaaa2.toString());
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, "Check 3: " + aaaa3.toString());
         */
        try {
//            gmvServicesConsumer.getGPSNanomindService().getGPSNanomindStub().getGPSData("GPGGALONG", new MCGPSAdapter());
//            gmvServicesConsumer.getGPSNanomindService().getGPSNanomindStub().getGPSData("log gpggalonga\n", new MCGPSAdapter());
            gmvServicesConsumer.getGPSNanomindService().getGPSNanomindStub().asyncGetGPSData(new Blob("log gpggalonga\n".getBytes()), new MCGPSAdapter());
//            gmvServicesConsumer.getPowerNanomindService().getPowerNanomindStub().powerOnSBandTX();
        } catch (MALInteractionException ex) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MALException ex) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public Attribute onGetValue(Identifier identifier, Byte rawType) {
        if (PARAMETER_CURRENT_PARTITION.equals(identifier.getValue())) {
            String msg = shellCommander.runCommandAndGetOutputMessage(CMD_CURRENT_PARTITION);
            return (Attribute) HelperAttributes.javaType2Attribute(msg);
        }

        if (PARAMETER_LINUX_VERSION.equals(identifier.getValue())) {
            String msg = shellCommander.runCommandAndGetOutputMessage(CMD_LINUX_VERSION);
            return (Attribute) HelperAttributes.javaType2Attribute(msg);
        }

        if (PARAMETER_CAN_RATE.equals(identifier.getValue())) {
            if (handler == null) {
                return null;
            }

            return (Attribute) HelperAttributes.javaType2Attribute(handler.getRate());
        }

        return null;
    }

    @Override
    public Boolean onSetValue(IdentifierList identifiers, ParameterRawValueList values) {
        return false;  // to confirm that no variable was set
    }

    @Override
    public UInteger actionArrived(Identifier name, AttributeValueList attributeValues,
            Long actionInstanceObjId, boolean reportProgress, MALInteraction interaction) {
        if (ACTION_GPS_SENTENCE.equals(name.getValue())) {
            try {
                gmvServicesConsumer.getGPSNanomindService().getGPSNanomindStub().asyncGetGPSData(new Blob("GPGGALONG".getBytes()), new MCGPSAdapter());
            } catch (MALInteractionException ex) {
                Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MALException ex) {
                Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null; // Success!
        }

        if (ACTION_REBOOT.equals(name.getValue())) {
            ShellCommander shell = new ShellCommander();
            String output = shell.runCommandAndGetOutputMessage(CMD_LINUX_REBOOT);
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, "Output: " + output);

            return null; // Success!
        }

        if (ACTION_CLOCK_SET_TIME.equals(name.getValue())) {
            if (attributeValues.isEmpty()) {
                return new UInteger(0); // Error!
            }

            AttributeValue aVal = attributeValues.get(0); // Extract the delta!
            long delta = (Long) HelperAttributes.attribute2JavaType(aVal.getValue());

            String str = (new SimpleDateFormat(DATE_PATTERN)).format(new Date(System.currentTimeMillis() + delta));

            ShellCommander shell = new ShellCommander();
            shell.runCommand("date -s \"" + str + " UTC\" | hwclock --systohc");

            return null; // Success!
        }

        return new UInteger(1);  // Action service not integrated
    }

    private class MCGPSAdapter extends GPSAdapter {

        @Override
        public void getGPSDataAckReceived(org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader, java.util.Map qosProperties) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, "1. getGPSDataAckReceived()");
        }

        @Override
        public void getGPSDataResponseReceived(org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader,
                org.ccsds.moims.mo.mal.structures.Blob data, java.util.Map qosProperties) {
            try {
                Logger.getLogger(CFPFrameHandler.class.getName()).log(Level.INFO,
                        "2. getGPSDataResponseReceived() Data: "
                        + Arrays.toString(data.getValue()));
            } catch (MALException ex) {
                Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void getGPSDataAckErrorReceived(org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader,
                org.ccsds.moims.mo.mal.MALStandardError error, java.util.Map qosProperties) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, 
                    "3. getGPSDataAckErrorReceived()");
        }

        @Override
        public void getGPSDataResponseErrorReceived(org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader,
                org.ccsds.moims.mo.mal.MALStandardError error, java.util.Map qosProperties) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, 
                    "4. getGPSDataResponseErrorReceived()", error.toString());
        }

    }

}
