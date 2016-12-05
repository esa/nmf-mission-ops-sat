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

import de.dlr.gsoc.mo.malspp.encoding.SPPEncoder;
import de.dlr.gsoc.mo.malspp.encoding.Configuration;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.ccsds.moims.mo.mal.MALListEncoder;
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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SPPEncoderTest {

	private static ByteArrayOutputStream outputStream;
	private static SPPEncoder encoder;

	@Before
	public void setUp() {
		outputStream = new ByteArrayOutputStream();
		Map<String, Object> properties = new HashMap<>();
		properties.put("de.dlr.gsoc.mo.malspp.VARINT_SUPPORTED", "TRUE");
		encoder = new SPPEncoder(outputStream, properties);
	}

	/**
	 * Test of encodeBoolean method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeBoolean1() throws Exception {
		encoder.encodeBoolean(Boolean.FALSE);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeBoolean2() throws Exception {
		encoder.encodeBoolean(Boolean.TRUE);
		assertArrayEquals(new byte[]{1}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeBoolean3() throws Exception {
		encoder.encodeBoolean(Boolean.TRUE);
		encoder.encodeBoolean(Boolean.FALSE);
		encoder.encodeBoolean(Boolean.TRUE);
		assertArrayEquals(new byte[]{1, 0, 1}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeBoolean method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeBooleanExpectedException() throws Exception {
		encoder.encodeBoolean(null);
	}

	/**
	 * Test of encodeNullableBoolean method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableBoolean1() throws Exception {
		encoder.encodeNullableBoolean(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableBoolean2() throws Exception {
		encoder.encodeNullableBoolean(Boolean.FALSE);
		assertArrayEquals(new byte[]{1, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableBoolean3() throws Exception {
		encoder.encodeNullableBoolean(Boolean.TRUE);
		assertArrayEquals(new byte[]{1, 1}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableBoolean4() throws Exception {
		encoder.encodeNullableBoolean(Boolean.TRUE);
		encoder.encodeNullableBoolean(null);
		encoder.encodeNullableBoolean(Boolean.TRUE);
		encoder.encodeNullableBoolean(Boolean.FALSE);
		assertArrayEquals(new byte[]{1, 1, 0, 1, 1, 1, 0}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeFloat method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeFloat1() throws Exception {
		encoder.encodeFloat(Float.NaN);
		assertArrayEquals(new byte[]{(byte) 0b01111111, (byte) 0b11000000, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFloat2() throws Exception {
		encoder.encodeFloat(Float.NEGATIVE_INFINITY);
		assertArrayEquals(new byte[]{(byte) 0b11111111, (byte) 0b10000000, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFloat3() throws Exception {
		encoder.encodeFloat(Float.POSITIVE_INFINITY);
		assertArrayEquals(new byte[]{(byte) 0b01111111, (byte) 0b10000000, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFloat4() throws Exception {
		encoder.encodeFloat((float) +0.0);
		assertArrayEquals(new byte[]{0, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFloat5() throws Exception {
		encoder.encodeFloat((float) -0.0);
		assertArrayEquals(new byte[]{(byte) 0b10000000, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFloat6() throws Exception {
		encoder.encodeFloat(Float.MIN_VALUE);
		assertArrayEquals(new byte[]{0, 0, 0, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFloat7() throws Exception {
		encoder.encodeFloat(Float.MAX_VALUE);
		assertArrayEquals(new byte[]{(byte) 0b01111111, (byte) 0b01111111, (byte) 0b11111111, (byte) 0b11111111}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableFloat method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableFloat() throws Exception {
		encoder.encodeNullableFloat((float) -0.0);
		encoder.encodeNullableFloat(null);
		encoder.encodeNullableFloat(Float.NaN);
		encoder.encodeNullableFloat(Float.MIN_NORMAL);
		assertArrayEquals(new byte[]{
			1, (byte) 0b10000000, 0, 0, 0,
			0,
			1, (byte) 0b01111111, (byte) 0b11000000, 0, 0,
			1, 0, (byte) 0b10000000, 0, 0}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeFloat method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeFloatExpectedException() throws Exception {
		encoder.encodeFloat(null);
	}

	/**
	 * Test of encodeDouble method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeDouble1() throws Exception {
		encoder.encodeDouble(Double.NaN);
		assertArrayEquals(new byte[]{
			(byte) 0b01111111, (byte) 0b11111000, 0, 0,
			0, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeDouble2() throws Exception {
		encoder.encodeDouble(Double.NEGATIVE_INFINITY);
		assertArrayEquals(new byte[]{
			(byte) 0b11111111, (byte) 0b11110000, 0, 0,
			0, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeDouble3() throws Exception {
		encoder.encodeDouble(Double.POSITIVE_INFINITY);
		assertArrayEquals(new byte[]{
			(byte) 0b01111111, (byte) 0b11110000, 0, 0,
			0, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeDouble4() throws Exception {
		encoder.encodeDouble(+0.0);
		assertArrayEquals(new byte[]{
			0, 0, 0, 0,
			0, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeDouble5() throws Exception {
		encoder.encodeDouble(-0.0);
		assertArrayEquals(new byte[]{
			(byte) 0b10000000, 0, 0, 0,
			0, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeDouble6() throws Exception {
		encoder.encodeDouble(Double.MIN_VALUE);
		assertArrayEquals(new byte[]{
			0, 0, 0, 0,
			0, 0, 0, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeDouble7() throws Exception {
		encoder.encodeDouble(Double.MAX_VALUE);
		assertArrayEquals(new byte[]{
			(byte) 0b01111111, (byte) 0b11101111, (byte) 0b11111111, (byte) 0b11111111,
			(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableDouble method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableDouble() throws Exception {
		encoder.encodeNullableDouble(null);
		encoder.encodeNullableDouble(-0.0);
		encoder.encodeNullableDouble(Double.NaN);
		encoder.encodeNullableDouble(Double.MIN_NORMAL);
		assertArrayEquals(new byte[]{
			0,
			1, (byte) 0b10000000, 0, 0, 0, 0, 0, 0, 0,
			1, (byte) 0b01111111, (byte) 0b11111000, 0, 0, 0, 0, 0, 0,
			1, 0, (byte) 0b00010000, 0, 0, 0, 0, 0, 0}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeDouble method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeDoubleExpectedException() throws Exception {
		encoder.encodeDouble(null);
	}

	/**
	 * Test of encodeOctet method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeOctet1() throws Exception {
		encoder.encodeOctet((byte) 127);
		assertArrayEquals(new byte[]{(byte) 0b01111111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeOctet2() throws Exception {
		encoder.encodeOctet((byte) -128);
		assertArrayEquals(new byte[]{(byte) 0b10000000}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeOctet3() throws Exception {
		encoder.encodeOctet((byte) 1);
		assertArrayEquals(new byte[]{(byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeOctet4() throws Exception {
		encoder.encodeOctet((byte) -1);
		assertArrayEquals(new byte[]{(byte) 0b11111111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeOctet5() throws Exception {
		encoder.encodeOctet((byte) 0);
		assertArrayEquals(new byte[]{(byte) 0b00000000}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeOctet6() throws Exception {
		encoder.encodeOctet((byte) -42);
		encoder.encodeOctet((byte) 47);
		assertArrayEquals(new byte[]{(byte) 0b11010110, (byte) 0b00101111}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeOctet method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeOctetExpectedException() throws Exception {
		encoder.encodeOctet(null);
	}

	/**
	 * Test of encodeNullableOctet method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableOctet1() throws Exception {
		encoder.encodeNullableOctet(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableOctet2() throws Exception {
		encoder.encodeNullableOctet((byte) 127);
		encoder.encodeNullableOctet(null);
		encoder.encodeNullableOctet((byte) 0);
		encoder.encodeNullableOctet((byte) -47);
		assertArrayEquals(new byte[]{
			1, (byte) 0b01111111,
			0,
			1, (byte) 0b00000000,
			1, (byte) 0b11010001
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeUOctet method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeUOctet1() throws Exception {
		encoder.encodeUOctet(new UOctet((short) 0));
		assertArrayEquals(new byte[]{(byte) 0b00000000}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUOctet2() throws Exception {
		encoder.encodeUOctet(new UOctet((short) 255));
		assertArrayEquals(new byte[]{(byte) 0b11111111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUOctet3() throws Exception {
		encoder.encodeUOctet(new UOctet((short) 1));
		assertArrayEquals(new byte[]{(byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUOctet4() throws Exception {
		encoder.encodeUOctet(new UOctet((short) 42));
		assertArrayEquals(new byte[]{(byte) 0b00101010}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUOctet5() throws Exception {
		encoder.encodeUOctet(new UOctet((short) 207));
		assertArrayEquals(new byte[]{(byte) 0b11001111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUOctet6() throws Exception {
		encoder.encodeUOctet(new UOctet((short) 128));
		encoder.encodeUOctet(new UOctet((short) 14));
		assertArrayEquals(new byte[]{(byte) 0b10000000, (byte) 0b00001110}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableUOctet method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableUOctet1() throws Exception {
		encoder.encodeNullableUOctet(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableUOctet2() throws Exception {
		encoder.encodeNullableUOctet(new UOctet((short) 254));
		encoder.encodeNullableUOctet(new UOctet((short) 0));
		encoder.encodeNullableUOctet(null);
		encoder.encodeNullableUOctet(new UOctet((short) 1));
		assertArrayEquals(new byte[]{
			1, (byte) 0b11111110,
			1, (byte) 0b00000000,
			0,
			1, (byte) 0b00000001
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeUOctet method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeUOctetExpectedException() throws Exception {
		encoder.encodeUOctet(null);
	}

	/**
	 * Test of encodeShort method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeShort1() throws Exception {
		encoder.encodeShort((short) 0);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort1b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeShort((short) 0);
		assertArrayEquals(new byte[]{0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort2() throws Exception {
		encoder.encodeShort((short) -1); // mapped to 1
		assertArrayEquals(new byte[]{(byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeShort((short) -1);
		assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort3() throws Exception {
		encoder.encodeShort((short) 1); // mapped to 2
		assertArrayEquals(new byte[]{(byte) 0b00000010}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort3b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeShort((short) 1);
		assertArrayEquals(new byte[]{0, (byte) 0x01}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort4() throws Exception {
		encoder.encodeShort((short) -42); // mapped to 83
		assertArrayEquals(new byte[]{(byte) 0b01010011}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort4b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeShort((short) -42);
		assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xD6}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort5() throws Exception {
		encoder.encodeShort((short) 42); // mapped to 84
		assertArrayEquals(new byte[]{(byte) 0b01010100}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort5b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeShort((short) 42);
		assertArrayEquals(new byte[]{0, (byte) 0x2A}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort6() throws Exception {
		encoder.encodeShort((short) 64); // mapped to 128
		assertArrayEquals(new byte[]{(byte) 0b10000000, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort6b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeShort((short) 64);
		assertArrayEquals(new byte[]{0, (byte) 0x40}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort7() throws Exception {
		encoder.encodeShort((short) -8192); // mapped to 16383
		assertArrayEquals(new byte[]{(byte) 0b11111111, (byte) 0b01111111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort7b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeShort((short) -8192);
		assertArrayEquals(new byte[]{(byte) 0xE0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort8() throws Exception {
		encoder.encodeShort((short) 8192); // mapped to 16384
		assertArrayEquals(new byte[]{(byte) 0b10000000, (byte) 0b10000000, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort8b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeShort((short) 8192);
		assertArrayEquals(new byte[]{(byte) 0x20, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort9() throws Exception {
		encoder.encodeShort((short) 32767); // mapped to 65534
		assertArrayEquals(new byte[]{(byte) 0b11111110, (byte) 0b11111111, (byte) 0b00000011}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort9b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeShort((short) 32767);
		assertArrayEquals(new byte[]{(byte) 0x7F, (byte) 0xFF}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort10() throws Exception {
		encoder.encodeShort((short) -32768); // mapped to 65535
		assertArrayEquals(new byte[]{(byte) 0b11111111, (byte) 0b11111111, (byte) 0b00000011}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeShort10b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeShort((short) -32768);
		assertArrayEquals(new byte[]{(byte) 0x80, 0}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableShort method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableShort1() throws Exception {
		encoder.encodeNullableShort(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableShort2() throws Exception {
		encoder.encodeNullableShort((short) -31415); // mapped to 62829
		encoder.encodeNullableShort((short) 8193);   // mapped to 16386
		encoder.encodeNullableShort((short) 0);
		encoder.encodeNullableShort((short) -256);   // mapped to 511
		encoder.encodeNullableShort(null);
		assertArrayEquals(new byte[]{
			1, (byte) 0b11101101, (byte) 0b11101010, (byte) 0b00000011,
			1, (byte) 0b10000010, (byte) 0b10000000, (byte) 0b00000001,
			1, (byte) 0,
			1, (byte) 0b11111111, (byte) 0b00000011,
			0
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableShort2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeNullableShort((short) -31415);
		encoder.encodeNullableShort((short) 8193);
		encoder.encodeNullableShort((short) 0);
		encoder.encodeNullableShort((short) -256);
		encoder.encodeNullableShort(null);
		assertArrayEquals(new byte[]{
			1, (byte) 0x85, (byte) 0x49,
			1, (byte) 0x20, (byte) 0x01,
			1, 0, 0,
			1, (byte) 0xFF, 0,
			0
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeShort method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeShortExpectedException() throws Exception {
		encoder.encodeShort(null);
	}

	/**
	 * Test of encodeUShort method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeUShort1() throws Exception {
		encoder.encodeUShort(new UShort(0));
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort1b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeUShort(new UShort(0));
		assertArrayEquals(new byte[]{0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort2() throws Exception {
		encoder.encodeUShort(new UShort(1));
		assertArrayEquals(new byte[]{(byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeUShort(new UShort(1));
		assertArrayEquals(new byte[]{0, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort3() throws Exception {
		encoder.encodeUShort(new UShort(2));
		assertArrayEquals(new byte[]{(byte) 0b00000010}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort4() throws Exception {
		encoder.encodeUShort(new UShort(127));
		assertArrayEquals(new byte[]{(byte) 0b01111111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort4b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeUShort(new UShort(127));
		assertArrayEquals(new byte[]{0, (byte) 0b01111111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort5() throws Exception {
		encoder.encodeUShort(new UShort(128));
		assertArrayEquals(new byte[]{(byte) 0b10000000, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort5b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeUShort(new UShort(128));
		assertArrayEquals(new byte[]{0, (byte) 0b10000000}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort6() throws Exception {
		encoder.encodeUShort(new UShort(129));
		assertArrayEquals(new byte[]{(byte) 0b10000001, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort7() throws Exception {
		encoder.encodeUShort(new UShort(255));
		assertArrayEquals(new byte[]{(byte) 0b11111111, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort7b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeUShort(new UShort(255));
		assertArrayEquals(new byte[]{0, (byte) 0b11111111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort8() throws Exception {
		encoder.encodeUShort(new UShort(256));
		assertArrayEquals(new byte[]{(byte) 0b10000000, (byte) 0b00000010}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort9() throws Exception {
		encoder.encodeUShort(new UShort(16383));
		assertArrayEquals(new byte[]{(byte) 0b11111111, (byte) 0b01111111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort10() throws Exception {
		encoder.encodeUShort(new UShort(16384));
		assertArrayEquals(new byte[]{(byte) 0b10000000, (byte) 0b10000000, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort11() throws Exception {
		encoder.encodeUShort(new UShort(16385));
		assertArrayEquals(new byte[]{(byte) 0b10000001, (byte) 0b10000000, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort12() throws Exception {
		encoder.encodeUShort(new UShort(32768));
		assertArrayEquals(new byte[]{(byte) 0b10000000, (byte) 0b10000000, (byte) 0b00000010}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort12b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeUShort(new UShort(32768));
		assertArrayEquals(new byte[]{(byte) 0b10000000, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort13() throws Exception {
		encoder.encodeUShort(new UShort(65535));
		assertArrayEquals(new byte[]{(byte) 0b11111111, (byte) 0b11111111, (byte) 0b00000011}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort13b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeUShort(new UShort(65535));
		assertArrayEquals(new byte[]{(byte) 0b11111111, (byte) 0b11111111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUShort14() throws Exception {
		encoder.encodeUShort(new UShort(40363));
		assertArrayEquals(new byte[]{(byte) 0b10101011, (byte) 0b10111011, (byte) 0b00000010}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableUShort method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableUShort1() throws Exception {
		encoder.encodeNullableUShort(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableUShort2() throws Exception {
		encoder.encodeNullableUShort(new UShort(65535));
		encoder.encodeNullableUShort(new UShort(0));
		encoder.encodeNullableUShort(null);
		encoder.encodeNullableUShort(new UShort(31415));
		encoder.encodeNullableUShort(new UShort(256));
		assertArrayEquals(new byte[]{
			1, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b00000011,
			1, (byte) 0,
			0,
			1, (byte) 0b10110111, (byte) 0b11110101, (byte) 0b00000001,
			1, (byte) 0b10000000, (byte) 0b00000010
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableUShort2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeNullableUShort(new UShort(65535));
		encoder.encodeNullableUShort(new UShort(0));
		encoder.encodeNullableUShort(null);
		encoder.encodeNullableUShort(new UShort(31415));
		encoder.encodeNullableUShort(new UShort(256));
		assertArrayEquals(new byte[]{
			1, (byte) 0b11111111, (byte) 0b11111111,
			1, 0, 0,
			0,
			1, (byte) 0b01111010, (byte) 0b10110111,
			1, (byte) 0b00000001, 0
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeUShort method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeUShortExpectedException() throws Exception {
		encoder.encodeUShort(null);
	}

	/**
	 * Test of encodeInteger method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeInteger1() throws Exception {
		encoder.encodeInteger(0);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeInteger1b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeInteger(0);
		assertArrayEquals(new byte[]{0, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeInteger2() throws Exception {
		encoder.encodeInteger(-2147483648); // mapped to 4294967295L
		assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeInteger2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeInteger(-2147483648);
		assertArrayEquals(new byte[]{(byte) 0x80, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeInteger3() throws Exception {
		encoder.encodeInteger(2147483647); // mapped to 4294967294L
		assertArrayEquals(new byte[]{(byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeInteger3b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeInteger(2147483647);
		assertArrayEquals(new byte[]{(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableInteger method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableInteger1() throws Exception {
		encoder.encodeNullableInteger(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableInteger2() throws Exception {
		encoder.encodeNullableInteger(1); // mapped to 2
		encoder.encodeNullableInteger(null);
		encoder.encodeNullableInteger(-1); // mapped to 1
		encoder.encodeNullableInteger(123456854); // mapped to 246913708
		assertArrayEquals(new byte[]{
			1, (byte) 0b00000010,
			0,
			1, (byte) 0b00000001,
			1, (byte) 0b10101100, (byte) 0b10110101, (byte) 0b11011110, (byte) 0b01110101
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableInteger2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeNullableInteger(1);
		encoder.encodeNullableInteger(null);
		encoder.encodeNullableInteger(-1);
		encoder.encodeNullableInteger(123456854);
		assertArrayEquals(new byte[]{
			1, 0, 0, 0, (byte) 0x01,
			0,
			1, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			1, (byte) 0x07, (byte) 0x5B, (byte) 0xCD, (byte) 0x56
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeInteger method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeIntegerExpectedException() throws Exception {
		encoder.encodeInteger(null);
	}

	/**
	 * Test of encodeUInteger method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeUInteger1() throws Exception {
		encoder.encodeUInteger(new UInteger(0));
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUInteger1b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeUInteger(new UInteger(0));
		assertArrayEquals(new byte[]{0, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUInteger2() throws Exception {
		encoder.encodeUInteger(new UInteger(4294967295L));
		assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUInteger2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeUInteger(new UInteger(4294967295L));
		assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUInteger3() throws Exception {
		encoder.encodeUInteger(new UInteger(31415));
		assertArrayEquals(new byte[]{(byte) 0b10110111, (byte) 0b11110101, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeUInteger3b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeUInteger(new UInteger(31415));
		assertArrayEquals(new byte[]{0, 0, (byte) 0x7A, (byte) 0xB7}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableUInteger method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableUInteger1() throws Exception {
		encoder.encodeNullableUInteger(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableUInteger2() throws Exception {
		encoder.encodeNullableUInteger(new UInteger(1));
		encoder.encodeNullableUInteger(new UInteger(16363));
		encoder.encodeNullableUInteger(new UInteger(2147559493L));
		encoder.encodeNullableUInteger(null);
		assertArrayEquals(new byte[]{
			1, (byte) 0b00000001,
			1, (byte) 0b11101011, (byte) 0b01111111,
			1, (byte) 0b11000101, (byte) 0b11010000, (byte) 0b10000100, (byte) 0b10000000, (byte) 0b00001000,
			0
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableUInteger2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeNullableUInteger(new UInteger(1));
		encoder.encodeNullableUInteger(new UInteger(16363));
		encoder.encodeNullableUInteger(new UInteger(2147559493L));
		encoder.encodeNullableUInteger(null);
		assertArrayEquals(new byte[]{
			1, 0, 0, 0, (byte) 0x01,
			1, 0, 0, (byte) 0x3F, (byte) 0xEB,
			1, (byte) 0x80, (byte) 0x01, (byte) 0x28, (byte) 0x45,
			0
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeUInteger method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeUIntegerExpectedException() throws Exception {
		encoder.encodeUInteger(null);
	}

	/**
	 * Test of encodeLong method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeLong1() throws Exception {
		encoder.encodeLong(0L);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeLong1b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeLong(0L);
		assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeLong2() throws Exception {
		encoder.encodeLong(-9223372036854775808L); // mapped to 18446744073709551615
		assertArrayEquals(new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000001
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeLong2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeLong(-9223372036854775808L);
		assertArrayEquals(new byte[]{
			(byte) 0x80, 0, 0, 0, 0, 0, 0, 0
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeLong3() throws Exception {
		encoder.encodeLong(9223372036854775807L); // mapped to 18446744073709551614
		assertArrayEquals(new byte[]{
			(byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000001
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeLong3b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeLong(9223372036854775807L);
		assertArrayEquals(new byte[]{
			(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeLong4() throws Exception {
		encoder.encodeLong(-2147483648L); // mapped to 4294967295L
		assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeLong4b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeLong(-2147483648L);
		assertArrayEquals(new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0x80, 0, 0, 0
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeLong5() throws Exception {
		encoder.encodeLong(2147483647L); // mapped to 4294967294L
		assertArrayEquals(new byte[]{(byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeLong5b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeLong(2147483647L);
		assertArrayEquals(new byte[]{0, 0, 0, 0, (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableLong method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableLong1() throws Exception {
		encoder.encodeNullableLong(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableLong2() throws Exception {
		encoder.encodeNullableLong(null);
		encoder.encodeNullableLong(9223372036854775807L); // mapped to 18446744073709551614
		encoder.encodeNullableLong(-1L); // mapped to 1
		encoder.encodeNullableLong(133747110815L); //mapped to 267494221630
		assertArrayEquals(new byte[]{
			0,
			1, (byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000001,
			1, (byte) 0b00000001,
			1, (byte) 0b10111110, (byte) 0b11001110, (byte) 0b10010111,
			(byte) 0b10111111, (byte) 0b11100100, (byte) 0b00000111
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableLong2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeNullableLong(null);
		encoder.encodeNullableLong(9223372036854775807L);
		encoder.encodeNullableLong(-1L);
		encoder.encodeNullableLong(133747110815L);
		assertArrayEquals(new byte[]{
			0,
			1, (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			1, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			1, 0, 0, 0, (byte) 0x1F,
			(byte) 0x23, (byte) 0xF2, (byte) 0xF3, (byte) 0x9F
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeLong method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeLongExpectedException() throws Exception {
		encoder.encodeLong(null);
	}

	/**
	 * Test of encodeULong method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeULong1() throws Exception {
		encoder.encodeULong(new ULong(BigInteger.ZERO));
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeULong1b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeULong(new ULong(BigInteger.ZERO));
		assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeULong2() throws Exception {
		encoder.encodeULong(new ULong(BigInteger.ONE));
		assertArrayEquals(new byte[]{(byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeULong2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeULong(new ULong(BigInteger.ONE));
		assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeULong3() throws Exception {
		encoder.encodeULong(new ULong(new BigInteger("18446744073709551615")));
		assertArrayEquals(new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000001
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeULong3b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeULong(new ULong(new BigInteger("18446744073709551615")));
		assertArrayEquals(new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeULong4() throws Exception {
		encoder.encodeULong(new ULong(new BigInteger("9223372036854775807")));
		assertArrayEquals(new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b01111111
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeULong4b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeULong(new ULong(new BigInteger("9223372036854775807")));
		assertArrayEquals(new byte[]{
			(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeULong5() throws Exception {
		encoder.encodeULong(new ULong(new BigInteger("123456789")));
		assertArrayEquals(new byte[]{(byte) 0b10010101, (byte) 0b10011010, (byte) 0b11101111, (byte) 0b00111010}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeULong5b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeULong(new ULong(new BigInteger("123456789")));
		assertArrayEquals(new byte[]{0, 0, 0, 0, (byte) 0x07, (byte) 0x5B, (byte) 0xCD, (byte) 0x15}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableULong method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableULong1() throws Exception {
		encoder.encodeNullableULong(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableULong2() throws Exception {
		encoder.encodeNullableULong(null);
		encoder.encodeNullableULong(new ULong(new BigInteger("18446744073709551615")));
		encoder.encodeNullableULong(new ULong(BigInteger.ZERO));
		encoder.encodeNullableULong(new ULong(new BigInteger("133747110815")));
		assertArrayEquals(new byte[]{
			0,
			1, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000001,
			1, (byte) 0,
			1, (byte) 0b10011111, (byte) 0b11100111, (byte) 0b11001011,
			(byte) 0b10011111, (byte) 0b11110010, (byte) 0b00000011
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableULong2b() throws Exception {
		setVarintSupportedProperty(false);
		encoder.encodeNullableULong(null);
		encoder.encodeNullableULong(new ULong(new BigInteger("18446744073709551615")));
		encoder.encodeNullableULong(new ULong(BigInteger.ZERO));
		encoder.encodeNullableULong(new ULong(new BigInteger("133747110815")));
		assertArrayEquals(new byte[]{
			0,
			1, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			1, 0, 0, 0, 0, 0, 0, 0, 0,
			1, 0, 0, 0, (byte) 0x1F,
			(byte) 0x23, (byte) 0xF2, (byte) 0xF3, (byte) 0x9F
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeULong method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeULongExpectedException() throws Exception {
		encoder.encodeULong(null);
	}

	/**
	 * Test of encodeString method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeString1() throws Exception {
		encoder.encodeString("");
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeString2() throws Exception {
		encoder.encodeString("DLR");
		assertArrayEquals(new byte[]{(byte) 3, (byte) 0x44, (byte) 0x4C, (byte) 0x52
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeString3() throws Exception {
		encoder.encodeString("Deutsches Zentrum für Luft- und Raumfahrt");
		assertArrayEquals(new byte[]{
			(byte) 42,
			(byte) 0x44, (byte) 0x65, (byte) 0x75, (byte) 0x74, (byte) 0x73, (byte) 0x63, (byte) 0x68, (byte) 0x65,
			(byte) 0x73, (byte) 0x20, (byte) 0x5A, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x72, (byte) 0x75,
			(byte) 0x6D, (byte) 0x20, (byte) 0x66, (byte) 0xC3, (byte) 0xBC, (byte) 0x72, (byte) 0x20, (byte) 0x4c,
			(byte) 0x75, (byte) 0x66, (byte) 0x74, (byte) 0x2D, (byte) 0x20, (byte) 0x75, (byte) 0x6E, (byte) 0x64,
			(byte) 0x20, (byte) 0x52, (byte) 0x61, (byte) 0x75, (byte) 0x6D, (byte) 0x66, (byte) 0x61, (byte) 0x68,
			(byte) 0x72, (byte) 0x74
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeString4() throws Exception {
		encoder.encodeString("\ud83d\udce1\ud83d\udc6c");
		assertArrayEquals(new byte[]{
			(byte) 8,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x93, (byte) 0xA1,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x91, (byte) 0xAC
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableString method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableString1() throws Exception {
		encoder.encodeNullableString(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableString2() throws Exception {
		encoder.encodeNullableString("\ud83d\ude80 DLR");
		encoder.encodeNullableString(null);
		encoder.encodeNullableString("");
		encoder.encodeNullableString("\u5b87\u5b99");
		assertArrayEquals(new byte[]{
			(byte) 1, (byte) 8, (byte) 0xF0, (byte) 0x9F, (byte) 0x9A, (byte) 0x80, (byte) 0x20, (byte) 0x44, (byte) 0x4C, (byte) 0x52,
			(byte) 0,
			(byte) 1, (byte) 0,
			(byte) 1, (byte) 6, (byte) 0xE5, (byte) 0xAE, (byte) 0x87, (byte) 0xE5, (byte) 0xAE, (byte) 0x99
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeString method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeStringExpectedException() throws Exception {
		encoder.encodeString(null);
	}

	/**
	 * Test of encodeBlob method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeBlob1() throws Exception {
		encoder.encodeBlob(new Blob(new byte[]{}));
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeBlob2() throws Exception {
		encoder.encodeBlob(new Blob(new byte[]{0}));
		assertArrayEquals(new byte[]{1, 0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeBlob3() throws Exception {
		encoder.encodeBlob(new Blob(new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0xFF, (byte) 0xAB}));
		assertArrayEquals(new byte[]{4, (byte) 0x01, (byte) 0x02, (byte) 0xFF, (byte) 0xAB}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeBlob4() throws Exception {
		final int randomArrLength = 65537;
		byte[] randomArr = new byte[randomArrLength + 3];
		new Random().nextBytes(randomArr);
		randomArr[0] = (byte) 0b10000001;
		randomArr[1] = (byte) 0b10000000;
		randomArr[2] = (byte) 0b00000100;
		encoder.encodeBlob(new Blob(Arrays.copyOfRange(randomArr, 3, randomArrLength + 3)));
		assertArrayEquals(randomArr, outputStream.toByteArray());
	}

	@Test
	public void testEncodeBlob5() throws Exception {
		encoder.encodeBlob(new Blob());
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableBlob method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableBlob1() throws Exception {
		encoder.encodeNullableBlob(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableBlob2() throws Exception {
		encoder.encodeNullableBlob(new Blob(new byte[]{(byte) 0x44, (byte) 0x4C, (byte) 0x52}));
		encoder.encodeNullableBlob(null);
		encoder.encodeNullableBlob(new Blob(new byte[]{}));
		encoder.encodeNullableBlob(new Blob(new byte[]{(byte) 0x21}));
		assertArrayEquals(new byte[]{
			(byte) 1, (byte) 3, (byte) 0x44, (byte) 0x4C, (byte) 0x52,
			(byte) 0,
			(byte) 1, (byte) 0,
			(byte) 1, (byte) 1, (byte) 0x21
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeBlob method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeBlobExpectedException() throws Exception {
		encoder.encodeBlob(null);
	}

	/**
	 * Test of encodeDuration method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeDuration1() throws Exception {
		setTimeProperties("DURATION", "00100000", "9999-01-23T21:43:56", null);
		encoder.encodeDuration(new Duration(-1));
		assertArrayEquals(new byte[]{
			(byte) 0b11111111
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeDuration2() throws Exception {
		setTimeProperties("DURATION", "00100000", "9999-01-23T21:43:56", null);
		encoder.encodeDuration(new Duration(1));
		assertArrayEquals(new byte[]{
			(byte) 0b00000001
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeDuration3() throws Exception {
		setTimeProperties("DURATION", "00100000", "9999-01-23T21:43:56", null);
		encoder.encodeDuration(new Duration(0));
		assertArrayEquals(new byte[]{
			0
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeDuration4() throws Exception {
		setTimeProperties("DURATION", "00101000", "9999-01-23T21:43:56", null);
		encoder.encodeDuration(new Duration(-987654));
		assertArrayEquals(new byte[]{
			(byte) 0xF0, (byte) 0xED, (byte) 0xFA
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeDuration5() throws Exception {
		setTimeProperties("DURATION", "00101100", "9999-01-23T21:43:56", null);
		encoder.encodeDuration(new Duration(2147483647));
		assertArrayEquals(new byte[]{
			(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableDuration method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableDuration() throws Exception {
		setTimeProperties("DURATION", "00100000", "9999-01-23T21:43:56", null);
		encoder.encodeNullableDuration(null);
		encoder.encodeNullableDuration(new Duration(-1));
		assertArrayEquals(new byte[]{
			0, 1, (byte) 0b11111111
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeDuration method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeDurationExpectedException() throws Exception {
		encoder.encodeDuration(null);
	}

	/**
	 * Test of encodeFineTime method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeFineTimeCUC1() throws Exception {
		setTimeProperties("FINE_TIME", "1010001100001000", Configuration.MAL_FINE_TIME_EPOCH, Configuration.MAL_FINE_TIME_EPOCH_TIMESCALE);
		encoder.encodeFineTime(new FineTime(1)); // MAL_FINE_TIME_EPOCH + 0.000000000001s
		assertArrayEquals(new byte[]{
			0, 0, 0, 0, 0, 1
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFineTimeCUC2() throws Exception {
		setTimeProperties("FINE_TIME", "1010101100001000", Configuration.MAL_FINE_TIME_EPOCH, Configuration.MAL_FINE_TIME_EPOCH_TIMESCALE);
		encoder.encodeFineTime(new FineTime(4451696123456789012L)); // 2013-02-21T12:34:56.123456789012 = MAL_FINE_TIME_EPOCH (2013-01-01T00:00:00) + ...
		assertArrayEquals(new byte[]{ // 4451696, 135742175047
			(byte) 0x43, (byte) 0xED, (byte) 0x70, (byte) 0x1F, (byte) 0x9A, (byte) 0xDD, (byte) 0x37, (byte) 0x47
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFineTimeCUC3() throws Exception {
		setTimeProperties("FINE_TIME", "1010111100001000", Configuration.MAL_FINE_TIME_EPOCH, Configuration.MAL_FINE_TIME_EPOCH_TIMESCALE);
		encoder.encodeFineTime(new FineTime(4451696987654321098L)); // 2013-02-21T12:34:56.987654321098 = MAL_FINE_TIME_EPOCH (2013-01-01T00:00:00) + ...
		assertArrayEquals(new byte[]{ // 4451696, 1085937410270
			0, (byte) 0x43, (byte) 0xED, (byte) 0x70, (byte) 0xFC, (byte) 0xD6, (byte) 0xE9, (byte) 0xE0, (byte) 0xDE
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFineTimeCDS1() throws Exception {
		setTimeProperties("FINE_TIME", "01001010", Configuration.MAL_FINE_TIME_EPOCH, "UTC");
		encoder.encodeFineTime(new FineTime(0 + 35000000000000L)); // MAL_FINE_TIME_EPOCH
		assertArrayEquals(new byte[]{ // 0, 0.0
			(byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFineTimeCDS2() throws Exception {
		setTimeProperties("FINE_TIME", "01001010", Configuration.MAL_FINE_TIME_EPOCH, "UTC");
		encoder.encodeFineTime(new FineTime(4451696123456789012L + 35000000000000L)); // 2013-02-21T12:34:56.123456789012 = MAL_FINE_TIME_EPOCH (2013-01-01T00:00:00) + ...
		assertArrayEquals(new byte[]{ // 51, 45296.123, 456789012
			(byte) 0, (byte) 51, (byte) 0x02, (byte) 0xB3, (byte) 0x29, (byte) 0xFB, (byte) 0x1B, (byte) 0x3A, (byte) 0x0C, (byte) 0x14
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFineTimeCDS3() throws Exception {
		setTimeProperties("FINE_TIME", "01001010", Configuration.MAL_FINE_TIME_EPOCH, "UTC");
		encoder.encodeFineTime(new FineTime(4451696987654321098L + 35000000000000L)); // 2013-02-21T12:34:56.987654321098 = MAL_FINE_TIME_EPOCH (2013-01-01T00:00:00) + ...
		assertArrayEquals(new byte[]{ // 51, 45296.987, 654321098
			(byte) 0, (byte) 51, (byte) 0x02, (byte) 0xB3, (byte) 0x2D, (byte) 0x5B, (byte) 0x27, (byte) 0x00, (byte) 0x25, (byte) 0xCA
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFineTimeCCS1() throws Exception {
		setTimeProperties("FINE_TIME", "01010110", "0000-01-23T21:43:56", "UTC");
		encoder.encodeFineTime(new FineTime(4451696987654321098L + 35000000000000L)); // 2013-02-21T12:34:56.987654321098 = MAL_FINE_TIME_EPOCH (2013-01-01T00:00:00) + ...
		assertArrayEquals(new byte[]{
			(byte) 0x07, (byte) 0xDD, 2, 21, 12, 34, 56, 98, 76, 54, 32, 10, 98
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeFineTimeCCS2() throws Exception {
		setTimeProperties("FINE_TIME", "01011110", "0000-01-23T21:43:56", "UTC");
		encoder.encodeFineTime(new FineTime(4451696123456789012L + 35000000000000L)); // 2013-02-21T12:34:56.123456789012 = MAL_FINE_TIME_EPOCH (2013-01-01T00:00:00) + ...
		assertArrayEquals(new byte[]{ // DOY 52
			(byte) 0x07, (byte) 0xDD, (byte) 0x00, (byte) 52, 12, 34, 56, 12, 34, 56, 78, 90, 12
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableFineTime method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableFineTime() throws Exception {
		setTimeProperties("FINE_TIME", "01001010", Configuration.MAL_FINE_TIME_EPOCH, "UTC");
		encoder.encodeNullableFineTime(new FineTime(0 + 35000000000000L)); // MAL_FINE_TIME_EPOCH
		encoder.encodeNullableFineTime(null);
		assertArrayEquals(new byte[]{ // 0, 0.0
			1, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, 0
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeFineTime method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeFineTimeExpectedException() throws Exception {
		encoder.encodeFineTime(null);
	}

	/**
	 * Test of encodeIdentifier method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeIdentifier1() throws Exception {
		encoder.encodeIdentifier(new Identifier(""));
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeIdentifier2() throws Exception {
		encoder.encodeIdentifier(new Identifier("DLR"));
		assertArrayEquals(new byte[]{(byte) 3, (byte) 0x44, (byte) 0x4C, (byte) 0x52
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeIdentifier3() throws Exception {
		encoder.encodeIdentifier(new Identifier("Deutsches Zentrum für Luft- und Raumfahrt"));
		assertArrayEquals(new byte[]{
			(byte) 42,
			(byte) 0x44, (byte) 0x65, (byte) 0x75, (byte) 0x74, (byte) 0x73, (byte) 0x63, (byte) 0x68, (byte) 0x65,
			(byte) 0x73, (byte) 0x20, (byte) 0x5A, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x72, (byte) 0x75,
			(byte) 0x6D, (byte) 0x20, (byte) 0x66, (byte) 0xC3, (byte) 0xBC, (byte) 0x72, (byte) 0x20, (byte) 0x4c,
			(byte) 0x75, (byte) 0x66, (byte) 0x74, (byte) 0x2D, (byte) 0x20, (byte) 0x75, (byte) 0x6E, (byte) 0x64,
			(byte) 0x20, (byte) 0x52, (byte) 0x61, (byte) 0x75, (byte) 0x6D, (byte) 0x66, (byte) 0x61, (byte) 0x68,
			(byte) 0x72, (byte) 0x74
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeIdentifier4() throws Exception {
		encoder.encodeIdentifier(new Identifier("\ud83d\udce1\ud83d\udc6c"));
		assertArrayEquals(new byte[]{
			(byte) 8,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x93, (byte) 0xA1,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x91, (byte) 0xAC
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableIdentifier method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableIdentifier1() throws Exception {
		encoder.encodeNullableIdentifier(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableIdentifier2() throws Exception {
		encoder.encodeNullableIdentifier(new Identifier("\ud83d\ude80 DLR"));
		encoder.encodeNullableIdentifier(new Identifier(""));
		encoder.encodeNullableIdentifier(new Identifier("\u5b87\u5b99"));
		encoder.encodeNullableIdentifier(null);
		assertArrayEquals(new byte[]{
			(byte) 1, (byte) 8, (byte) 0xF0, (byte) 0x9F, (byte) 0x9A, (byte) 0x80, (byte) 0x20, (byte) 0x44, (byte) 0x4C, (byte) 0x52,
			(byte) 1, (byte) 0,
			(byte) 1, (byte) 6, (byte) 0xE5, (byte) 0xAE, (byte) 0x87, (byte) 0xE5, (byte) 0xAE, (byte) 0x99,
			(byte) 0
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeIdentifier method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeIdentifierExpectedException() throws Exception {
		encoder.encodeIdentifier(null);
	}

	/**
	 * Test of encodeTime method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeTimeCUC1() throws Exception {
		setTimeProperties("TIME", "00100000", "1970-01-01T00:00:00", Configuration.JAVA_EPOCH_TIMESCALE);
		encoder.encodeTime(new Time(0)); // 1970-01-01T00:00:00
		assertArrayEquals(new byte[]{ // 0.000
			(byte) 0b00000000
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCUC2() throws Exception {
		setTimeProperties("TIME", "00011100", "1958-01-01T00:00:00", "TAI");
		encoder.encodeTime(new Time(0)); // 1970-01-01T00:00:00.000
		assertArrayEquals(new byte[]{ // 378691208.000
			(byte) 0b00010110, (byte) 0b10010010, (byte) 0b01011110, (byte) 0b10001000
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCUC3() throws Exception {
		setTimeProperties("TIME", "00100010", "1970-01-01T00:00:00", Configuration.JAVA_EPOCH_TIMESCALE);
		encoder.encodeTime(new Time(1)); // 1970-01-01T00:00:00.001
		assertArrayEquals(new byte[]{ // 0.001
			(byte) 0b00000000, (byte) 0b00000000, (byte) 0b01000010
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCUC4() throws Exception {
		setTimeProperties("TIME", "00011101", "1958-01-01T00:00:00", "TAI");
		encoder.encodeTime(new Time(1377088523004L)); // 2013-08-21T12:34:56.004
		assertArrayEquals(new byte[]{
			(byte) 0b01101000, (byte) 0b10100111, (byte) 0b00010010, (byte) 0b10010011, (byte) 0b00000001
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCUC5() throws Exception {
		setTimeProperties("TIME", "1010111000100000", "1600-01-01T00:00:00", "TAI");
		encoder.encodeTime(new Time(1377088523123L)); // 2013-08-21T12:34:56.123
		assertArrayEquals(new byte[]{ // 13053184531.123
			(byte) 0b00000011, (byte) 0b0001010, (byte) 0b00000111, (byte) 0b11001010, (byte) 0b00010011, (byte) 0b00011111, (byte) 0b01111101
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCUC6() throws Exception {
		setTimeProperties("TIME", "00011111", "1958-01-01T00:00:00", "TAI");
		encoder.encodeTime(new Time(1421687121689L));
		assertArrayEquals(new byte[]{
			(byte) 0b001101011, (byte) 0b01001111, (byte) 0b10010111, (byte) 0b11011001, (byte) 0b10110000, (byte) 0b01100010, (byte) 0b01001110
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCUC7() throws Exception {
		setTimeProperties("TIME", "00011111", "1958-01-01T00:00:00", "TAI");
		encoder.encodeTime(new Time(1421687121690L));
		assertArrayEquals(new byte[]{
			(byte) 0b001101011, (byte) 0b01001111, (byte) 0b10010111, (byte) 0b11011001, (byte) 0b10110000, (byte) 0b10100011, (byte) 0b11010111
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCDS1() throws Exception {
		setTimeProperties("TIME", "01001000", "1970-01-01T00:00:00", Configuration.JAVA_EPOCH_TIMESCALE);
		encoder.encodeTime(new Time(0)); // 1970-01-01T00:00:00
		assertArrayEquals(new byte[]{ // 0, 0.0
			(byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCDS2() throws Exception {
		setTimeProperties("TIME", "01000000", "1958-01-01T00:00:00", "TAI");
		encoder.encodeTime(new Time(1377088523123L)); // 2013-08-21T12:34:56.123
		assertArrayEquals(new byte[]{ // 20321, 45296.123
			(byte) 0b01001111, (byte) 0b01100001, (byte) 0b00000010, (byte) 0b10110011, (byte) 0b00101001, (byte) 0b11111011
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCDS3() throws Exception {
		setTimeProperties("TIME", "01001100", "1600-01-01T00:00:00", "TAI");
		encoder.encodeTime(new Time(1377088523004L)); // 2013-08-21T12:34:56.004
		assertArrayEquals(new byte[]{ // 151078, 45296.004
			(byte) 0b00000010, (byte) 0b01001110, (byte) 0b00100110, (byte) 0b00000010, (byte) 0b10110011, (byte) 0b00101001, (byte) 0b10000100
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCCS1() throws Exception {
		setTimeProperties("TIME", "01010000", "0000-01-23T21:43:56", null);
		encoder.encodeTime(new Time(0)); // 1970-01-01T00:00:00
		assertArrayEquals(new byte[]{
			(byte) 0b00000111, (byte) 0b10110010, 1, 1, 0, 0, 0
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCCS2() throws Exception {
		setTimeProperties("TIME", "01010010", "0000-01-23T21:43:56", null);
		encoder.encodeTime(new Time(1377088523123L)); // 2013-08-21T12:34:56.1230
		assertArrayEquals(new byte[]{
			(byte) 0x07, (byte) 0xDD, 8, 21, 12, 34, 56, 12, 30
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCCS3() throws Exception {
		setTimeProperties("TIME", "01011001", "0000-01-23T21:43:56", null);
		encoder.encodeTime(new Time(-10)); // 1969-12-31T23:59:59.990
		assertArrayEquals(new byte[]{ // DOY 365
			(byte) 0x07, (byte) 0xB1, (byte) 0x01, (byte) 0x6D, 23, 59, 59, 99
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeTimeCCS4() throws Exception {
		setTimeProperties("TIME", "01011010", "0000-01-23T21:43:56", null);
		encoder.encodeTime(new Time(1377088523004L)); // 2013-08-21T12:34:56.0040
		assertArrayEquals(new byte[]{ // DOY 233
			(byte) 0x07, (byte) 0xDD, (byte) 0x00, (byte) 0xE9, 12, 34, 56, 00, 40
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableTime method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableTime() throws Exception {
		setTimeProperties("TIME", "01010000", "0000-01-23T21:43:56", null);
		encoder.encodeNullableTime(null);
		encoder.encodeNullableTime(new Time(0)); // 1970-01-01T00:00:00
		assertArrayEquals(new byte[]{
			0, 1, (byte) 0b00000111, (byte) 0b10110010, 1, 1, 0, 0, 0
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeTime method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeTimeExpectedException() throws Exception {
		encoder.encodeTime(null);
	}

	/**
	 * Test of encodeURI method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeURI1() throws Exception {
		encoder.encodeURI(new URI(""));
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeURI2() throws Exception {
		encoder.encodeURI(new URI("http://www.dlr.de"));
		assertArrayEquals(new byte[]{
			(byte) 17,
			(byte) 0x68, (byte) 0x74, (byte) 0x74, (byte) 0x70, (byte) 0x3A, (byte) 0x2F, (byte) 0x2F, (byte) 0x77,
			(byte) 0x77, (byte) 0x77, (byte) 0x2E, (byte) 0x64, (byte) 0x6C, (byte) 0x72, (byte) 0x2E, (byte) 0x64,
			(byte) 0x65
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeURI3() throws Exception {
		encoder.encodeURI(new URI("Deutsches Zentrum für Luft- und Raumfahrt"));
		assertArrayEquals(new byte[]{
			(byte) 42,
			(byte) 0x44, (byte) 0x65, (byte) 0x75, (byte) 0x74, (byte) 0x73, (byte) 0x63, (byte) 0x68, (byte) 0x65,
			(byte) 0x73, (byte) 0x20, (byte) 0x5A, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x72, (byte) 0x75,
			(byte) 0x6D, (byte) 0x20, (byte) 0x66, (byte) 0xC3, (byte) 0xBC, (byte) 0x72, (byte) 0x20, (byte) 0x4c,
			(byte) 0x75, (byte) 0x66, (byte) 0x74, (byte) 0x2D, (byte) 0x20, (byte) 0x75, (byte) 0x6E, (byte) 0x64,
			(byte) 0x20, (byte) 0x52, (byte) 0x61, (byte) 0x75, (byte) 0x6D, (byte) 0x66, (byte) 0x61, (byte) 0x68,
			(byte) 0x72, (byte) 0x74
		}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeURI4() throws Exception {
		encoder.encodeURI(new URI("\ud83d\udce1\ud83d\udc6c"));
		assertArrayEquals(new byte[]{
			(byte) 8,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x93, (byte) 0xA1,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x91, (byte) 0xAC
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableURI method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableURI1() throws Exception {
		encoder.encodeNullableURI(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableURI2() throws Exception {
		encoder.encodeNullableURI(null);
		encoder.encodeNullableURI(new URI("\ud83d\ude80 DLR"));
		encoder.encodeNullableURI(new URI(""));
		encoder.encodeNullableURI(new URI("\u5b87\u5b99"));
		assertArrayEquals(new byte[]{
			(byte) 0,
			(byte) 1, (byte) 8, (byte) 0xF0, (byte) 0x9F, (byte) 0x9A, (byte) 0x80, (byte) 0x20, (byte) 0x44, (byte) 0x4C, (byte) 0x52,
			(byte) 1, (byte) 0,
			(byte) 1, (byte) 6, (byte) 0xE5, (byte) 0xAE, (byte) 0x87, (byte) 0xE5, (byte) 0xAE, (byte) 0x99
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeURI method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeURIExpectedException() throws Exception {
		encoder.encodeURI(null);
	}

	/**
	 * Test of encodeElement method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeElement1() throws Exception {
		encoder.encodeElement(new URI(""));
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeElement2() throws Exception {
		encoder.encodeElement(new UShort(128));
		assertArrayEquals(new byte[]{(byte) 0b10000000, (byte) 0b00000001}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeElement3() throws Exception {
		encoder.encodeElement(new Union(new Byte((byte) -128)));
		assertArrayEquals(new byte[]{(byte) 0b10000000}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableElement method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableElement1() throws Exception {
		encoder.encodeNullableElement(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableElement2() throws Exception {
		encoder.encodeNullableElement(new Blob(new byte[]{}));
		encoder.encodeNullableElement(null);
		encoder.encodeNullableElement(null);
		encoder.encodeNullableElement(new Union(new Integer(0)));
		assertArrayEquals(new byte[]{
			(byte) 1, (byte) 0,
			(byte) 0,
			(byte) 0,
			(byte) 1, (byte) 0
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeElement method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeElementExpectedException() throws Exception {
		encoder.encodeElement(null);
	}

	/**
	 * Test of encodeAttribute method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeAttribute1() throws Exception {
		encoder.encodeAttribute(new UInteger(42));
		assertArrayEquals(new byte[]{11, 42}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeAttribute2() throws Exception {
		encoder.encodeAttribute(new Union(new Integer(42))); // mapped to 84
		assertArrayEquals(new byte[]{10, 84}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeNullableAttribute method, of class SPPEncoder.
	 */
	@Test
	public void testEncodeNullableAttribute1() throws Exception {
		encoder.encodeNullableAttribute(null);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
	}

	@Test
	public void testEncodeNullableAttribute2() throws Exception {
		encoder.encodeNullableAttribute(new URI("DLR"));
		encoder.encodeNullableAttribute(new Union(Boolean.TRUE));
		encoder.encodeNullableAttribute(null);
		encoder.encodeNullableAttribute(new Blob(new byte[]{0x42, 0x0}));
		assertArrayEquals(new byte[]{
			(byte) 1, (byte) 17, (byte) 3, (byte) 0x44, (byte) 0x4C, (byte) 0x52,
			(byte) 1, (byte) 1, (byte) 1,
			(byte) 0,
			(byte) 1, (byte) 0, (byte) 2, (byte) 0x42, (byte) 0x0
		}, outputStream.toByteArray());
	}

	/**
	 * Test of encodeAttribute method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncodeAttributeExpectedException() throws Exception {
		encoder.encodeAttribute(null);
	}

	/**
	 * Test of createListEncoder method, of class SPPEncoder.
	 */
	@Test
	public void testCreateListEncoder1() throws Exception {
		List<Element> list = new ArrayList<>();
		MALListEncoder listEncoder = encoder.createListEncoder(list);
		assertArrayEquals(new byte[]{0}, outputStream.toByteArray());
		assertEquals(encoder, listEncoder);
	}

	@Test
	public void testCreateListEncoder2() throws Exception {
		List<Element> list = new ArrayList<>();
		list.add(new Union(new Integer(42)));
		list.add(new URI("http://www.dlr.de"));
		MALListEncoder listEncoder = encoder.createListEncoder(list);
		assertArrayEquals(new byte[]{2}, outputStream.toByteArray());
		assertEquals(encoder, listEncoder);
	}

	/**
	 * Test of createListEncoder method, of class SPPEncoder.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateListEncoderExpectedException() throws Exception {
		MALListEncoder listEncoder = encoder.createListEncoder(null);
	}

	private void setTimeProperties(String timeFormat, String pField, String epoch, String timescale) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("de.dlr.gsoc.mo.malspp." + timeFormat + "_CODE_FORMAT", pField);
		properties.put("de.dlr.gsoc.mo.malspp." + timeFormat + "_EPOCH", epoch);
		if (null != timescale) {
			properties.put("de.dlr.gsoc.mo.malspp." + timeFormat + "_EPOCH_TIMESCALE", timescale);
		}
		encoder.setProperties(properties);
	}

	private void setVarintSupportedProperty(Boolean varintSupported) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("de.dlr.gsoc.mo.malspp.VARINT_SUPPORTED", varintSupported.toString());
		encoder.setProperties(properties);
	}
}
