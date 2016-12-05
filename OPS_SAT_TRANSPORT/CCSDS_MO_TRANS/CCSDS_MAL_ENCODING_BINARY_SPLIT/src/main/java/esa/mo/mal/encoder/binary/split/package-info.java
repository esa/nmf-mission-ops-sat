/**
An implementation of the encoding API for a binary encoding. This extends the base binary encoder to split out the
encoding of nulls and booleans to an initial bit set. It produces considerably smaller encoded messages than the
standard binary encoder for larger messages.
 */
package esa.mo.mal.encoder.binary.split;
