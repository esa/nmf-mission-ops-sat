/* 
 * MAL/SPP Binding for CCSDS Mission Operations Framework
 * Copyright (C) 2015 Deutsches Zentrum für Luft- und Raumfahrt e.V. (DLR).
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.dlr.gsoc.mo.malspp.transport;

import static de.dlr.gsoc.mo.malspp.transport.SPPTransport.MAX_SPACE_PACKET_SIZE;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.UShort;

public class Configuration {

    private static final Map<String, Map<AppId, Map>> mappingConfigurations = new HashMap<>();
    private final Map properties;

    // Mapping configuration parameter property names (per APID/APID qualifier)
    private static final String PROPERTY_AUTHENTICATION_ID = "de.dlr.gsoc.mo.malspp.AUTHENTICATION_ID";
    private static final String PROPERTY_DOMAIN = "de.dlr.gsoc.mo.malspp.DOMAIN";
    private static final String PROPERTY_NETWORK_ZONE = "de.dlr.gsoc.mo.malspp.NETWORK_ZONE";
    private static final String PROPERTY_PRIORITY = "de.dlr.gsoc.mo.malspp.PRIORITY";
    private static final String PROPERTY_PACKET_DATA_FIELD_SIZE_LIMIT = "de.dlr.gsoc.mo.malspp.PACKET_DATA_FIELD_SIZE_LIMIT";
    private static final String PROPERTY_SESSION_NAME = "de.dlr.gsoc.mo.malspp.SESSION_NAME";
    // Default mapping configuration parameter and timestamp values where applicable
    private static final Blob DEFAULT_AUTHENTICATION_ID = new Blob(new byte[] {});
    private static final IdentifierList DEFAULT_DOMAIN = new IdentifierList(0);
    private static final Identifier DEFAULT_NETWORK_ZONE = new Identifier("");
    private static final UInteger DEFAULT_PRIORITY = new UInteger(0);
    private static final Identifier DEFAULT_SESSION_NAME = new Identifier("");
    protected static final Time DEFAULT_TIMESTAMP = new Time(0);
    // Timeout for Space Packet sequences
    private static final String PROPERTY_TIMEOUT = "de.dlr.gsoc.mo.malspp.TIMEOUT";
    private static final long DEFAULT_TIMEOUT = 0;
    // 'Undocumented' configuration parameters to limit the range of used instance
    // identifiers
    private static final String PROPERTY_NUM_IDENTIFIERS = "de.dlr.gsoc.mo.malspp.NUM_IDENTIFIERS";
    private static final String PROPERTY_START_IDENTIFIER = "de.dlr.gsoc.mo.malspp.START_IDENTIFIER";
    private static final short DEFAULT_NUM_IDENTIFIERS = 256;
    private static final short DEFAULT_START_IDENTIFIER = 0;

    // Transport parameters needed by this layer (underlying layer may expect more)
    private static final String PROPERTY_MAPPING_CONFIGURATION_FILE = "de.dlr.gsoc.mo.malspp.MAPPING_CONFIGURATION_FILE";

    // Per endpoint QoS property names
    // PENDING: Property appendIdToUri not yet in specification.
    private static final String PROPERTY_APPEND_ID_TO_URI = "org.ccsds.moims.mo.malspp.appendIdToUri";
    // Per endpoint QoS property names (non-normative)
    private static final String PROPERTY_APID_QUALIFIER = "org.ccsds.moims.mo.malspp.apidQualifier";
    private static final String PROPERTY_APID = "org.ccsds.moims.mo.malspp.apid";
    // Per message QoS property names
    private static final String PROPERTY_AUTHENTICATION_ID_FLAG = "org.ccsds.moims.mo.malspp.authenticationIdFlag";
    private static final String PROPERTY_DOMAIN_FLAG = "org.ccsds.moims.mo.malspp.domainFlag";
    private static final String PROPERTY_NETWORK_ZONE_FLAG = "org.ccsds.moims.mo.malspp.networkZoneFlag";
    private static final String PROPERTY_PRIORITY_FLAG = "org.ccsds.moims.mo.malspp.priorityFlag";
    private static final String PROPERTY_SESSION_NAME_FLAG = "org.ccsds.moims.mo.malspp.sessionNameFlag";
    private static final String PROPERTY_TIMESTAMP_FLAG = "org.ccsds.moims.mo.malspp.timestampFlag";
    // Per message QoS property names (non-normative)
    private static final String PROPERTY_IS_TC_PACKET = "org.ccsds.moims.mo.malspp.isTcPacket";

    // Mapping configuration paramter property names (per APID/APID qualifier) for
    // encoding.
    // Only used to parse XML configuration file in order to forward those
    // properties to the
    // encoding layer. The encoding layer has to implement its own interpretation of
    // those properties.
    private static final String PROPERTY_TIME_CODE_FORMAT = "de.dlr.gsoc.mo.malspp.TIME_CODE_FORMAT";
    private static final String PROPERTY_TIME_EPOCH = "de.dlr.gsoc.mo.malspp.TIME_EPOCH";
    private static final String PROPERTY_TIME_EPOCH_TIMESCALE = "de.dlr.gsoc.mo.malspp.TIME_EPOCH_TIMESCALE";
    private static final String PROPERTY_TIME_UNIT = "de.dlr.gsoc.mo.malspp.TIME_UNIT";
    private static final String PROPERTY_FINE_TIME_CODE_FORMAT = "de.dlr.gsoc.mo.malspp.FINE_TIME_CODE_FORMAT";
    private static final String PROPERTY_FINE_TIME_EPOCH = "de.dlr.gsoc.mo.malspp.FINE_TIME_EPOCH";
    private static final String PROPERTY_FINE_TIME_EPOCH_TIMESCALE = "de.dlr.gsoc.mo.malspp.FINE_TIME_EPOCH_TIMESCALE";
    private static final String PROPERTY_FINE_TIME_UNIT = "de.dlr.gsoc.mo.malspp.FINE_TIME_UNIT";
    private static final String PROPERTY_DURATION_CODE_FORMAT = "de.dlr.gsoc.mo.malspp.DURATION_CODE_FORMAT";
    private static final String PROPERTY_DURATION_UNIT = "de.dlr.gsoc.mo.malspp.DURATION_UNIT";
    private static final String PROPERTY_VARINT_SUPPORTED = "de.dlr.gsoc.mo.malspp.VARINT_SUPPORTED";

    // Configuration XML attribure names
    private static final String XML_ATTR_APID_QUALIFIER = "apidQualifier";
    private static final String XML_ATTR_APID = "apid";
    private static final String XML_ATTR_TIME_UNIT = "unit";
    private static final String XML_ATTR_TIME_SCALE = "scale";

    // Error strings
    private static final String MALFORMED_XML = "Malformed XML configuration file";

    public Configuration(final Map properties) {
        this.properties = properties;
    }

    private String mappingConfigurationFile() {
        return (String) properties.get(PROPERTY_MAPPING_CONFIGURATION_FILE);
    }

    public Blob authenticationId() {
        // Represent the AUTHENTICATION_ID Blob as String of hexadecimal digits
        return properties.get(PROPERTY_AUTHENTICATION_ID) == null ? DEFAULT_AUTHENTICATION_ID
                : new Blob(hexToByte((String) properties.get(PROPERTY_AUTHENTICATION_ID)));
    }

    public IdentifierList domain() {
        IdentifierList domain;
        if (properties.get(PROPERTY_DOMAIN) != null) {
            domain = new IdentifierList();
            for (String domainPart : ((String) properties.get(PROPERTY_DOMAIN)).split("\\.")) {
                domain.add(new Identifier(domainPart));
            }
        } else {
            domain = DEFAULT_DOMAIN;
        }
        return domain;
    }

    public Identifier networkZone() {
        return properties.get(PROPERTY_NETWORK_ZONE) == null ? DEFAULT_NETWORK_ZONE
                : new Identifier((String) properties.get(PROPERTY_NETWORK_ZONE));
    }

    public int packetDataFieldSizeLimit() {
        int sizeLimitFromConfig = (new UShort(
                Integer.parseInt((String) properties.get(PROPERTY_PACKET_DATA_FIELD_SIZE_LIMIT)))).getValue();
        return sizeLimitFromConfig == 0 ? MAX_SPACE_PACKET_SIZE : sizeLimitFromConfig;
    }

    public UInteger priority() {
        return properties.get(PROPERTY_PRIORITY) == null ? DEFAULT_PRIORITY
                : new UInteger(Long.parseLong((String) properties.get(PROPERTY_PRIORITY)));
    }

    public Identifier sessionName() {
        return properties.get(PROPERTY_SESSION_NAME) == null ? DEFAULT_SESSION_NAME
                : new Identifier((String) properties.get(PROPERTY_SESSION_NAME));
    }

    protected long timeout() {
        return properties.get(PROPERTY_TIMEOUT) == null ? DEFAULT_TIMEOUT
                : Long.parseLong((String) properties.get(PROPERTY_TIMEOUT));
    }

    protected short numIdentifiers() {
        return properties.get(PROPERTY_NUM_IDENTIFIERS) == null ? DEFAULT_NUM_IDENTIFIERS
                : Short.parseShort((String) properties.get(PROPERTY_NUM_IDENTIFIERS));
    }

    protected short startIdentifier() {
        return properties.get(PROPERTY_START_IDENTIFIER) == null ? DEFAULT_START_IDENTIFIER
                : Short.parseShort((String) properties.get(PROPERTY_START_IDENTIFIER));
    }

    public boolean appendIdToUri() {
        return getBooleanProperty(PROPERTY_APPEND_ID_TO_URI);
    }

    public short apid() {
        Object o = properties.get(PROPERTY_APID);
        if (o instanceof Integer) {
            return ((Integer) o).shortValue();
        }
        return Short.parseShort((String) o);
    }

    public int qualifier() {
        Object o = properties.get(PROPERTY_APID_QUALIFIER);
        if (o instanceof Integer) {
            return (Integer) o;
        }
        return Integer.valueOf((String) o);
    }

    public boolean authenticationIdFlag() {
        return getBooleanProperty(PROPERTY_AUTHENTICATION_ID_FLAG);
    }

    public boolean domainFlag() {
        return getBooleanProperty(PROPERTY_DOMAIN_FLAG);
    }

    public boolean networkZoneFlag() {
        return getBooleanProperty(PROPERTY_NETWORK_ZONE_FLAG);
    }

    public boolean priorityFlag() {
        return getBooleanProperty(PROPERTY_PRIORITY_FLAG);
    }

    public boolean sessionNameFlag() {
        return getBooleanProperty(PROPERTY_SESSION_NAME_FLAG);
    }

    public boolean timestampFlag() {
        return getBooleanProperty(PROPERTY_TIMESTAMP_FLAG);
    }

    public boolean isTCpacket() {
        return getBooleanProperty(PROPERTY_IS_TC_PACKET);
    }

    /**
     * Get a property flag from its name and return its boolean value. Accepts
     * Boolean objects and Strings as value in the property map.
     *
     * @param flagProperty
     * @return
     */
    private boolean getBooleanProperty(final String flagProperty) {
        Object propertyObject = properties == null ? null : properties.get(flagProperty);
        return (null == propertyObject) || (propertyObject instanceof Boolean ? (Boolean) propertyObject
                : Boolean.valueOf((String) propertyObject));
    }

    /**
     * Return a map containing all properties that are effective for a specific
     * application identified by APID qualifier and APID. Please note that the
     * application is not necessarily identified by the sending or receiving
     * endpoint but is dependent on the packet type (TM/TC).
     *
     * @param primaryQualifier The APID qualifier to use for retrieving the
     *                         effective configuration.
     * @param primaryApid      The APID to use for retrieving the effective
     *                         configuration.
     * @return A map containing the effective configuration.
     */
    protected synchronized Map getEffectiveProperties(final int primaryQualifier, final short primaryApid)
            throws MALException {
        // Read config file only once. If different configurations are needed for
        // multiple
        // transports, they can be told apart by the different config filename.
        String fn = mappingConfigurationFile();
        if (!mappingConfigurations.containsKey(fn)) {
            try {
                mappingConfigurations.put(fn, loadMappingConf(fn));
            } catch (XMLStreamException | FileNotFoundException ex) {
                throw new MALException(ex.getMessage(), ex);
            }
        }
        Map<AppId, Map> mappingConfs = mappingConfigurations.get(fn);
        // Mix it in this order so it is possible to dynamically reconfigure the mapping
        // configuration parameters.
        AppId appId = new AppId(primaryQualifier, primaryApid);

        if (mappingConfs.get(appId) == null) {
            // Logger.getLogger(Configuration.class.getName()).log(Level.INFO,
            // "The apid:" + primaryApid + " is not defined in the mcp file. Taking the
            // values for apid=-1");

            // Take default configuration (The file must have APID=-1)
            appId = new AppId(primaryQualifier, (short) -1);
        }

        return mix(mappingConfs.get(appId), properties);
    }

    private static Map<AppId, Map> loadMappingConf(final String filename)
            throws XMLStreamException, FileNotFoundException {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLEventReader xer = xif.createXMLEventReader(new FileInputStream(filename));
        LinkedList<Scope> scope = new LinkedList<>();
        Map<AppId, Map> config = null;
        Map<String, String> appConfig = null;
        while (xer.hasNext()) {
            XMLEvent e = xer.nextEvent();
            Scope s = scope.peekLast();
            switch (e.getEventType()) {
                case XMLEvent.START_DOCUMENT:
                    scope.add(Scope.START);
                    break;
                case XMLEvent.START_ELEMENT:
                    String name = e.asStartElement().getName().getLocalPart();
                    if (Scope.START == s && Scope.CONFIG.getName().equals(name)) {
                        config = new HashMap<>();
                        scope.add(Scope.CONFIG);
                    } else if (Scope.CONFIG == s && Scope.APP.getName().equals(name)) {
                        try {
                            int qualifier = Integer.parseInt(e.asStartElement()
                                    .getAttributeByName(QName.valueOf(XML_ATTR_APID_QUALIFIER)).getValue());
                            short apid = Short.parseShort(
                                    e.asStartElement().getAttributeByName(QName.valueOf(XML_ATTR_APID)).getValue());
                            appConfig = new HashMap<>();
                            config.put(new AppId(qualifier, apid), appConfig);
                            scope.add(Scope.APP);
                        } catch (NumberFormatException | NullPointerException ex) {
                            throw new XMLStreamException(MALFORMED_XML + " @ " + e.getLocation().toString(), ex);
                        }
                    } else if (Scope.APP == s) {
                        if (Scope.AUTHENTICATION_ID.getName().equals(name)) {
                            scope.add(Scope.AUTHENTICATION_ID);
                        } else if (Scope.DOMAIN.getName().equals(name)) {
                            scope.add(Scope.DOMAIN);
                        } else if (Scope.NETWORK_ZONE.getName().equals(name)) {
                            scope.add(Scope.NETWORK_ZONE);
                        } else if (Scope.PRIORITY.getName().equals(name)) {
                            scope.add(Scope.PRIORITY);
                        } else if (Scope.SESSION_NAME.getName().equals(name)) {
                            scope.add(Scope.SESSION_NAME);
                        } else if (Scope.PACKET_DATA_FIELD_SIZE_LIMIT.getName().equals(name)) {
                            scope.add(Scope.PACKET_DATA_FIELD_SIZE_LIMIT);
                        } else if (Scope.VARINT_SUPPORTED.getName().equals(name)) {
                            scope.add(Scope.VARINT_SUPPORTED);
                        } else if (Scope.TIME.getName().equals(name)) {
                            scope.add(Scope.TIME);
                        } else if (Scope.FINETIME.getName().equals(name)) {
                            scope.add(Scope.FINETIME);
                        } else if (Scope.DURATION.getName().equals(name)) {
                            scope.add(Scope.DURATION);
                        }
                    } else if (Scope.TIME == s) {
                        if (Scope.FORMAT.getName().equals(name)) {
                            String v = e.asStartElement().getAttributeByName(QName.valueOf(XML_ATTR_TIME_UNIT))
                                    .getValue();
                            appConfig.put(PROPERTY_TIME_UNIT, v);
                            scope.add(Scope.FORMAT);
                        } else if (Scope.EPOCH.getName().equals(name)) {
                            String v = e.asStartElement().getAttributeByName(QName.valueOf(XML_ATTR_TIME_SCALE))
                                    .getValue();
                            appConfig.put(PROPERTY_TIME_EPOCH_TIMESCALE, v);
                            scope.add(Scope.EPOCH);
                        }
                    } else if (Scope.FINETIME == s) {
                        if (Scope.FORMAT.getName().equals(name)) {
                            String v = e.asStartElement().getAttributeByName(QName.valueOf(XML_ATTR_TIME_UNIT))
                                    .getValue();
                            appConfig.put(PROPERTY_FINE_TIME_UNIT, v);
                            scope.add(Scope.FORMAT);
                        } else if (Scope.EPOCH.getName().equals(name)) {
                            String v = e.asStartElement().getAttributeByName(QName.valueOf(XML_ATTR_TIME_SCALE))
                                    .getValue();
                            appConfig.put(PROPERTY_FINE_TIME_EPOCH_TIMESCALE, v);
                            scope.add(Scope.EPOCH);
                        }
                    } else if (Scope.DURATION == s) {
                        if (Scope.FORMAT.getName().equals(name)) {
                            String v = e.asStartElement().getAttributeByName(QName.valueOf(XML_ATTR_TIME_UNIT))
                                    .getValue();
                            appConfig.put(PROPERTY_DURATION_UNIT, v);
                            scope.add(Scope.FORMAT);
                        }
                    }
                    break;
                case XMLEvent.CHARACTERS:
                    String data = e.asCharacters().getData().trim();
                    if (Scope.AUTHENTICATION_ID == s) {
                        appConfig.put(PROPERTY_AUTHENTICATION_ID, data);
                    } else if (Scope.DOMAIN == s) {
                        appConfig.put(PROPERTY_DOMAIN, data);
                    } else if (Scope.NETWORK_ZONE == s) {
                        appConfig.put(PROPERTY_NETWORK_ZONE, data);
                    } else if (Scope.PRIORITY == s) {
                        appConfig.put(PROPERTY_PRIORITY, data);
                    } else if (Scope.SESSION_NAME == s) {
                        appConfig.put(PROPERTY_SESSION_NAME, data);
                    } else if (Scope.PACKET_DATA_FIELD_SIZE_LIMIT == s) {
                        appConfig.put(PROPERTY_PACKET_DATA_FIELD_SIZE_LIMIT, data);
                    } else if (Scope.VARINT_SUPPORTED == s) {
                        appConfig.put(PROPERTY_VARINT_SUPPORTED, data);
                    } else if (Scope.FORMAT == s || Scope.EPOCH == s) {
                        Scope parentScope;
                        try {
                            parentScope = scope.get(scope.size() - 2);
                        } catch (IndexOutOfBoundsException ex) {
                            throw new XMLStreamException(MALFORMED_XML + " @ " + e.getLocation().toString());
                        }
                        if (Scope.FORMAT == s) {
                            if (Scope.TIME == parentScope) {
                                appConfig.put(PROPERTY_TIME_CODE_FORMAT, data);
                            } else if (Scope.FINETIME == parentScope) {
                                appConfig.put(PROPERTY_FINE_TIME_CODE_FORMAT, data);
                            } else if (Scope.DURATION == parentScope) {
                                appConfig.put(PROPERTY_DURATION_CODE_FORMAT, data);
                            }
                        } else if (Scope.EPOCH == s) {
                            if (Scope.TIME == parentScope) {
                                appConfig.put(PROPERTY_TIME_EPOCH, data);
                            } else if (Scope.FINETIME == parentScope) {
                                appConfig.put(PROPERTY_FINE_TIME_EPOCH, data);
                            }
                        }
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    if (!s.getName().equals(e.asEndElement().getName().getLocalPart())) {
                        throw new XMLStreamException(MALFORMED_XML + " @ " + e.getLocation().toString());
                    }
                    scope.removeLast();
                    break;
            }
        }
        return config;
    }

    /**
     * Convert the hexadecimal digits in the String parameter to a byte array. Two
     * hex digits represent one byte, therefore the number of digits has to be even.
     *
     * @param s Hexadecimal digits representing ta Blob.
     * @return Byte array represented by the hex digits.
     */
    private static byte[] hexToByte(final String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException();
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Short.parseShort(s.substring(i * 2, (i + 1) * 2), 16);
        }
        return data;
    }

    /**
     * Returns a newly created Map containing the union of two property maps. If a
     * value is present in both maps the value of newProperties will be taken. Any
     * map may be null.
     *
     * @param baseProperties Base property map.
     * @param newProperties  New property map. Will overwrite values of
     *                       baseProperties.
     * @return A new Map containing a mixture of baseProperties and newProperties.
     */
    protected static Map mix(final Map baseProperties, final Map newProperties) {
        Map props = null == baseProperties ? new HashMap() : new HashMap(baseProperties);
        if (null != newProperties) {
            props.putAll(newProperties);
        }
        return props;
    }

    static class AppId {

        public int qualifier;
        public short apid;

        AppId(int qualifier, short apid) {
            this.qualifier = qualifier;
            this.apid = apid;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final AppId other = (AppId) obj;
            if (this.qualifier != other.qualifier) {
                return false;
            }
            if (this.apid != other.apid) {
                return false;
            }
            return true;
        }
    }

    private enum Scope {

        START(""), CONFIG("config"), APP("app"), TIME("time"), FINETIME("fineTime"), DURATION("duration"),
        FORMAT("format"), EPOCH("epoch"), AUTHENTICATION_ID("authenticationId"), DOMAIN("domain"),
        NETWORK_ZONE("networkZone"), PRIORITY("priority"), SESSION_NAME("sessionName"),
        PACKET_DATA_FIELD_SIZE_LIMIT("packetDataFieldSizeLimit"), VARINT_SUPPORTED("varintSupported");

        private final String elementName;

        Scope(String elementName) {
            this.elementName = elementName;
        }

        public String getName() {
            return elementName;
        }
    }
}
