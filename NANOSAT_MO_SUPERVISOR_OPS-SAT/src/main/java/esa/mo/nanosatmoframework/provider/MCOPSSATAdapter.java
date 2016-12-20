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

import esa.mo.com.impl.util.GMVServicesConsumer;
import esa.mo.helpertools.helpers.HelperAttributes;
import esa.mo.nanosatmoframework.MCRegistration;
import esa.mo.nanosatmoframework.MonitorAndControlNMFAdapter;
import esa.mo.sm.impl.util.ShellCommander;
import esa.mo.transport.can.opssat.CANReceiveInterface;
import esa.mo.transport.can.opssat.CFPFrameHandler;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.provider.MALInteraction;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.structures.Union;
import org.ccsds.moims.mo.mc.action.structures.ActionDefinitionDetails;
import org.ccsds.moims.mo.mc.action.structures.ActionDefinitionDetailsList;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetails;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetailsList;
import org.ccsds.moims.mo.mc.structures.ArgumentDefinitionDetails;
import org.ccsds.moims.mo.mc.structures.ArgumentDefinitionDetailsList;
import org.ccsds.moims.mo.mc.structures.AttributeValueList;
import org.ccsds.moims.mo.mc.structures.ConditionalReferenceList;
import org.ccsds.moims.mo.mc.structures.Severity;
import org.ccsds.moims.mo.opssat_pf.gps.consumer.GPSAdapter;

// Specific OPS-SAT Monitoring and Control
public class MCOPSSATAdapter extends MonitorAndControlNMFAdapter {

    private CFPFrameHandler handler;

    private static final String PARAMETER_CURRENT_PARTITION = "CurrentPartition";
    private static final String CMD_CURRENT_PARTITION = "mount -l | grep \"on / \" | grep -o 'mmc.*[0-9]p[0-9]'";

    private static final String PARAMETER_LINUX_VERSION = "LinuxVersion";
    private static final String CMD_LINUX_VERSION = "uname -a";

    private static final String PARAMETER_CAN_RATE = "CANDataRate";

    private static final String ACTION_GPS_SENTENCE = "GPS_Sentence";

    private final ShellCommander shellCommander = new ShellCommander();

    private GMVServicesConsumer gmvServicesConsumer;

    @Override
    public void initialRegistrations(MCRegistration registration) {
        registration.setMode(MCRegistration.RegistrationMode.DONT_UPDATE_IF_EXISTS);

        // ------------------ Parameters ------------------
        ParameterDefinitionDetailsList defs = new ParameterDefinitionDetailsList();
        defs.add(new ParameterDefinitionDetails(
                new Identifier(PARAMETER_CURRENT_PARTITION),
                "The Current partition where the OS is running.",
                Union.STRING_SHORT_FORM.byteValue(),
                "",
                false,
                new Duration(10),
                null,
                null
        ));

        defs.add(new ParameterDefinitionDetails(
                new Identifier(PARAMETER_LINUX_VERSION),
                "The version of the software.",
                Union.STRING_SHORT_FORM.byteValue(),
                "",
                false,
                new Duration(10),
                null,
                null
        ));

        defs.add(new ParameterDefinitionDetails(
                new Identifier(PARAMETER_CAN_RATE),
                "The data rate on the can bus.",
                Union.DOUBLE_SHORT_FORM.byteValue(),
                "Messages/sec",
                false,
                new Duration(4),
                null,
                null
        ));

        try {
            handler = new CFPFrameHandler(new CANReceiveInterface() {
                @Override
                public void receive(byte[] message) {
                    // Do nothing
                }

            });
//            handler.init();
        } catch (IOException ex) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }

        registration.registerParameters(defs);

        ActionDefinitionDetailsList actionDefs = new ActionDefinitionDetailsList();

        ArgumentDefinitionDetailsList arguments1 = new ArgumentDefinitionDetailsList();
        {
            Byte rawType = Attribute._STRING_TYPE_SHORT_FORM;
            String rawUnit = "NMEA sentence identifier";
            ConditionalReferenceList conversionCondition = null;
            Byte convertedType = null;
            String convertedUnit = null;

            arguments1.add(new ArgumentDefinitionDetails(rawType, rawUnit, conversionCondition, convertedType, convertedUnit));
        }

        ActionDefinitionDetails actionDef1 = new ActionDefinitionDetails(
                new Identifier(ACTION_GPS_SENTENCE),
                "Injects the NMEA sentence identifier into the CAN bus.",
                Severity.INFORMATIONAL,
                new UShort(0),
                arguments1,
                null
        );

        actionDefs.add(actionDef1);
        LongList actionObjIds = registration.registerActions(actionDefs);

        // Start the GMV consumer
        gmvServicesConsumer = new GMVServicesConsumer();
        gmvServicesConsumer.init();

        /*        
        ShellCommander shell = new ShellCommander();
        String output = shell.runCommandAndGetOutputMessage("./led_test.sh");
        Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, "Output: " + output);
         */

 /*        
        ShellCommander shell = new ShellCommander();
        String ttyDevice = "/dev/ttyUSB0";
        String baudRate = "115200";
        String output = shell.runCommandAndGetOutputMessage("microcom -s " + baudRate + " " + ttyDevice);
        Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, "Output: " + output);
         */

 /*
        try {
 
//            gmvServicesConsumer.getGPSNanomindService().getGPSNanomindStub().getGPSData("GPGGALONG", new MCGPSAdapter());
            gmvServicesConsumer.getGPSNanomindService().getGPSNanomindStub().getGPSData("log gpggalonga\n", new MCGPSAdapter());
//            gmvServicesConsumer.getPowerNanomindService().getPowerNanomindStub().powerOnSBandTX();
        } catch (MALInteractionException ex) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MALException ex) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
         */
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
    public Boolean onSetValue(Identifier identifier, Attribute value) {
        return false;  // to confirm that no variable was set
    }

    @Override
    public UInteger actionArrived(Identifier name, AttributeValueList attributeValues, Long actionInstanceObjId, boolean reportProgress, MALInteraction interaction) {
        if (ACTION_GPS_SENTENCE.equals(name.getValue())) {
            try {
                gmvServicesConsumer.getGPSNanomindService().getGPSNanomindStub().getGPSData("GPGGALONG", new MCGPSAdapter());
            } catch (MALInteractionException ex) {
                Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MALException ex) {
                Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null;
        }

        return new UInteger(1);  // Action service not integrated
    }

    private class MCGPSAdapter extends GPSAdapter {

        @Override
        public void getGPSDataResponseReceived(org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader, String data, java.util.Map qosProperties) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, "1. Data: " + data);
        }

        @Override
        public void getGPSDataUpdateReceived(org.ccsds.moims.mo.mal.transport.MALMessageHeader msgHeader, String data, java.util.Map qosProperties) {
            Logger.getLogger(MCOPSSATAdapter.class.getName()).log(Level.INFO, "2. Data: " + data);
        }
    }

}
