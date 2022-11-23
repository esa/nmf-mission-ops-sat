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
package de.dlr.gsoc.mo.malspp.encoding;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALEncoder;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListEncoder;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.Element;
import org.ccsds.moims.mo.mal.structures.FineTime;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.ULong;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.orekit.time.AbsoluteDate;

public class SPPEncoder implements MALEncoder, MALListEncoder {

    protected static final String ILLEGAL_NULL_ARGUMENT = "Argument may not be null.";
    protected static final String WRONG_TIME_FORMAT = "Wrong time format specification.";
    private final OutputStream outputStream;
    private Map properties;
    private CCSDSTime timeFormatter;
    private CCSDSTime fineTimeFormatter;
    private CCSDSTime durationFormatter;
    private boolean varintSupported;

    public SPPEncoder(final OutputStream outputStream, final Map properties) {
        this.outputStream = outputStream;
        this.properties = properties;
        this.varintSupported = new Configuration(properties).varintSupported();
    }

    @Override
    public void encodeBoolean(final Boolean att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }

        final byte b;
        if (att) {
            b = 1;
        } else {
            b = 0;
        }
        write(b);
    }

    @Override
    public void encodeNullableBoolean(final Boolean att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeBoolean(att);
        }
    }

    @Override
    public void encodeFloat(final Float att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }

        final int b = Float.floatToRawIntBits(att);
        for (int i = 3; i >= 0; i--) {
            write(b >> (8 * i));
        }
    }

    @Override
    public void encodeNullableFloat(final Float att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeFloat(att);
        }
    }

    @Override
    public void encodeDouble(final Double att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }

        final long b = Double.doubleToRawLongBits(att);
        for (int i = 7; i >= 0; i--) {
            // casting long to int and losing information is explicitly wanted here
            write((int) (b >> (8 * i)));
        }
    }

    @Override
    public void encodeNullableDouble(final Double att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeDouble(att);
        }
    }

    @Override
    public void encodeOctet(final Byte att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        write(att);
    }

    @Override
    public void encodeNullableOctet(final Byte att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeOctet(att);
        }
    }

    @Override
    public void encodeUOctet(final UOctet att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        write(att.getValue());
    }

    @Override
    public void encodeNullableUOctet(final UOctet att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeUOctet(att);
        }
    }

    @Override
    public void encodeShort(final Short att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        writeVarint(BigInteger.valueOf(att.longValue()), 2, true);
    }

    @Override
    public void encodeNullableShort(final Short att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeShort(att);
        }
    }

    @Override
    public void encodeUShort(final UShort att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        writeVarint(BigInteger.valueOf(att.getValue()), 2, false);
    }

    @Override
    public void encodeNullableUShort(final UShort att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeUShort(att);
        }
    }

    @Override
    public void encodeInteger(final Integer att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        writeVarint(BigInteger.valueOf(att.longValue()), 4, true);
    }

    @Override
    public void encodeNullableInteger(final Integer att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeInteger(att);
        }
    }

    @Override
    public void encodeUInteger(final UInteger att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        writeVarint(BigInteger.valueOf(att.getValue()), 4, false);
    }

    @Override
    public void encodeNullableUInteger(final UInteger att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeUInteger(att);
        }
    }

    @Override
    public void encodeLong(final Long att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        writeVarint(BigInteger.valueOf(att), 8, true);
    }

    @Override
    public void encodeNullableLong(final Long att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeLong(att);
        }
    }

    @Override
    public void encodeULong(final ULong att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        writeVarint(att.getValue(), 8, false);
    }

    @Override
    public void encodeNullableULong(final ULong att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeULong(att);
        }
    }

    @Override
    public void encodeString(final String att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        final byte[] bytes = att.getBytes(StandardCharsets.UTF_8);
        //			encodeUInteger(new UInteger(bytes.length));
        encodeUShort(new UShort(bytes.length));
        write(bytes);
    }

    @Override
    public void encodeNullableString(final String att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeString(att);
        }
    }

    @Override
    public void encodeBlob(final Blob att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        // PENDING: Bug in MAL Java API: If the Blob is URL based, getLength() returns 0. Here:
        // Workaround by directly querying the length of the value byte array in this case.
        final int length = att.isURLBased() ? att.getValue().length : att.getLength();
        encodeUShort(new UShort(length));
        final byte[] value = att.getValue();
        if (null != value) {
            write(value);
        }
    }

    @Override
    public void encodeNullableBlob(final Blob att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeBlob(att);
        }
    }

    @Override
    public void encodeDuration(final Duration att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        final AbsoluteDate epoch = CCSDSTime.createEpoch(Configuration.DURATION_EPOCH,
            Configuration.DURATION_EPOCH_TIMESCALE);
        // PENDING: Error in MAL Java API Magenta Book, Duration should contain fractional seconds,
        // but contains integer seconds. There is nothing we can do except to wait for an updated
        // book and implementation.
        //		long ct = att.getValue();
        final double ct = att.getValue();
        final CCSDSTime tf = getDurationFormatter();
        if (tf.getTimeCode() != CCSDSTime.TimeCode.CUC) {
            throw new MALException(WRONG_TIME_FORMAT);
        }
        // Split into coarseTime and fineTime in order to minimize rounding errors.
        final AbsoluteDate coarseTime = new AbsoluteDate(epoch, ct);
        final AbsoluteDate fineTime = coarseTime.shiftedBy(0);
        final byte[] tField = tf.getEncodedTime(coarseTime, fineTime, true);
        write(tField);
    }

    @Override
    public void encodeNullableDuration(final Duration att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeDuration(att);
        }
    }

    @Override
    public void encodeFineTime(final FineTime att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        final AbsoluteDate epoch = CCSDSTime.createEpoch(Configuration.MAL_FINE_TIME_EPOCH,
            Configuration.MAL_FINE_TIME_EPOCH_TIMESCALE);
        // Split into coarseTime and fineTime in order to minimize rounding errors.
        final long ct = att.getValue() / 1000000000000L;
        final double ft = (att.getValue() - ct * 1000000000000L) / 1000000000000.0;
        final AbsoluteDate coarseTime = new AbsoluteDate(epoch, ct);
        final AbsoluteDate fineTime = coarseTime.shiftedBy(ft);
        final byte[] tField = getFineTimeFormatter().getEncodedTime(coarseTime, fineTime, false);
        write(tField);
    }

    @Override
    public void encodeNullableFineTime(final FineTime att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeFineTime(att);
        }
    }

    @Override
    public void encodeIdentifier(final Identifier att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        encodeString(att.getValue());
    }

    @Override
    public void encodeNullableIdentifier(final Identifier att) throws MALException {

        /*
        encodeNulltag(att);
        if (att != null) {
        encodeIdentifier(att);
        }
        */

        // Fix for the case where the String inside the Identifier contains a null...
        if (att != null) {
            if (att.getValue() == null) {
                encodeNulltag(null);
            } else {
                encodeNulltag(att);
            }
        } else {
            encodeNulltag(null);
        }

        if (att != null) {
            if (att.getValue() != null) {
                encodeIdentifier(att);
            }
        }
    }

    @Override
    public void encodeTime(final Time att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        // PENDING: Epoch for Time in MAL Java API unclear. Here: Use Java epoch.
        // Construct our own Java epoch due to bug in Orekit library (https://www.orekit.org/forge/issues/142).
        final AbsoluteDate epoch = CCSDSTime.createEpoch(Configuration.JAVA_EPOCH, Configuration.JAVA_EPOCH_TIMESCALE);
        // Split into coarseTime and fineTime in order to minimize rounding errors.
        final long ct = att.getValue() / 1000;
        final double ft = (att.getValue() - ct * 1000) / 1000.0;
        final AbsoluteDate coarseTime = new AbsoluteDate(epoch, ct);
        final AbsoluteDate fineTime = coarseTime.shiftedBy(ft);
        final byte[] tField = getTimeFormatter().getEncodedTime(coarseTime, fineTime, false);
        write(tField);
    }

    @Override
    public void encodeNullableTime(final Time att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeTime(att);
        }
    }

    @Override
    public void encodeURI(final URI att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        encodeString(att.getValue());
    }

    @Override
    public void encodeNullableURI(final URI att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeURI(att);
        }
    }

    @Override
    public void encodeElement(final Element element) throws IllegalArgumentException, MALException {
        if (element == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        element.encode(this);
    }

    @Override
    public void encodeNullableElement(final Element element) throws MALException {
        encodeNulltag(element);
        if (element != null) {
            encodeElement(element);
        }
    }

    @Override
    public void encodeAttribute(final Attribute att) throws IllegalArgumentException, MALException {
        if (att == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        encodeUOctet(new UOctet((short) (att.getTypeShortForm().byteValue() - 1)));
        att.encode(this);
    }

    @Override
    public void encodeNullableAttribute(final Attribute att) throws MALException {
        encodeNulltag(att);
        if (att != null) {
            encodeAttribute(att);
        }
    }

    @Override
    public MALListEncoder createListEncoder(final List list) throws IllegalArgumentException, MALException {
        if (list == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        encodeUShort(new UShort(list.size()));
        return this;
    }

    @Override
    public void close() {
        // do nothing
    }

    /**
     * Wrapper for outputStream.write()
     *
     * @param b
     * @throws MALException
     */
    protected void write(final byte[] b) throws MALException {
        try {
            outputStream.write(b);
        } catch (final IOException ex) {
            throw new MALException(ex.getMessage(), ex);
        }
    }

    /**
     * Wrapper for outputStream.write()
     *
     * @param b
     * @throws MALException
     */
    protected void write(final int b) throws MALException {
        try {
            outputStream.write(b);
        } catch (final IOException ex) {
            throw new MALException(ex.getMessage(), ex);
        }
    }

    /**
     * Encodes the presence flag of a nullable element.
     *
     * @param obj A presence flag with value false is encoded, if obj is null, otherwise the
     * presence flag is encoded with value true.
     * @throws MALException
     */
    protected void encodeNulltag(final Object obj) throws MALException {
        encodeBoolean(!(null == obj));
    }

    /**
     * Writes a variable integer value to outputStream. Respects the parameter VARINT_SUPPORTED and
     * writes out a non-variable integer in case Varints are not supported.
     *
     * @param value The value to encode.
     * @param nOctets The number of bytes that make up the value to encode.
     * @param signed True, if the values are signed and in case of variable integers shall be
     * zig-zag mapped before encoding. False if no mapping shall be employed. Ignored for non-
     * variable integers.
     * @throws IOException
     */
    private void writeVarint(BigInteger value, final int nOctets, final boolean signed) throws MALException {
        if (!varintSupported) {
            for (int i = nOctets - 1; i >= 0; i--) {
                write(value.shiftRight(8 * i).byteValue());
            }
            return;
        }

        if (signed) {
            // Perform zig-zag mapping of value.
            // With hypothetical operator overloading: value = (value << 1) ^ (value >> (8 * nOctets - 1))
            value = value.shiftLeft(1).xor((value.shiftRight(8 * nOctets - 1)));
        }
        while (true) {
            // Check if only 0s are to come except for the lowest 7 bits. This means we reached the
            // end and are about to write the last 7 bit group now. The MSB is 0 by this if
            // condition and needs not be cleared explicitly.
            // With hypothetical operator overloading: if ((value & ~ 0b01111111) == 0)
            if ((value.and(BigInteger.valueOf(0b01111111).not())).equals(BigInteger.ZERO)) {
                write(value.intValue());
                return;
            }
            // Only take the lowest 7 bits, set the MSB and afterwards shift these 7 bits into
            // oblivion.
            // With hypothetical operator overloading: write((value & 0b01111111) | 0b10000000)
            write(value.and(BigInteger.valueOf(0b01111111)).or(BigInteger.valueOf(0b10000000)).intValue());
            value = value.shiftRight(7);
        }
    }

    /**
     * Gets a formatter for Time fields. Return existing or create new one, if not existing.
     *
     * @return Time formatter allowing do encode times.
     * @throws MALException
     */
    private CCSDSTime getTimeFormatter() throws MALException {
        if (timeFormatter == null) {
            final Configuration config = new Configuration(properties);
            timeFormatter = new CCSDSTime(config.timeCodeFormat(), config.timeEpoch(), config.timeEpochTimescale(),
                config.timeUnit());
        }
        return timeFormatter;
    }

    /**
     * Gets a formatter for FineTime fields. Return existing or create new one, if not existing.
     *
     * @return Fine time formatter allowing do encode fine times.
     * @throws MALException
     */
    private CCSDSTime getFineTimeFormatter() throws MALException {
        if (fineTimeFormatter == null) {
            final Configuration config = new Configuration(properties);
            fineTimeFormatter = new CCSDSTime(config.fineTimeCodeFormat(), config.fineTimeEpoch(), config
                .fineTimeEpochTimescale(), config.fineTimeUnit());
        }
        return fineTimeFormatter;
    }

    /**
     * Gets a formatter for Duration fields. Return existing or create new one, if not existing.
     *
     * This method uses an arbitrary epoch (DURATION_EPOCH) to map relative times to absolute times
     * and to use the standard time encoding methods.
     *
     * @return Duration formatter allowing do encode durations.
     * @throws MALException
     */
    private CCSDSTime getDurationFormatter() throws MALException {
        if (durationFormatter == null) {
            final Configuration config = new Configuration(properties);
            durationFormatter = new CCSDSTime(config.durationCodeFormat(), Configuration.DURATION_EPOCH,
                Configuration.DURATION_EPOCH_TIMESCALE, config.durationUnit());
        }
        return durationFormatter;
    }

    /**
     * @return The mapping configuration parameters.
     */
    protected Map getProperties() {
        return properties;
    }

    /**
     * Used for injecting properties during testing.
     *
     * @param properties The mapping configuration parameters to set.
     */
    protected void setProperties(final Map properties) {
        this.properties = properties;
        this.varintSupported = new Configuration(properties).varintSupported();
    }
}
