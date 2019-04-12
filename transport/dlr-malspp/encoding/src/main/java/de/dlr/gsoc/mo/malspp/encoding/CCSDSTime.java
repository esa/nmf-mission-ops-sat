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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALException;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

/**
 * Helper class for working with CCSDS time codes and preamble fields.
 */
public class CCSDSTime {

	private static final String WRONG_TIME_FORMAT = "Wrong time format specification.";
	private static final String NEGATIVE_TIME = "Time before epoch not allowed.";
	// TODO: Allow automatic download and/or manual definition of leap second file.
	private static final String OREKIT_UTC_TAI_FILE = "UTC-TAI.zip"; // contains UTC-TAI.history from https://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history
	private byte[] pField;
	private AbsoluteDate epoch;
	private int nOctets;
	private TimeCode timeCode;
	private int unitMultiplier;
	private Map<String, Object> properties;

	public static enum TimeCode {

		CUC, CDS, CCS
	};

	static {
		try {
			org.orekit.data.DataProvidersManager.getInstance().addProvider(new org.orekit.data.ClasspathCrawler(OREKIT_UTC_TAI_FILE));
		} catch (OrekitException ex) {
			throw new java.lang.ExceptionInInitializerError(ex.getMessage());
		}
	}

	/**
	 * Create a new CCSDS Time Code object by parsing a potentially multi-octet preamble field.
	 *
	 * @param pField Preamble field as byte array. A one or two-byte length array has to be
	 * provided.
	 * @param epoch Epoch as ISO-8601 formatted String as supported by the Orekit library. The CCSDS
	 * ASCII calendar segmented time code is a supported subset of ISO-8601.
	 * @param timeScale Timescale in which the epoch is interpreted if a CUC encoding is employed.
	 * See getTimeScale for possible values. UTC if null.
	 * @param unitMultiplier Number of time units that make up a second. Only used for CUC encoding.
	 * @throws MALException
	 */
	public CCSDSTime(final byte[] pField, final String epoch, final String timeScale, final int unitMultiplier) throws MALException {
		properties = new HashMap<>();
		this.unitMultiplier = unitMultiplier;
		parsePField(pField);
		parseEpoch(epoch, timeScale);
	}

	/**
	 * Create a new CCSDS Time Code object by parsing a single-octet preamble field.
	 *
	 * @param pField Preamble field as byte.
	 * @param epoch Epoch as ISO-8601 formatted String as supported by the Orekit library. The CCSDS
	 * ASCII calendar segmented time code is a supported subset of ISO-8601.
	 * @param timeScale Timescale in which the epoch is interpreted if a CUC encoding is employed.
	 * See getTimeScale for possible values. UTC if null.
	 * @param unitMultiplier Number of time units that make up a second. Only used for CUC encoding.
	 * @throws MALException
	 */
	public CCSDSTime(final byte pField, final String epoch, final String timeScale, final int unitMultiplier) throws MALException {
		this(new byte[]{pField}, epoch, timeScale, unitMultiplier);
	}

	/**
	 * Create a new CCSDS Time Code object by parsing a single- or multioctet preamble field given
	 * in binary representation as a String.
	 *
	 * @param pField Preamble field as String expressed in binary values.
	 * @param epoch Epoch as ISO-8601 formatted String as supported by the Orekit library. The CCSDS
	 * ASCII calendar segmented time code is a supported subset of ISO-8601.
	 * @param timeScale Timescale in which the epoch is interpreted if a CUC encoding is employed.
	 * See getTimeScale for possible values. UTC if null.
	 * @param unitMultiplier Number of time units that make up a second. Only used for CUC encoding.
	 * @throws MALException
	 */
	public CCSDSTime(final String pField, final String epoch, final String timeScale, final int unitMultiplier) throws MALException {
		this(binaryStringToByteArray(pField), epoch, timeScale, unitMultiplier);
	}

	/**
	 * Helper method for creating an AbsoluteDate object representing an epoch.
	 *
	 * @param epoch ISO-8601 formatted epoch string (only the subset allowed by Orekit is
	 * supported).
	 * @param timeScale Timescale in which the epoch is interpreted. See getTimeScale for possible
	 * values. UTC if null.
	 * @return AbsoluteDate object representing the epoch.
	 * @throws MALException
	 */
	public static AbsoluteDate createEpoch(final String epoch, final String timeScale) throws MALException {
		return new AbsoluteDate(epoch, getTimeScale(timeScale));
	}

	/**
	 * Parses the preamble field and sets the class' data members.
	 *
	 * If the epoch is an agency-defined epoch the epoch data member is set to null.
	 *
	 * @param pField Preamble field as byte array.
	 * @throws MALException
	 */
	private void parsePField(final byte[] pField) throws MALException {
		if (pField.length < 1 || pField.length > 2) {
			throw new MALException(WRONG_TIME_FORMAT);
		}

		if (pField.length == 2 && ((pField[0] & 0x80) == 0 || (pField[1] & 0x80) != 0)) {
			// If an extended P-Field is given it should be signalled in the first octet,
			// only extensions by one more octet are supported.
			throw new MALException(WRONG_TIME_FORMAT);
		}

		// time code format and epoch identification
		switch (pField[0] & 0b01110000) {
			case 0b00100000:
				// CUC, agency-defined epoch
				timeCode = TimeCode.CUC;
				epoch = null;
				break;
			case 0b00010000:
				// CUC, CCSDS epoch
				timeCode = TimeCode.CUC;
				epoch = AbsoluteDate.CCSDS_EPOCH;
				break;
			case 0b01000000:
				// CDS
				if (pField.length != 1) {
					// no P-Field extension allowed
					throw new MALException(WRONG_TIME_FORMAT);
				}
				timeCode = TimeCode.CDS;
				epoch = ((pField[0] & 0b00001000) >>> 3) == 0 ? AbsoluteDate.CCSDS_EPOCH : null;
				break;
			case 0b01010000:
				// CCS
				if (pField.length != 1) {
					// no P-Field extension allowed
					throw new MALException(WRONG_TIME_FORMAT);
				}
				timeCode = TimeCode.CCS;
				epoch = null;
				break;
			default:
				// only CUC, CDS and CCS are supported
				throw new MALException(WRONG_TIME_FORMAT);
		}

		// time code properties retrieval
		switch (timeCode) {
			case CUC:
				int nBasicOctets = ((pField[0] & 0b00001100) >>> 2) + 1;
				int nFractionalOctets = pField[0] & 0b00000011;
				if (pField.length == 2) {
					nBasicOctets += (pField[1] & 0b01100000) >>> 5;
					nFractionalOctets += (pField[1] & 0b00011100) >>> 2;
				}
				properties.put("nBasicOctets", nBasicOctets);
				properties.put("nFractionalOctets", nFractionalOctets);
				nOctets = nBasicOctets + nFractionalOctets;
				break;
			case CDS:
				int nDayOctets = ((pField[0] & 0b00000100) >>> 2) == 0 ? 2 : 3;
				int nSubMilliOctets = (pField[0] & 0b00000011) << 1;
				if (nSubMilliOctets == 6) {
					// reserved for future use
					throw new MALException(WRONG_TIME_FORMAT);
				}
				properties.put("nDayOctets", nDayOctets);
				properties.put("nSubMilliOctets", nSubMilliOctets);
				nOctets = nDayOctets + nSubMilliOctets + 4;
				break;
			case CCS:
				boolean isDOY = ((pField[0] & 0b00001000) >>> 3) == 1;
				int nResOctets = pField[0] & 0b00000111;
				if (nResOctets == 7) {
					// not used
					throw new MALException(WRONG_TIME_FORMAT);
				}
				properties.put("isDOY", isDOY);
				properties.put("nResOctets", nResOctets);
				nOctets = nResOctets + 7;
				break;
		}
		this.pField = pField;
	}

	/**
	 * Parses the epoch String parameter and sets the class' epoch data member.
	 *
	 * If there is a mismatch between parsed epoch and the epoch parameter a MALException is thrown.
	 *
	 * @param epoch Epoch as ISO-8601 formatted String as supported by the Orekit library. The CCSDS
	 * ASCII calendar segmented time code is a supported subset of ISO-8601.
	 * @param timeScale Timescale in which the epoch is interpreted. See getTimeScale for possible
	 * values. UTC if null.
	 * @throws MALException
	 */
	private void parseEpoch(final String epoch, final String timeScale) throws MALException {
		AbsoluteDate parsedEpoch;
		try {
			parsedEpoch = new AbsoluteDate(epoch, getTimeScale(timeScale));
		} catch (IllegalArgumentException ex) {
			throw new MALException(ex.getMessage(), ex);
		}
		if (this.epoch != null && !this.epoch.equals(parsedEpoch)) {
			// mismatch between epoch parsed from P-field and epoch from parameter
			throw new MALException(WRONG_TIME_FORMAT);
		}
		this.epoch = parsedEpoch;
	}

	/**
	 * Decodes a time as one of the CCSDS time code formats CUC, CDS or CCS as specified by the
	 * preamble field.
	 *
	 * @param tField Byte array containing the encoded time value.
	 * @return An AbsoluteDate that contains the decoded time value.
	 * @throws MALException
	 */
	public AbsoluteDate getDecodedTime(final byte[] tField) throws MALException {
		try {
			switch (timeCode) {
				case CUC:
					// TODO: A bit dirty because time unit first is taken to be one second and
					// afterwards corrected for the real time unit used. This can be made prettier
					// by implementing parseCCSDSUnsegmentedTimeCode that respechts the time unit.
					AbsoluteDate possiblyWrongTime = AbsoluteDate.parseCCSDSUnsegmentedTimeCode(pField[0], pField.length == 2 ? pField[1] : 0, tField, epoch);
					if (1 == unitMultiplier) {
						// time unit is one second, i.e. no correction necessary
						return possiblyWrongTime;
					}
					return new AbsoluteDate(epoch, possiblyWrongTime.durationFrom(epoch) / unitMultiplier);
				case CDS:
					DateComponents dEpoch = epoch.getComponents(TimeScalesFactory.getUTC()).getDate(); // CDS is a UTC-based timecode
					return AbsoluteDate.parseCCSDSDaySegmentedTimeCode(pField[0], tField, dEpoch);
				case CCS:
					return AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode(pField[0], tField);
				default:
					throw new MALException(WRONG_TIME_FORMAT);
			}
		} catch (OrekitException ex) {
			throw new MALException(ex.getMessage(), ex);
		}
	}

	/**
	 * Encode a time as one of the CCSDS Time Codes CUC, CDS or CCS.
	 *
	 * The basic unit for CUC is assumed to be one second. Beware that while a resolution of up to
	 * 10 octets for the fractional time unit is supported, accuracy may be lower.
	 *
	 * @param coarseTime Time to encode as AbsoluteDate, needs to represent a whole second. This
	 * parameter is unused and thus may be null for CDS or CCS.
	 * @param fineTime (Fractional) seconds from coarseTime as AbsoluteDate to encode.
	 * @param mayBeNegative True, if the time to encode is allowed to be before the epoch. False, if
	 * only times equal to the epoch or in the future of the epoch may be allowed.
	 * @return T-field as byte array with encoded time value.
	 * @throws MALException
	 */
	public byte[] getEncodedTime(final AbsoluteDate coarseTime, final AbsoluteDate fineTime, boolean mayBeNegative) throws MALException {
		if (!mayBeNegative && fineTime.compareTo(epoch) < 0) {
			throw new MALException(NEGATIVE_TIME);
		}
		switch (timeCode) {
			case CUC:
				return encodeCUC(coarseTime, fineTime);
			case CDS:
				return encodeCDS(fineTime);
			case CCS:
				return encodeCCS(fineTime);
			default:
				throw new MALException(WRONG_TIME_FORMAT);
		}
	}

	/**
	 * Encodes a time as CCSDS Unsegmented Time Code.
	 *
	 * @param coarseTime Time to encode as AbsoluteDate, needs to represent a whole second.
	 * @param fineTime (Fractional) seconds from coarseTime as AbsoluteDate to encode.
	 * @return T-field as byte array with encoded time value.
	 * @throws MALException
	 */
	private byte[] encodeCUC(final AbsoluteDate coarseTime, final AbsoluteDate fineTime) throws MALException {
		final int nBasicOctets = (Integer) properties.get("nBasicOctets");
		final int nFractionalOctets = (Integer) properties.get("nFractionalOctets");
		ByteArrayOutputStream tField = new ByteArrayOutputStream(nOctets);

		long coarseSeconds = (long) coarseTime.durationFrom(epoch) * unitMultiplier;
		long fineSeconds = Math.round(fineTime.durationFrom(coarseTime) * unitMultiplier * (1L << (8 * nFractionalOctets)));
		for (int i = nBasicOctets; i > 0; i--) {
			tField.write((byte) (coarseSeconds >> (8 * (i - 1))));
		}
		for (int i = nFractionalOctets; i > 0; i--) {
			tField.write((byte) (fineSeconds >> (8 * (i - 1))));
		}
		return tField.toByteArray();
	}

	/**
	 * Encodes a time as CCSDS Segmented Time Code.
	 *
	 * @param coarseTime Time to encode as AbsoluteDate, needs to represent a whole second.
	 * @param fineTime (Fractional) seconds from coarseTime as AbsoluteDate to encode.
	 * @return T-field as byte array with encoded time value.
	 * @throws MALException
	 */
	private byte[] encodeCDS(final AbsoluteDate time) throws MALException {
		final int nDayOctets = (Integer) properties.get("nDayOctets");
		final int nSubMilliOctets = (Integer) properties.get("nSubMilliOctets");
		ByteArrayOutputStream tField = new ByteArrayOutputStream(nOctets);

		TimeScale utc;
		try {
			utc = TimeScalesFactory.getUTC();
		} catch (OrekitException ex) {
			throw new MALException(ex.getMessage(), ex);
		}
		DateTimeComponents dateTime = time.getComponents(utc);

		int days = dateTime.getDate().getJ2000Day() - epoch.getComponents(utc).getDate().getJ2000Day();
		for (int i = nDayOctets; i > 0; i--) {
			tField.write((byte) (days >> (8 * (i - 1))));
		}

		double seconds = dateTime.getTime().getSecondsInDay();
		long millisecs = (long) (seconds * 1000);
		for (int i = 4; i > 0; i--) {
			tField.write((byte) (millisecs >> (8 * (i - 1))));
		}

		int resFactor = 0;
		switch (nSubMilliOctets) {
			case 0: // submillisecond segement is absent
				break;
			case 2:	// microsecond resolution
				resFactor = 1000;
				break;
			case 4:	// picosecond resolution
				resFactor = 1000000000;
				break;
			default:
				throw new MALException(WRONG_TIME_FORMAT);
		}
		// Don't calculate subMilliSecs from seconds in day due to possible loss of precision.
		long subMilliSecs = (long) ((dateTime.getTime().getSecond() * 1000 % 1) * resFactor);
		for (int i = nSubMilliOctets; i > 0; i--) {
			tField.write((byte) (subMilliSecs >> (8 * (i - 1))));
		}
		return tField.toByteArray();
	}

	/**
	 * Encodes a time as CCSDS Calendar Segmented Time Code.
	 *
	 * @param time Time to encode as AbsoluteDate.
	 * @return T-field as byte array with encoded time value.
	 * @throws MALException
	 */
	private byte[] encodeCCS(final AbsoluteDate time) throws MALException {
		final boolean isDOY = (Boolean) properties.get("isDOY");
		final int nResOctets = (Integer) properties.get("nResOctets");
		ByteArrayOutputStream tField = new ByteArrayOutputStream(nOctets);

		TimeScale utc;
		try {
			utc = TimeScalesFactory.getUTC();
		} catch (OrekitException ex) {
			throw new MALException(ex.getMessage(), ex);
		}
		DateTimeComponents dateTime = time.getComponents(utc);
		DateComponents date = dateTime.getDate();
		TimeComponents t = dateTime.getTime();

		int year = date.getYear();
		tField.write(year >> 8);
		tField.write(year);
		if (isDOY) {
			int doy = date.getDayOfYear();
			tField.write(doy >> 8);
			tField.write(doy);
		} else {
			tField.write(date.getMonth());
			tField.write(date.getDay());
		}
		tField.write(t.getHour());
		tField.write(t.getMinute());
		double second = t.getSecond();
		tField.write((byte) second);
		// Convert to long value here to preserve picosecond accuracy.
		long picoSecond = (long) (second * 1000000000000L) % 1000000000000L;
		long div = 10000000000L;
		for (int i = 0; i < nResOctets; i++) {
			tField.write((byte) (picoSecond / div));
			picoSecond %= div;
			div /= 100;
		}
		return tField.toByteArray();
	}

	/**
	 * Returns the number of bytes of the time (T) field.
	 *
	 * @return Length of T field in bytes.
	 */
	public int getDataLength() {
		return nOctets;
	}

	/**
	 * Returns the time code format.
	 *
	 * @return One of TimeCode.CUC, CDS or CCS.
	 */
	public TimeCode getTimeCode() {
		return timeCode;
	}

	/**
	 * Returns the epoch.
	 *
	 * @return Epoch as AbsoluteDate.
	 */
	public AbsoluteDate getEpoch() {
		return epoch;
	}

	/**
	 * Return a timescale according to the supplied string.
	 *
	 * @param timeScale Timescale to be returned. If null or unknown return UTC timescale. Possible
	 * values are UTC, TAI, GMST, GPS, GST, TCB, TCG, TDB, TT. This string is case-insensitive.
	 * @return The timescale according to the supplied parameter.
	 * @throws MALException
	 */
	private static TimeScale getTimeScale(final String timeScale) throws MALException {
		try {
			if (null == timeScale) {
				return TimeScalesFactory.getUTC();
			}
			switch (timeScale.toUpperCase(Locale.ROOT)) {
				case "UTC":
					return TimeScalesFactory.getUTC();
				case "TAI":
					return TimeScalesFactory.getTAI();
				case "GMST":
					return TimeScalesFactory.getGMST(IERSConventions.IERS_2010, false);
				case "GPS":
					return TimeScalesFactory.getGPS();
				case "GST":
					return TimeScalesFactory.getGST();
				case "TCB":
					return TimeScalesFactory.getTCB();
				case "TCG":
					return TimeScalesFactory.getTCG();
				case "TDB":
					return TimeScalesFactory.getTDB();
				case "TT":
					return TimeScalesFactory.getTT();
			}
			return TimeScalesFactory.getUTC();
		} catch (OrekitException ex) {
			throw new MALException(ex.getMessage(), ex);
		}
	}

	/**
	 * Construct a byte array from a String given in binary representation.
	 *
	 * @param s String in binary representation, length needs to be multiple of octets.
	 * @return Byte array converted from the binary number in the String.
	 */
	private static byte[] binaryStringToByteArray(final String s) {
		byte[] ba = new byte[s.length() / 8];
		for (int i = 0; i < s.length() / 8; i++) {
			ba[i] = (byte) Short.parseShort(s.substring(i * 8, (i + 1) * 8), 2);
		}
		return ba;
	}
}
