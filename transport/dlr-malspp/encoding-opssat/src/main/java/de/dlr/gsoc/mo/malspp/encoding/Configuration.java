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

import java.util.Map;

public class Configuration {

	private final Map properties;
	// Mapping configuration parameter property names
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

	// Global constants
	protected static final String DURATION_EPOCH = "1958-01-01T00:00:00.000"; // in principle arbitrary, but user may specify CCSDS epoch for time code duration, which wpuld lead to problems
	protected static final String DURATION_EPOCH_TIMESCALE = "TAI"; // same as above
	// PENDING: Epoch for FineTime in MAL Java API unclear. Here: Use 2013-01-01T00:00:00.000 TAI.
	protected static final String MAL_FINE_TIME_EPOCH = "2013-01-01T00:00:00.000";
	protected static final String MAL_FINE_TIME_EPOCH_TIMESCALE = "TAI";
	protected static final String JAVA_EPOCH = "1970-01-01T00:00:00.000"; // needed due to bug in Orekit
	protected static final String JAVA_EPOCH_TIMESCALE = "UTC";

	// time unit string
	private static final String UNIT_SECOND = "second";
	private static final String UNIT_MILLISECOND = "millisecond";

	public Configuration(final Map properties) {
		this.properties = properties;
	}

	public String timeCodeFormat() {
		return (String) properties.get(PROPERTY_TIME_CODE_FORMAT);
	}

	public String timeEpoch() {
		return (String) properties.get(PROPERTY_TIME_EPOCH);
	}

	public String timeEpochTimescale() {
		return (String) properties.get(PROPERTY_TIME_EPOCH_TIMESCALE);
	}

	/**
	 * Returns the factor with which to multiply the basic time unit to get the number of seconds.
	 *
	 * @param propertyName Name of the property to read out.
	 * @return Multiplier which denotes the number of time units that make up a second. Defaults to
	 * 1.
	 */
	private int getUnit(String propertyName) {
		String unitProperty = (String) properties.get(propertyName);
		if (null == unitProperty) {
			// time unit standard is one second if not specified otherwise
			return 1;
		}
		switch (unitProperty) {
			case UNIT_SECOND:
				return 1;
			case UNIT_MILLISECOND:
				return 1000;
		}
		return 1;
	}

	public int timeUnit() {
		return getUnit(PROPERTY_TIME_UNIT);
	}

	public int fineTimeUnit() {
		return getUnit(PROPERTY_FINE_TIME_UNIT);
	}

	public int durationUnit() {
		return getUnit(PROPERTY_DURATION_UNIT);
	}

	public String fineTimeCodeFormat() {
		return (String) properties.get(PROPERTY_FINE_TIME_CODE_FORMAT);
	}

	public String fineTimeEpoch() {
		return (String) properties.get(PROPERTY_FINE_TIME_EPOCH);
	}

	public String fineTimeEpochTimescale() {
		return (String) properties.get(PROPERTY_FINE_TIME_EPOCH_TIMESCALE);
	}

	public String durationCodeFormat() {
		return (String) properties.get(PROPERTY_DURATION_CODE_FORMAT);
	}

	public boolean varintSupported() {
		return Boolean.valueOf((String) properties.get(PROPERTY_VARINT_SUPPORTED));
	}
}
