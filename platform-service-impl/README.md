Platform services adapters for OPS-SAT
============

ESA's OPS-SAT mission uses the NMF Core implementation which includes the implementation of all the Platform services. The Platform services take advantage of the adapter pattern in order to support different units.

There are 5 platform peripherals in OPS-SAT that can be used by the experimenters: Camera, Optical Data Receiver, SDR, FineADCS, and GPS.


Camera service adapter
============

The Camera service adapter for OPS-SAT is connected to the Camera payload device via a USB virtual serial port. OPS-SAT's Camera is a ST200 from Hyperion Technologies and it has a power consumption of 650 mW in nominal mode, a mass of 42 grams, and an update rate of 5 Hz.

Upon initialization, the adapter checks if there is a serial port device camera and if so, it attempts to connect to it. If the device is not available, the Camera service is still started however if for example, a consumer tries to take a picture, then the service will return an error.

When the camera is functional and a consumer takes a picture, the adapter will double-check if the device is connected and then, send the instructions to configure it and take the picture. Finally, it copies the content of the image that is stored as an external memory device into a Picture object and sends it back to the consumer.

GPS service adapter
============

The GPS service adapter for OPS-SAT is connected to the GPS payload device via an MO interface exposed by the Nanomind device on the CAN bus. OPS-SAT's GPS is part of the OEM615 Family from NovAtel and it has low power consumption, dual frequency (L1, L2, and L2C for GPS and GLONASS), and supports multi-constellations (E1 for Galileo and B1 for BeiDou).

The Nanomind is connected to the GPS Unit via a UART connection that allows requesting information from a set of commands. Then, the Nanomind device exposes a GPS service for interacting with the GPS unit. This service can be consumed from ground or by the Experimental Platform.

Although the GPS service from the Nanomind has the same name as the GPS service from the Platform services, they are completely different.

AutonomousADCS service adapter
============

The Autonomous ADCS service adapter for OPS-SAT is connected to the ADCS payload device via I²C. OPS-SAT's ADCS is an iADCS-100 from Berlin Space Technologies and consists of a star tracker, gyro module, reaction wheels, and magnetorquers, with the possibility to integrate external sensors such as sun sensors.

It incorporates the ADCS algorithms from the LEOS platform and allows full ADCS functionality including nadir pointing as well as autonomous target acquisition and tracking.

Other service adapters
============

The Software-defined Radio service adapter and the Optical Data Receiver service adapter were not implemented because the units were not available during this research.

