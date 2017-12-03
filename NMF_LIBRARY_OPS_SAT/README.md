NMF Library for OPS-SAT
============

An NMF Library is a shared library made available on the on-board computer that includes the NMF Mission implementation software. It includes all the necessary dependencies to execute the implementation of the NanoSat MO Supervisor for that mission and also NMF Apps.

NMF Apps developed in Java running on-board can link to this library that contains the NanoSat MO Connector component and therefore the compiled jar file doesn’t need to integrate it. This technique minimizes the content needed to be transferred to the spacecraft, to just the logic of the application.


