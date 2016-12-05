/**
 * An implementation of the transport interfaces for the SPP protocol. Builds upon the generic transport framework. It
 * provides the classes needed for a real SPP based transport, there cannot be one supplied here as SPP is actually a
 * protocol rather than a message transport so requires another technology to move the messages from A to B.
 *
 * Derived transports that use SPP should ensure that only SPPMessages are created by overriding the correct methods in
 * GENTransport.
 */
package esa.mo.mal.transport.spp;
