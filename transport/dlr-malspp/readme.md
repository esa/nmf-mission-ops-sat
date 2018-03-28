CCSDS MO MAL/SPP Binding Prototype Implementation
=================================================
This projects contains a prototype implementation of the [MAL/SPP Binding] [CCSDS 524.1-R-1] in Java. It is assumed you are familiar with the [Space Packet Protocol][CCSDS 133.0-B-1] and the [CCSDS Mission Operations (MO) framework] [CCSDS 520.0-G-3] in general and the [MO Reference Model]
[CCSDS 520.1-M-1] and the [MO MAL] [CCSDS 521.0-B-2] in particular. If you want to work with the source code, knowledge of the [MAL Java API] [CCSDS 523.1-M-1] is mandatory.

The MAL/SPP Binding is a technology binding that allows the translation of MAL messages into a concrete representation using Space Packets and vice versa. The binding itself relies on a lower layer for actually sending the Space Packets.

[CCSDS 133.0-B-1]: http://public.ccsds.org/publications/archive/133x0b1c2.pdf
                   "Space Packet Protocol (September 2003)"
[CCSDS 524.1-R-1]: not yet published
                   "Mission Operations - Space Packet Binding (December 2014)"
[CCSDS 520.0-G-3]: http://public.ccsds.org/publications/archive/520x0g3.pdf
                   "Mission Operations Services Concept (December 2010)"
[CCSDS 520.1-M-1]: http://public.ccsds.org/publications/archive/520x1m1.pdf
                   "Mission Operations Reference Model (July 2010)"
[CCSDS 521.0-B-2]: http://public.ccsds.org/publications/archive/521x0b2e1.pdf
                   "Mission Operations Message Abstraction Layer (March 2013)"
[CCSDS 523.1-M-1]: http://public.ccsds.org/publications/archive/523x1m1.pdf
                   "Mission Operations Message Abstraction Layer - Java API (April 2013)"

Module structure
================
The MAL/SPP implementation consists of several artifacts:

* `DLR_MO_POM`  
   Top level POM file for the transport and encoding artifacts.
* `DLR_MO_MALSPP_ENCODING`  
   Responsible for encoding MAL message bodies. This artifact implements the optional Encoding API defined in [CCSDS 523.1-M-1] and thus can be used in transport protocols different from the Space Packet protocol. It is contained in the Java package `de.dlr.gsoc.mo.malspp.encoding`.
* `DLR_MO_MALSPP_TRANSPORT`  
   Responsible for sending and receiving Space Packets. This artifact implements the Transport API defined in [CCSDS 523.1-M-1] and is contained in the Java package `de.dlr.gsoc.mo.malspp.transport`. It connects the MAL layer with an underlying Space Packet transport layer (using the Space Packet Socket API) and uses the optional Encoding API for MAL message body encoding.

The following additional artifacts are part of the MO testbed, and technically not necessary for using the MAL/SPP Binding:

* `MOIMS_TESTBED_SPP`  
   Contains the FitNesse tests for evaluating MAL/SPP Binding implementations.
* `MOIMS_TESTBED_SPP_FRAMEWORK`  
   Contains the fixture code for evaluating MAL/SPP Binding implementations. It also contains the Space Packet Protocol Socket API and an example implementation using TCP as underlying transport mechanism.

Compilation and usage
=====================
This project relies on [Maven](http://maven.apache.org/) as build management system. Dependencies and settings common to the encoding and the transport artifact are defined in `DLR_MO_POM/pom.xml`. A reactor build containing encoding, transport and all necessary testbed modules is defined in the `./pom.xml` file. Compilation, testing and installation works the standard Maven way (`mvn install`, etc.).

Include the following Maven dependencies in your project in order to use the DLR MAL/SPP implementation:

```xml
<dependency>
   <groupId>de.dlr.gsoc</groupId>
   <artifactId>MO_MALSPP_TRANSPORT</artifactId>
   <version>1.0</version>
</dependency>
```

```xml
<dependency>
   <groupId>de.dlr.gsoc</groupId>
   <artifactId>MO_MALSPP_ENCODING</artifactId>
   <version>1.0</version>
</dependency>
```

These artifacts can be used in a standards compliant CCSDS MO stack by providing the following information to your stack setup (refer to its documentation on information how to do this):

* Name of the transport protocol: `malspp`
* Transport factory class: `de.dlr.gsoc.mo.malspp.transport.SPPTransportFactory`
* Encoding factory class: `de.dlr.gsoc.mo.malspp.encoding.SPPElementStreamFactory`

You also have to provide a number of configuration options, some are optional, others are mandatory. Mapping configuration parameters need to be negotiated with your communication partners out of band. They are application process specific (i.e. dependent on APID and APID qualifier) and need to be defined in an XML file.

All QoS properties use the QoS property mechanisms provided by the MAL Java API and it is the responsibility of the MO stack to allow you to populate them accordingly. The [Configuration] section provides you with the list of options.

Configuration
=============
According to [CCSDS 524.1-R-1] several mapping configuration parameters have to be provided. The following tables provide information on how the parameter values should be denoted and shows an example and additional information if necessary. They should be used together with Annex B of [CCSDS 524.1-R-1] when configuring the system. Another table is shown here with (per-message) quality of service (QoS) properties defined in Annex C of [CCSDS 524.1-R-1]. There are two tables for per-transport QoS properties. One is used to configure the underlying Space Packet Protocol API implementation, the other one for configuration of this transport implementation. These properties are not listed in [CCSDS 524.1-R-1], because they are out of scope of the MAL/SPP binding.

Mapping configuration parameters
--------------------------------
In order to provide mapping configuration parameters to this implementation, put them into an XML file similar to this one:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://www.dlr.de/gsoc/mo/malspp">
	<app apidQualifier="247" apid="1">
		<packetDataFieldSizeLimit>0</packetDataFieldSizeLimit>
		<varintSupported>true</varintSupported>
		<time>
			<format unit="second">00011111</format>
			<epoch scale="TAI">1958-01-01T00:00:00.000</epoch>
		</time>
		<fineTime>
			<format unit="second">1010111100001000</format>
			<epoch scale="TAI">2013-01-01T00:00:00.000</epoch>
		</fineTime>
		<duration>
			<format unit="second">00011100</format>
		</duration>
		<authenticationId>0001</authenticationId>
		<domain />
		<networkZone />
		<priority />
		<sessionName />
	</app>

	<app apidQualifier="248" apid="2">
		<packetDataFieldSizeLimit>0</packetDataFieldSizeLimit>
		<varintSupported>true</varintSupported>
		<time>
			<format unit="second">00011111</format>
			<epoch scale="TAI">1958-01-01T00:00:00.000</epoch>
		</time>
		<fineTime>
			<format unit="second">1010111100001000</format>
			<epoch scale="TAI">2013-01-01T00:00:00.000</epoch>
		</fineTime>
		<duration>
			<format unit="second">00011100</format>
		</duration>
		<authenticationId>0A0B0C</authenticationId>
		<domain>malspp.test.domain</domain>
		<networkZone>malsppTestNw</networkZone>
		<priority>4294967295</priority>
		<sessionName>malsppTestSession</sessionName>
	</app>
</config>
```

The parameters are automatically converted to the appropriate data types described in [CCSDS 524.1-R-1], if the notation given in the following tables is followed.

This MAL/SPP binding implementation is split in two parts, the encoding layer and the transport layer. Each of them has its own set of mapping configuration parameters, but they are specified together in the same XML file.

### Encoding layer mapping configuration parameters
All encoding layer mapping configuration parameters are mandatory. See [Notes] 4 and 5 for more information on time code notation. Please note that these time parameters control how times are encoded and decoded in and from Space Packets. They do not specify how to interpret time values on MAL level. Unfortunately, on MAL level the interpretation of time values has not been specified unambiguously, thus we need to make some assumptions for the epochs of time values in MAL. For Time we assume an epoch of 1970-01-01T00:00:00.000 UTC, i.e. the Java epoch. For FineTime we assume an epoch of 2013-01-01T00:00:00.000 UTC. Both values are specified in class `de.dlr.gsoc.mo.malspp.encoding.Configuration`.

| Parameter name               | Notation                                | Example                 | XML                                                            |
|------------------------------|-----------------------------------------|-------------------------|----------------------------------------------------------------|
| DURATION_CODE_FORMAT         | binary digits representing CUC P-field  | 00101100                | `<format>` element in `<duration>` element                     |
| DURATION_UNIT                | second / millisecond                    | second                  | `unit` attribute in `<format>` element in `<duration>` element |
| FINE_TIME_CODE_FORMAT        | binary digits representing P-field      | 1010111100001000        | `<format>` element in `<fineTime>` element                     |
| FINE_TIME_EPOCH              | ASCII time code                         | 2013-01-01T00:00:00.000 | `<epoch>` element in `<fineTime>` element                      |
| FINE_TIME_EPOCH_TIMESCALE    | timescale ([Notes] 7)                   | UTC                     | `scale` attribute in `<epoch>` element in `<fineTime>` element |
| FINE_TIME_UNIT               | second / millisecond                    | second                  | `unit` attribute in `<format>` element in `<fineTime>` element |
| TIME_CODE_FORMAT             | binary digits representing P-field      | 00101110                | `<format>` element in `<time>` element                         |
| TIME_EPOCH                   | ASCII time code                         | 1970-01-01T00:00:00.000 | `<epoch>` element in `<time>` element                          |
| TIME_EPOCH_TIMESCALE         | timescale ([Notes] 7)                   | TAI                     | `scale` attribute in `<epoch>` element in `<time>` element     |
| TIME_UNIT                    | second / millisecond                    | second                  | `unit` attribute in `<format>` element in `<time>` element     |
| VARINT_SUPPORTED             | true / false ([Notes] 2)                | true                    | `<varintSupported>` element                                    |

### Transport layer mapping configuration parameters
All transport layer parameters except `PACKET_DATA_FIELD_SIZE_LIMIT` have default values in case they are omitted. `PACKET_DATA_FIELD_SIZE_LIMIT` is mandatory. The usage of a specific optional parameter is determined by a per-message QoS property flag for that parameter.

| Parameter name               | Notation                                           | Example                 | Default value           | XML                                  |
|------------------------------|----------------------------------------------------|-------------------------|-------------------------|--------------------------------------|
| AUTHENTICATION_ID            | hexadecimal digits representing bytes ([Notes] 3)  | 1A2B                    | (empty Blob)            | `<authenticationId>` element         |
| DOMAIN                       | domain identifiers separated by dots ([Notes] 6)   | scA.aocs.thruster.A     | (empty identifier list) | `<domain>` element                   |
| NETWORK_ZONE                 | string                                             | GROUND                  | (empty identifier)      | `<networkZone>` element              |
| PACKET_DATA_FIELD_SIZE_LIMIT | 0 <= number <= 65535 ([Notes] 1)                   | 8192                    | (mandatory, no default) | `<packetDataFieldSizeLimit>` element |
| PRIORITY                     | 0 <= number <= 4294967295                          | 47                      | 0                       | `<priority>` element                 |
| SESSION_NAME                 | string                                             | LIVE                    | (empty identifier)      | `<sessionName>` element              |

QoS properties
--------------
This MAL/SPP binding implementation supports (and needs) a number of QoS (quality of service) properties. One group of these properties is specific to a single transport instance, another one is specific to each endpoint, and yet another one is set on a per-message basis.

### Transport QoS properties
One set of transport QoS properties is used to configure the implementation of the Space Packet Protocol API. A test implementation using TCP is bundled with this prototype. In order for the API to select an SPP implementation the following entry has to be present in the **system property map**: `org.ccsds.moims.mo.malspp.test.spp.factory.class`. The value is the fully qualified class name of the SPP implementation. The default value of `org.ccsds.moims.mo.testbed.util.sppimpl.tcp.TCPSPPSocketFactory` is used when the property is not found and points to the bundled implementation using TCP. The following QoS properties are used to configure the SPP TCP implementation and are passed using the MO stack's QoS properties for transports. All property names need to be prepended with `org.ccsds.moims.mo.malspp.test.sppimpl.tcp.`.

| Property name  | Notation                                | Example                 | Mandatory |
|----------------|-----------------------------------------|-------------------------|-----------|
| isServer       | true / false                            | false                   | yes       |
| port           | port number                             | 54321                   | yes       |
| hostname       | string, only needed if isServer==false  | localhost               | no / yes  |

Another set of transport QoS properties is used to configure the transport implementation a level higher than the Space Packet Protocol API. They need to be prepended by `de.dlr.gsoc.mo.malspp.`.

| Property name              | Notation                                              | Example                                                  | Mandatory |
|----------------------------|-------------------------------------------------------|----------------------------------------------------------|-----------|
| TIMEOUT                    | number of milliseconds (default 0, [Notes] 8)         | 5000                                                     | no        |
| MAPPING_CONFIGURATION_FILE | file name of mapping configuration parameter XML file | target/deployment/dlr/mappingConfigurationParameters.xml | yes       |

### Per-endpoint QoS properties
Per-endpoint QoS properties are used to configure each endpoint. They are mandatory except for `appendToUri` which defaults to `true`. Each property needs to be prepended by `org.ccsds.moims.mo.malspp.`. They can be passed in as transport QoS properties, which then can be overridden for the actual endpoint creation.

| Property name  | Notation                                | Example                 | Mandatory |
|----------------|-----------------------------------------|-------------------------|-----------|
| apid           | 0 <= number <= 2046                     | 0                       | yes       |
| apid.qualifier | 0 <= number <= 65535                    | 42                      | yes       |
| appendToUri    | true / false (default: true)            | true                    | no        |

There are two *undocumented* parameters `de.dlr.gsoc.mo.malspp.NUM_IDENTIFIERS` and `de.dlr.gsoc.mo.malspp.START_IDENTIFIER`, which are subject to change. They allow the range of possible instance identifiers, that are allocated by a specific transport instance, to be limited. `START_IDENTIFIER` denotes the first identifier to use (0 <= number <= 255; default 0), `NUM_IDENTIFIERS` denotes the consecutive number of available instance identifiers (1 <= number <= 256 - `START_IDENTIFIER`; default 256).

### Per-message QoS properties
Per-message QoS property names are defined in [CCSDS 524.1-R-1], except for `isTcPacket`. They need to be prepended with `org.ccsds.moims.mo.malspp.` and are optional. In case an optional per-message QoS property is left out, the effect is as if it had been set to `true` (according to [CCSDS 524.1-R-1] for the flags). The flags are effectively a toggle switch for usage of the transport layer mapping configuration parameters. Providing `isTcPacket` as per-message QoS property is this implementation's realization of the out-of-band agreement of determining a Space Packet's packet type according to 3.4.2.3 [CCSDS 524.1-R-1]. If `isTcPacket` is not provided a default value of `true` is assumed, meaning all generated Space Packets will be of packet type *telecommand*.

| Property name          | Notation     |
|------------------------|--------------|
| authenticationIdFlag   | true / false |
| domainFlag             | true / false |
| networkZoneFlag        | true / false |
| priorityFlag           | true / false |
| sessionNameFlag        | true / false |
| timestampFlag          | true / false |
| isTcPacket             | true / false |

Notes
-----
1. A *PACKET_DATA_FIELD_SIZE_LIMIT* value of 0  denotes the largest size possible, i.e. 65536 bytes. When setting this value make sure to set it large enough so that the Space Packets provide enough room for the secondary header and some payload data.
2. Boolean values (true/false) are case insensitive.
3. The number of hexadecimal digits representing bytes needs to be even.
4. P-fields are given in binary digits according to [CCSDS 301.0-B-4]. The number of digits has to be 8 for a standard P-field or 16 for an extended P-field. Longer extensions are not allowed. Only P-fields representing a CUC time code are allowed for *DURATION_CODE_FORMAT*.
5. _ASCII time code_ refers to the _CCSDS ASCII Calendar Segmented Time Code_ defined in [CCSDS 301.0-B-4] and is a subset of ISO-8601.
6. Domain identifiers must not contain dots (.) as they are used for separating the domains.
7. It has to be specified in which timescale the epochs are to interpret. Possible values are UTC, TAI, GMST, GPS, GST, TCB, TCG, TDB, TT, case insensitive. If these parameters are omitted it is assumed the epochs are given in UTC timescale. Please note, that this only affects the interpretation of the epoch parameters.
8. This specifies the number of milliseconds for sequenced packet timeout. If a sequence of packets is not full after this timeout, the whole sequence is discarded. A timeout of 0 means no timeout, which is also the default value.

[CCSDS 301.0-B-4]: http://public.ccsds.org/publications/archive/301x0b4.pdf
                   "Time Code Formats (November 2010)"


Implementation details
======================
The transport and encoding implementations for the MAL/SPP Binding follow the class structure set forth in [CCSDS 523.1-M-1]. The following UML class diagrams show this sructure in detail, only the public members are visible. Classes and interfaces starting with `MAL` are part of the MAL Java API and should be made available by a MAL Java implementation.

![Transport API implementation](transport_api.png)

![Encoding API implementation, element stream handling part](transport_encoding_api.png)

![Encoding API implementation, data encoding part](data_encoding_api.png)

Encoding layer internals
------------------------
Internally, several non-public methods have been defined, in addition to two helper classes, `ServiceInfo` and `CCSDSTime`. While `ServiceInfo` is closely tied to the MAL concepts and tries to hide away some of the more clumsy aspects of the type handling in its Java API, the class `CCSDSTime` is only concerned with encoding and decoding of time code fields that comply to [CCSDS 301.0-B-4]. The only MAL dependency is an easily replaceable exception class. The only other dependecy is the [Orekit library](http://www.orekit.org), which handles time representation and decoding.

Transport layer internals
-------------------------
Several non-public methods have been defined, as well as three helper classes, `SPPSegmenter`, `SPPCounter` and `SPPURI`. `SPPSegmenter` has two tasks: First, for oversized MAL messages several segmented Space Packets are produced (`split()` method). Second, received Space Packets are processed (`process()` method) and grouped according to the segment they belong to, such that a call to `next()` always retrieves all Space Packets belonging to a single MAL message. `SPPCounter` represents a wrap-around counter that is used for the *Packet Sequence Count* field of Space Packets and for the *Segment Counter* in the secondary header. `SPPURI` represents valid URIs for the MAL/SPP binding, allows their construction and extraction of the URI components.

`SPPTransportFactory` maintains a map (`transports`) which maps protocol strings to instances of `SPPTransport`. In theory it is possible that a transport can handle mutliple protocols. Each protocol then gets its own transport instance, which is held in this map.

`SPPTransport` is at the core of handling the interaction between MAL and Space Packets. It is a factory of `SPPEndpoint` and maintains several maps holding references to the created endpoints. `endpointsByName` and `endpointsByURI` map local names and URIs to the endpoints, respectively. Only the latter contains all endpoints, because a URI is mandatory while a name is optional. Because each endpoint can be sender or receiver of messages, they need to have a communication socket associated. For simplicity this implementation uses only one socket per transport that is shared across all endpoints belonging to that transport. The socket is created when the transport is initialized. Upon construction of the first endpoint a thread named `ReceiveThread_malspp` is created handling message reception for this socket. Because of this the reception logic cannot reside in `SPPEndpoint` (there is only one `receive()` method for the socket, which needs to serve multiple endoints). Instead, `SPPTransport.receive()` initiates MAL message decoding, determines the correct reception endpoint using the URI the message was sent to and calls the `onMessage()` method of the endpoint's `MALMessageListener` in a new thread called `ListenerThread_malspp`.

APID and APID qualifier are bound to the socket (and therefore common for all endpoints of a single transport) and are determined from configuration. Instance id (i.e. source or destination identifier) allocation, however, is performed by the MAL/SPP Binding layer. The combination of APID and APID qualifier also identifies the *Packet Sequence Counter* for the Space Packets according to [CCSDS 133.0-B-1]. This identification is represented by the inner class `SequenceCounterId`. `sequenceCounters` provides the map from `SequenceCounterId` to `SPPCounter`, which handles correct packet sequence counting. `identifiers` then maps `SequenceCounterId` to a queue of possible instance identifiers, which simply is a pool of numbers, where each new instance id is taken from or returned back to when the endpoint is closed. The inner class `SegmentCounterId` is used for identification of a segment counter, which is used for recombining segmented Space Packets. Identification is based on a number of MAL message header fields that together uniquely determine a single MAL message. `segmentCounters` provides the map from `SegmentCounterId` to `SPPCounter`.

Sending and message construction logic resides in `SPPEndpoint`. MAL messages are constructed by creating an `SPPMessageHeader`, an `SPPMessageBody` (or appropriate subclass) and passing them to the constructor of `SPPMessage`. `SPPMessage` can create Space Packets from this message with `createSpacePackets()` (segmented if necessary). By passing Space Packets to the constructor `SPPMessage` also does decoding of received messages. Message body encoding or decoding is performed transparently and on demand by `SPPMessageBody`. `SPPMessageHeader` is a mere data container with some convenience methods. Message header encoding happens in `SPPMessage`, decoding happens in `SPPMessageHeader`, i.e. in `SPPMessage.writeSecondaryHeader()` for encoding and `SPPMessageHeader.initMessageHeader()` for decoding (the primary header is handled by implementations of the SPP API).

Summarizing the internals it is important to note, that there is only one socket per transport, that is shared across all endpoints. This socket is listened to by a single receive thread, which reconstructs MAL messages from received Space Packets. The queue of received messages is handled by a single message handler thread. The `onMessage()` method of a registered listener is called in its own thread, i.e. there can be multiple threads running simultaneously handling messages and possibly sending out new messages. Therefore socket access is synchronized in order to prevent multiple threads writing to it at the same time.