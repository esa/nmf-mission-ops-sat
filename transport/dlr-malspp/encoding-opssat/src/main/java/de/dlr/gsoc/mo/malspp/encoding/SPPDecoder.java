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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALDecoder;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListDecoder;
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
import org.ccsds.moims.mo.mal.structures.Union;
import org.orekit.time.AbsoluteDate;
import static de.dlr.gsoc.mo.malspp.encoding.SPPEncoder.WRONG_TIME_FORMAT;
import static de.dlr.gsoc.mo.malspp.encoding.SPPEncoder.ILLEGAL_NULL_ARGUMENT;

public class SPPDecoder implements MALDecoder {

	protected static final String INVALID_VALUE = "Invalid value read from input stream.";
	protected static final String INSUFFICIENT_DATA = "Insufficient data in input stream.";
	protected static final String LENGTH_NOT_SUPPORTED = "Field or list length exceeds supported length.";
	private final InputStream inputStream;
	private Map properties;
	private CCSDSTime timeFormatter;
	private CCSDSTime fineTimeFormatter;
	private CCSDSTime durationFormatter;
	private boolean varintSupported;

	public SPPDecoder(final InputStream inputStream, final Map properties) {
		this.inputStream = inputStream;
		this.properties = properties;
		this.varintSupported = new Configuration(properties).varintSupported();
	}

	@Override
	public Boolean decodeBoolean() throws MALException {
		byte b = read();
		switch (b) {
			case 0:
				return Boolean.FALSE;
			case 1:
				return Boolean.TRUE;
			default:
				throw new MALException(INVALID_VALUE);
//				throw new MALException(INVALID_VALUE + " The value is: " + Byte.toString(b));
		}
	}

	@Override
	public Boolean decodeNullableBoolean() throws MALException {
		return isNull() ? null : decodeBoolean();
	}

	@Override
	public Float decodeFloat() throws MALException {
		byte[] b = read(4);
		int ret = 0;
		for (int i = 0; i < 4; i++) {
			ret <<= 8;
			ret |= (int) b[i] & 0xFF;
		}
		return Float.intBitsToFloat(ret);
	}

	@Override
	public Float decodeNullableFloat() throws MALException {
		return isNull() ? null : decodeFloat();
	}

	@Override
	public Double decodeDouble() throws MALException {
		byte[] b = read(8);
		long ret = 0;
		for (int i = 0; i < 8; i++) {
			ret <<= 8;
			ret |= (long) b[i] & 0xFF;
		}
		return Double.longBitsToDouble(ret);
	}

	@Override
	public Double decodeNullableDouble() throws MALException {
		return isNull() ? null : decodeDouble();
	}

	@Override
	public Byte decodeOctet() throws MALException {
		return read();
	}

	@Override
	public Byte decodeNullableOctet() throws MALException {
		return isNull() ? null : decodeOctet();
	}

	@Override
	public UOctet decodeUOctet() throws MALException {
		return new UOctet((short) (read() & 0xFF));
	}

	@Override
	public UOctet decodeNullableUOctet() throws MALException {
		return isNull() ? null : decodeUOctet();
	}

	@Override
	public Short decodeShort() throws MALException {
		return decodeVarint(2, true).shortValue();
	}

	@Override
	public Short decodeNullableShort() throws MALException {
		return isNull() ? null : decodeShort();
	}

	@Override
	public UShort decodeUShort() throws MALException {
		return new UShort(decodeVarint(2, false).intValue());
	}

	@Override
	public UShort decodeNullableUShort() throws MALException {
		return isNull() ? null : decodeUShort();
	}

	@Override
	public Integer decodeInteger() throws MALException {
		return decodeVarint(4, true).intValue();
	}

	@Override
	public Integer decodeNullableInteger() throws MALException {
		return isNull() ? null : decodeInteger();
	}

	@Override
	public UInteger decodeUInteger() throws MALException {
		return new UInteger(decodeVarint(4, false).longValue());
	}

	@Override
	public UInteger decodeNullableUInteger() throws MALException {
		return isNull() ? null : decodeUInteger();
	}

	@Override
	public Long decodeLong() throws MALException {
		return decodeVarint(8, true).longValue();
	}

	@Override
	public Long decodeNullableLong() throws MALException {
		return isNull() ? null : decodeLong();
	}

	@Override
	public ULong decodeULong() throws MALException {
		return new ULong(decodeVarint(8, false));
	}

	@Override
	public ULong decodeNullableULong() throws MALException {
		return isNull() ? null : decodeULong();
	}

	@Override
	public String decodeString() throws MALException {
		int length = decodeUShort().getValue();
		if (length > 65535) {
			throw new MALException(LENGTH_NOT_SUPPORTED);
		}
		String ret = null;
		try {
			ret = new String(read((int) length), "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			// UTF-8 is required by the Java Standard, this exception cannot be thrown
		}
		return ret;
	}

	@Override
	public String decodeNullableString() throws MALException {
		return isNull() ? null : decodeString();
	}

	@Override
	public Blob decodeBlob() throws MALException {
		int length = decodeUShort().getValue();
		if (length > 65535) {
			throw new MALException(LENGTH_NOT_SUPPORTED);
		}
		return new Blob(read(length));
	}

	@Override
	public Blob decodeNullableBlob() throws MALException {
		return isNull() ? null : decodeBlob();
	}

	@Override
	public Duration decodeDuration() throws MALException {
		CCSDSTime tf = getDurationFormatter();
		if (tf.getTimeCode() != CCSDSTime.TimeCode.CUC) {
			throw new MALException(WRONG_TIME_FORMAT);
		}
		byte[] tField = read(tf.getDataLength());
		AbsoluteDate time = tf.getDecodedTime(tField);
		AbsoluteDate epoch = CCSDSTime.createEpoch(Configuration.DURATION_EPOCH, Configuration.DURATION_EPOCH_TIMESCALE);
		double seconds = time.durationFrom(epoch);

		// check sign bit of tField
		if ((tField[0] & 0x80) != 0) {
			// negative duration, undo 2's complement by constructing T field with every bit set
			byte[] minNegativeTimeField = new byte[tf.getDataLength()];
			for (int i = 0; i < minNegativeTimeField.length; i++) {
				minNegativeTimeField[i] = -1;
			}
			AbsoluteDate minNegativeTime = tf.getDecodedTime(minNegativeTimeField);
			double minNegativeSeconds = minNegativeTime.durationFrom(epoch);
			seconds -= minNegativeSeconds + 1;
		}

		// PENDING: Error in MAL Java API Magenta Book, Duration should contain fractional seconds,
		// but contains integer seconds. There is nothing we can do except to wait for an updated
		// book and implementation. As soon as this is available care needs to be taken to check
		// especially the fine time behaviour for negative durations.
//		return new Duration((int) seconds);
		return new Duration(seconds);
	}

	@Override
	public Duration decodeNullableDuration() throws MALException {
		return isNull() ? null : decodeDuration();
	}

	@Override
	public FineTime decodeFineTime() throws MALException {
		CCSDSTime tf = getFineTimeFormatter();
		byte[] tField = read(tf.getDataLength());
		AbsoluteDate time = tf.getDecodedTime(tField);
		AbsoluteDate epoch = CCSDSTime.createEpoch(Configuration.MAL_FINE_TIME_EPOCH, Configuration.MAL_FINE_TIME_EPOCH_TIMESCALE);
		long coarseSeconds = (long) time.durationFrom(epoch);
		AbsoluteDate coarseTime = epoch.shiftedBy(coarseSeconds);
		long picoSeconds = coarseSeconds * 1000000000000L + Math.round(time.durationFrom(coarseTime) * 1000000000000L);
		return new FineTime(picoSeconds);
	}

	@Override
	public FineTime decodeNullableFineTime() throws MALException {
		return isNull() ? null : decodeFineTime();
	}

	@Override
	public Identifier decodeIdentifier() throws MALException {
		return new Identifier(decodeString());
	}

	@Override
	public Identifier decodeNullableIdentifier() throws MALException {
		return isNull() ? null : decodeIdentifier();
	}

	@Override
	public Time decodeTime() throws MALException {
		CCSDSTime tf = getTimeFormatter();
		byte[] tField = read(tf.getDataLength());
		AbsoluteDate time = tf.getDecodedTime(tField);
		// PENDING: Epoch for Time in MAL Java API unclear. Here: Use Java epoch.
		// Construct our own Java epoch due to bug in Orekit library (https://www.orekit.org/forge/issues/142).
		AbsoluteDate epoch = CCSDSTime.createEpoch(Configuration.JAVA_EPOCH, Configuration.JAVA_EPOCH_TIMESCALE);
		double seconds = time.durationFrom(epoch);
		return new Time(Math.round(seconds * 1000));
	}

	@Override
	public Time decodeNullableTime() throws MALException {
		return isNull() ? null : decodeTime();
	}

	@Override
	public URI decodeURI() throws MALException {
		return new URI(decodeString());
	}

	@Override
	public URI decodeNullableURI() throws MALException {
		return isNull() ? null : decodeURI();
	}

	@Override
	public Element decodeElement(final Element element) throws IllegalArgumentException, MALException {
		if (element == null) {
			throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
		}
		return element.decode(this);
	}

	@Override
	public Element decodeNullableElement(final Element element) throws IllegalArgumentException, MALException {
		return isNull() ? null : decodeElement(element);
	}

	@Override
	public Attribute decodeAttribute() throws MALException {
		int shortForm = decodeUOctet().getValue() + 1;
		switch (shortForm) {
			case Attribute._BLOB_TYPE_SHORT_FORM:
				return decodeBlob();
			case Attribute._BOOLEAN_TYPE_SHORT_FORM:
				return new Union(decodeBoolean());
			case Attribute._DOUBLE_TYPE_SHORT_FORM:
				return new Union(decodeDouble());
			case Attribute._DURATION_TYPE_SHORT_FORM:
				return decodeDuration();
			case Attribute._FINETIME_TYPE_SHORT_FORM:
				return decodeFineTime();
			case Attribute._FLOAT_TYPE_SHORT_FORM:
				return new Union(decodeFloat());
			case Attribute._IDENTIFIER_TYPE_SHORT_FORM:
				return decodeIdentifier();
			case Attribute._INTEGER_TYPE_SHORT_FORM:
				return new Union(decodeInteger());
			case Attribute._LONG_TYPE_SHORT_FORM:
				return new Union(decodeLong());
			case Attribute._OCTET_TYPE_SHORT_FORM:
				return new Union(decodeOctet());
			case Attribute._SHORT_TYPE_SHORT_FORM:
				return new Union(decodeShort());
			case Attribute._STRING_TYPE_SHORT_FORM:
				return new Union(decodeString());
			case Attribute._TIME_TYPE_SHORT_FORM:
				return decodeTime();
			case Attribute._UINTEGER_TYPE_SHORT_FORM:
				return decodeUInteger();
			case Attribute._ULONG_TYPE_SHORT_FORM:
				return decodeULong();
			case Attribute._UOCTET_TYPE_SHORT_FORM:
				return decodeUOctet();
			case Attribute._URI_TYPE_SHORT_FORM:
				return decodeURI();
			case Attribute._USHORT_TYPE_SHORT_FORM:
				return decodeUShort();
			default:
				throw new MALException("Unknown attribute type short form: " + shortForm);
		}
	}

	@Override
	public Attribute decodeNullableAttribute() throws MALException {
		return isNull() ? null : decodeAttribute();
	}

	@Override
	public MALListDecoder createListDecoder(final List list) throws IllegalArgumentException, MALException {
		if (list == null) {
			throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
		}
		return new SPPListDecoder(inputStream, list, properties);
	}

	/**
	 * Decodes a presence flag and thus determins whetther a null value is indicated (true) or not
	 * (false).
	 *
	 * @return True if presence flag indicates null value (i.e. if the flag evaluates to false),
	 * false otherwise.
	 * @throws MALException
	 */
	protected boolean isNull() throws MALException {
		return !decodeBoolean();
	}

	/**
	 * Reads one byte from inputStream. If no bytes are available a MALException is thrown.
	 *
	 * @return One byte read from inputStream is returned.
	 * @throws MALException
	 */
	private byte read() throws MALException {
		return read(1)[0];
	}

	/**
	 * Returns a number n bytes from inputStream. If less than n bytes are available a MALExcpetion
	 * is thrown.
	 *
	 * @param n Number of bytes to be read from inputStream.
	 * @return Array of bytes read from inputStream.
	 * @throws MALException
	 */
	protected byte[] read(final int n) throws MALException {
		try {
			if (inputStream.available() >= n) {
				byte[] bytes = new byte[n];
				if (n == 0 || inputStream.read(bytes, 0, n) == n) {
					return bytes;
				}
			}
			throw new MALException(INSUFFICIENT_DATA);
		} catch (IOException ex) {
			throw new MALException(ex.getMessage(), ex);
		}
	}

	/**
	 * Decodes a variable integer from inputStream. Respects the parameter VARINT_SUPPORTED and
	 * reads in a non-variable integer in case Varints are not supported.
	 *
	 * @param nOctets Number of bytes of the encoded value. Note that this is not the number of
	 * bytes that need to be read from the stream in case of variable integers.
	 * @param signed True if the value shall be treated as (in case of variable integers zig-zag
	 * encoded) signed value, false if it shall be treated as unsigned value.
	 * @return Decoded value.
	 * @throws MALException
	 */
	private BigInteger decodeVarint(final int nOctets, final boolean signed) throws MALException {
		if (!varintSupported) {
			byte[] b = read(nOctets);
			if (signed) {
				return new BigInteger(b);
			}
			return new BigInteger(1, b);
		}

		BigInteger ret = BigInteger.ZERO;
		int maxOctets = (int) Math.ceil(nOctets * 8.0 / 7.0);
		int i = 0;
		byte b;
		do {
			if (i >= maxOctets) {
				throw new MALException(INVALID_VALUE);
			}
			b = read();
			// With hypothetical operator overloading: ret = ret | ((b & 0b01111111) << (7 * i)))
			ret = ret.or(BigInteger.valueOf(b & 0b01111111).shiftLeft(7 * i));
			++i;
		} while (b >>> 7 != 0);
		if (i == maxOctets && (b & ((byte) 0b10000000 >> (7 * maxOctets - 8 * nOctets))) != 0) {
			// more bits set than allowed
			throw new MALException(INVALID_VALUE);
		}
		if (signed) {
			// With hypothetical operator overloading: ret = (ret >>> 1) ^ -(ret & 1)
			ret = ret.shiftRight(1).xor(ret.and(BigInteger.ONE).negate());
		}
		return ret;
	}

	/**
	 * Gets a formatter for Time fields. Return existing or create new one, if not existing.
	 *
	 * @return Time formatter allowing do decode times.
	 * @throws MALException
	 */
	private CCSDSTime getTimeFormatter() throws MALException {
		if (timeFormatter == null) {
			Configuration config = new Configuration(properties);
			timeFormatter = new CCSDSTime(config.timeCodeFormat(), config.timeEpoch(), config.timeEpochTimescale(), config.timeUnit());
		}
		return timeFormatter;
	}

	/**
	 * Gets a formatter for FineTime fields. Return existing or create new one, if not existing.
	 *
	 * @return Fine time formatter allowing do decode fine times.
	 * @throws MALException
	 */
	private CCSDSTime getFineTimeFormatter() throws MALException {
		if (fineTimeFormatter == null) {
			Configuration config = new Configuration(properties);
			fineTimeFormatter = new CCSDSTime(config.fineTimeCodeFormat(), config.fineTimeEpoch(), config.fineTimeEpochTimescale(), config.fineTimeUnit());
		}
		return fineTimeFormatter;
	}

	/**
	 * Gets a formatter for Duration fields. Return existing or create new one, if not existing.
	 *
	 * This method uses an arbitrary epoch (DURATION_EPOCH) to map relative times to absolute times
	 * and to use the standard time decoding methods.
	 *
	 * @return Duration formatter allowing do decode durations.
	 * @throws MALException
	 */
	private CCSDSTime getDurationFormatter() throws MALException {
		if (durationFormatter == null) {
			Configuration config = new Configuration(properties);
			durationFormatter = new CCSDSTime(config.durationCodeFormat(), Configuration.DURATION_EPOCH, Configuration.DURATION_EPOCH_TIMESCALE, config.durationUnit());
		}
		return durationFormatter;
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
