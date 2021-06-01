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
import de.dlr.gsoc.mo.malspp.encoding.SPPDecoder;
import de.dlr.gsoc.mo.malspp.encoding.Configuration;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListDecoder;
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

public class SPPDecoderTest {

	private SPPDecoder decoder;

	@Before
	public void setUp() {
		decoder = null;
	}

	/**
	 * Test of decodeBoolean method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeBoolean1() throws Exception {
		newBuffer(new byte[]{0});
		assertEquals(Boolean.FALSE, decoder.decodeBoolean());
	}

	@Test
	public void testDecodeBoolean2() throws Exception {
		newBuffer(new byte[]{1});
		assertEquals(Boolean.TRUE, decoder.decodeBoolean());
	}

	@Test
	public void testDecodeBoolean3() throws Exception {
		newBuffer(new byte[]{0, 1, 0});
		assertEquals(Boolean.FALSE, decoder.decodeBoolean());
		assertEquals(Boolean.TRUE, decoder.decodeBoolean());
		assertEquals(Boolean.FALSE, decoder.decodeBoolean());
	}

	/**
	 * Test of decodeNullableBoolean method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableBoolean1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableBoolean());
	}

	@Test
	public void testDecodeNullableBoolean2() throws Exception {

		newBuffer(new byte[]{
			1, 0,
			0,
			1, 1,
			1, 0
		});
		assertEquals(Boolean.FALSE, decoder.decodeNullableBoolean());
		assertNull(decoder.decodeNullableBoolean());
		assertEquals(Boolean.TRUE, decoder.decodeNullableBoolean());
		assertEquals(Boolean.FALSE, decoder.decodeNullableBoolean());
	}

	/**
	 * Test of decodeBoolean method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeBooleanExpectedException1() throws Exception {
		newBuffer(new byte[]{2});
		try {
			decoder.decodeBoolean();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	@Test
	public void testDecodeBooleanExpectedException2() throws Exception {
		newBuffer(new byte[]{2});
		try {
			decoder.decodeNullableBoolean();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	@Test
	public void testDecodeBooleanExpectedException3() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeBoolean();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	/**
	 * Test of decodeFloat method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeFloat1() throws Exception {
		newBuffer(new byte[]{(byte) 0b01111111, (byte) 0b11000000, 0, 0});
		assertEquals(Float.valueOf(Float.NaN), decoder.decodeFloat());
	}

	@Test
	public void testDecodeFloat2() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111111, (byte) 0b10000000, 0, 0});
		assertEquals(Float.valueOf(Float.NEGATIVE_INFINITY), decoder.decodeFloat());
	}

	@Test
	public void testDecodeFloat3() throws Exception {
		newBuffer(new byte[]{(byte) 0b01111111, (byte) 0b10000000, 0, 0});
		assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), decoder.decodeFloat());
	}

	@Test
	public void testDecodeFloat4() throws Exception {
		newBuffer(new byte[]{0, 0, 0, 0});
		assertEquals(Float.valueOf((float) +0.0), decoder.decodeFloat());
	}

	@Test
	public void testDecodeFloat5() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, 0, 0, 0});
		assertEquals(Float.valueOf((float) -0.0), decoder.decodeFloat());
	}

	@Test
	public void testDecodeFloat6() throws Exception {
		newBuffer(new byte[]{0, 0, 0, (byte) 0b00000001});
		assertEquals(Float.valueOf(Float.MIN_VALUE), decoder.decodeFloat());
	}

	@Test
	public void testDecodeFloat7() throws Exception {
		newBuffer(new byte[]{(byte) 0b01111111, (byte) 0b01111111, (byte) 0b11111111, (byte) 0b11111111});
		assertEquals(Float.valueOf(Float.MAX_VALUE), decoder.decodeFloat());
	}

	/**
	 * Test of decodeNullableFloat method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableFloat1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableFloat());
	}

	@Test
	public void testDecodeNullableFloat2() throws Exception {
		newBuffer(new byte[]{
			1, (byte) 0b10000000, 0, 0, 0,
			0,
			1, (byte) 0b01111111, (byte) 0b11000000, 0, 0,
			1, 0, (byte) 0b10000000, 0, 0
		});
		assertEquals(Float.valueOf((float) -0.0), decoder.decodeNullableFloat());
		assertNull(decoder.decodeNullableFloat());
		assertEquals(Float.valueOf(Float.NaN), decoder.decodeNullableFloat());
		assertEquals(Float.valueOf(Float.MIN_NORMAL), decoder.decodeNullableFloat());
	}

	/**
	 * Test of decodeFloat method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeFloatExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeFloat();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeFloatExpectedException2() throws Exception {
		newBuffer(new byte[]{3, 2, 1});
		try {
			decoder.decodeFloat();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	/**
	 * Test of decodeDouble method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeDouble1() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b01111111, (byte) 0b11111000, 0, 0,
			0, 0, 0, 0
		});
		assertEquals(Double.valueOf(Double.NaN), decoder.decodeDouble());
	}

	@Test
	public void testDecodeDouble2() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b11111111, (byte) 0b11110000, 0, 0,
			0, 0, 0, 0
		});
		assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), decoder.decodeDouble());
	}

	@Test
	public void testDecodeDouble3() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b01111111, (byte) 0b11110000, 0, 0,
			0, 0, 0, 0
		});
		assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), decoder.decodeDouble());
	}

	@Test
	public void testDecodeDouble4() throws Exception {
		newBuffer(new byte[]{
			0, 0, 0, 0,
			0, 0, 0, 0
		});
		assertEquals(Double.valueOf(+0.0), decoder.decodeDouble());
	}

	@Test
	public void testDecodeDouble5() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b10000000, 0, 0, 0,
			0, 0, 0, 0
		});
		assertEquals(Double.valueOf(-0.0), decoder.decodeDouble());
	}

	@Test
	public void testDecodeDouble6() throws Exception {
		newBuffer(new byte[]{
			0, 0, 0, 0,
			0, 0, 0, (byte) 0b00000001
		});
		assertEquals(Double.valueOf(Double.MIN_VALUE), decoder.decodeDouble());
	}

	@Test
	public void testDecodeDouble7() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b01111111, (byte) 0b11101111, (byte) 0b11111111, (byte) 0b11111111,
			(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111
		});
		assertEquals(Double.valueOf(Double.MAX_VALUE), decoder.decodeDouble());
	}

	/**
	 * Test of decodeNullableDouble method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableDouble1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableDouble());
	}

	@Test
	public void testDecodeNullableDouble2() throws Exception {
		newBuffer(new byte[]{
			0,
			1, (byte) 0b10000000, 0, 0, 0, 0, 0, 0, 0,
			1, (byte) 0b01111111, (byte) 0b11111000, 0, 0, 0, 0, 0, 0,
			1, 0, (byte) 0b00010000, 0, 0, 0, 0, 0, 0
		});
		assertNull(decoder.decodeNullableDouble());
		assertEquals(Double.valueOf(-0.0), decoder.decodeNullableDouble());
		assertEquals(Double.valueOf(Double.NaN), decoder.decodeNullableDouble());
		assertEquals(Double.valueOf(Double.MIN_NORMAL), decoder.decodeNullableDouble());
	}

	/**
	 * Test of decodeDouble method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeDoubleExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeDouble();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeDoubleExpectedException2() throws Exception {
		newBuffer(new byte[]{1, 2, 3, 4, 5, 6, 7});
		try {
			decoder.decodeDouble();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	/**
	 * Test of decodeOctet method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeOctet1() throws Exception {
		newBuffer(new byte[]{(byte) 0b01111111});
		assertEquals(Byte.valueOf((byte) 127), decoder.decodeOctet());
	}

	@Test
	public void testDecodeOctet2() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000});
		assertEquals(Byte.valueOf((byte) -128), decoder.decodeOctet());
	}

	@Test
	public void testDecodeOctet3() throws Exception {
		newBuffer(new byte[]{(byte) 0b00000001});
		assertEquals(Byte.valueOf((byte) 1), decoder.decodeOctet());
	}

	@Test
	public void testDecodeOctet4() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111111});
		assertEquals(Byte.valueOf((byte) -1), decoder.decodeOctet());
	}

	@Test
	public void testDecodeOctet5() throws Exception {
		newBuffer(new byte[]{(byte) 0b00000000});
		assertEquals(Byte.valueOf((byte) 0), decoder.decodeOctet());
	}

	@Test
	public void testDecodeOctet6() throws Exception {
		newBuffer(new byte[]{(byte) 0b11010110, (byte) 0b00101111});
		assertEquals(Byte.valueOf((byte) -42), decoder.decodeOctet());
		assertEquals(Byte.valueOf((byte) 47), decoder.decodeOctet());
	}

	/**
	 * Test of decodeNullableOctet method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableOctet1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableOctet());
	}

	@Test
	public void testDecodeNullableOctet2() throws Exception {
		newBuffer(new byte[]{
			1, (byte) 0b01111111,
			0,
			1, (byte) 0b00000000,
			1, (byte) 0b11010001
		});
		assertEquals(Byte.valueOf((byte) 127), decoder.decodeNullableOctet());
		assertNull(decoder.decodeNullableOctet());
		assertEquals(Byte.valueOf((byte) 0), decoder.decodeNullableOctet());
		assertEquals(Byte.valueOf((byte) -47), decoder.decodeNullableOctet());
	}

	/**
	 * Test of decodeOctet method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeOctetExpectedException() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeOctet();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	/**
	 * Test of decodeUOctet method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeUOctet1() throws Exception {
		newBuffer(new byte[]{(byte) 0b00000000});
		assertEquals(new UOctet((short) 0), decoder.decodeUOctet());
	}

	@Test
	public void testDecodeUOctet2() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111111});
		assertEquals(new UOctet((short) 255), decoder.decodeUOctet());
	}

	@Test
	public void testDecodeUOctet3() throws Exception {
		newBuffer(new byte[]{(byte) 0b00000001});
		assertEquals(new UOctet((short) 1), decoder.decodeUOctet());
	}

	@Test
	public void testDecodeUOctet4() throws Exception {
		newBuffer(new byte[]{(byte) 0b00101010});
		assertEquals(new UOctet((short) 42), decoder.decodeUOctet());
	}

	@Test
	public void testDecodeUOctet5() throws Exception {
		newBuffer(new byte[]{(byte) 0b11001111});
		assertEquals(new UOctet((short) 207), decoder.decodeUOctet());
	}

	@Test
	public void testDecodeUOctet6() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b00001110});
		assertEquals(new UOctet((short) 128), decoder.decodeUOctet());
		assertEquals(new UOctet((short) 14), decoder.decodeUOctet());
	}

	/**
	 * Test of decodeNullableUOctet method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableUOctet1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableUOctet());
	}

	@Test
	public void testDecodeNullableUOctet2() throws Exception {
		newBuffer(new byte[]{
			1, (byte) 0b11111110,
			1, (byte) 0b00000000,
			0,
			1, (byte) 0b00000001
		});
		assertEquals(new UOctet((short) 254), decoder.decodeNullableUOctet());
		assertEquals(new UOctet((short) 0), decoder.decodeNullableUOctet());
		assertNull(decoder.decodeNullableUOctet());
		assertEquals(new UOctet((short) 1), decoder.decodeNullableUOctet());
	}

	/**
	 * Test of decodeUOctet method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeUOctetExpectedException() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeUOctet();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	/**
	 * Test of decodeShort method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeShort1() throws Exception {
		newBuffer(new byte[]{(byte) 0});
		assertEquals(Short.valueOf((short) 0), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort1b() throws Exception {
		newBuffer(new byte[]{0, 0});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) 0), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort2() throws Exception {
		newBuffer(new byte[]{(byte) 0b00000001});
		assertEquals(Short.valueOf((short) -1), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort2b() throws Exception {
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) -1), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort3() throws Exception {
		newBuffer(new byte[]{(byte) 0b00000010});
		assertEquals(Short.valueOf((short) 1), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort3b() throws Exception {
		newBuffer(new byte[]{0, (byte) 0x01});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) 1), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort4() throws Exception {
		newBuffer(new byte[]{(byte) 0b01010011});
		assertEquals(Short.valueOf((short) -42), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort4b() throws Exception {
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xD6});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) -42), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort5() throws Exception {
		newBuffer(new byte[]{(byte) 0b01010100});
		assertEquals(Short.valueOf((short) 42), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort5b() throws Exception {
		newBuffer(new byte[]{0, (byte) 0x2A});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) 42), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort6() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b00000001});
		assertEquals(Short.valueOf((short) 64), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort6b() throws Exception {
		newBuffer(new byte[]{0, (byte) 0x40});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) 64), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort7() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111111, (byte) 0b01111111});
		assertEquals(Short.valueOf((short) -8192), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort7b() throws Exception {
		newBuffer(new byte[]{(byte) 0xE0, 0});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) -8192), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort8() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b10000000, (byte) 0b00000001});
		assertEquals(Short.valueOf((short) 8192), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort8b() throws Exception {
		newBuffer(new byte[]{(byte) 0x20, 0});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) 8192), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort9() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111110, (byte) 0b11111111, (byte) 0b00000011});
		assertEquals(Short.valueOf((short) 32767), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort9b() throws Exception {
		newBuffer(new byte[]{(byte) 0x7F, (byte) 0xFF});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) 32767), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort10() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111111, (byte) 0b11111111, (byte) 0b00000011});
		assertEquals(Short.valueOf((short) -32768), decoder.decodeShort());
	}

	@Test
	public void testDecodeShort10b() throws Exception {
		newBuffer(new byte[]{(byte) 0x80, 0});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) -32768), decoder.decodeShort());
	}

	/**
	 * Test of decodeNullableShort method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableShort1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableShort());
	}

	@Test
	public void testDecodeNullableShort2() throws Exception {
		newBuffer(new byte[]{
			1, (byte) 0b11101101, (byte) 0b11101010, (byte) 0b00000011,
			1, (byte) 0b10000010, (byte) 0b10000000, (byte) 0b00000001,
			1, (byte) 0,
			1, (byte) 0b11111111, (byte) 0b00000011,
			0
		});
		assertEquals(Short.valueOf((short) -31415), decoder.decodeNullableShort());
		assertEquals(Short.valueOf((short) 8193), decoder.decodeNullableShort());
		assertEquals(Short.valueOf((short) 0), decoder.decodeNullableShort());
		assertEquals(Short.valueOf((short) -256), decoder.decodeNullableShort());
		assertNull(decoder.decodeNullableShort());
	}

	@Test
	public void testDecodeNullableShort2b() throws Exception {
		newBuffer(new byte[]{
			1, (byte) 0x85, (byte) 0x49,
			1, (byte) 0x20, (byte) 0x01,
			1, 0, 0,
			1, (byte) 0xFF, 0,
			0
		});
		setVarintSupportedProperty(false);
		assertEquals(Short.valueOf((short) -31415), decoder.decodeNullableShort());
		assertEquals(Short.valueOf((short) 8193), decoder.decodeNullableShort());
		assertEquals(Short.valueOf((short) 0), decoder.decodeNullableShort());
		assertEquals(Short.valueOf((short) -256), decoder.decodeNullableShort());
		assertNull(decoder.decodeNullableShort());
	}

	/**
	 * Test of decodeShort method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeShortExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeShortExpectedException2() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000});
		try {
			decoder.decodeShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeShortExpectedException2b() throws Exception {
		newBuffer(new byte[]{(byte) 0x01});
		setVarintSupportedProperty(false);
		try {
			decoder.decodeShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeShortExpectedException3() throws Exception {
		newBuffer(new byte[]{(byte) 0b10101010, (byte) 0b10101010});
		try {
			decoder.decodeShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeShortExpectedException4() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b10000000, (byte) 0b10000000});
		try {
			decoder.decodeShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	@Test
	public void testDecodeShortExpectedException5() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111110, (byte) 0b11111111, (byte) 0b00000111});
		try {
			System.out.println(decoder.decodeShort());
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	/**
	 * Test of decodeUShort method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeUShort1() throws Exception {
		newBuffer(new byte[]{0});
		assertEquals(new UShort(0), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort1b() throws Exception {
		newBuffer(new byte[]{0, 0});
		setVarintSupportedProperty(false);
		assertEquals(new UShort(0), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort2() throws Exception {
		newBuffer(new byte[]{(byte) 0b00000001});
		assertEquals(new UShort(1), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort2b() throws Exception {
		newBuffer(new byte[]{0, (byte) 0b00000001});
		setVarintSupportedProperty(false);
		assertEquals(new UShort(1), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort3() throws Exception {
		newBuffer(new byte[]{(byte) 0b00000010});
		assertEquals(new UShort(2), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort4() throws Exception {
		newBuffer(new byte[]{(byte) 0b01111111});
		assertEquals(new UShort(127), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort4b() throws Exception {
		newBuffer(new byte[]{0, (byte) 0b01111111});
		setVarintSupportedProperty(false);
		assertEquals(new UShort(127), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort5() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b00000001});
		assertEquals(new UShort(128), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort5b() throws Exception {
		newBuffer(new byte[]{0, (byte) 0b10000000});
		setVarintSupportedProperty(false);
		assertEquals(new UShort(128), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort6() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000001, (byte) 0b00000001});
		assertEquals(new UShort(129), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort7() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111111, (byte) 0b00000001});
		assertEquals(new UShort(255), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort7b() throws Exception {
		newBuffer(new byte[]{0, (byte) 0b11111111});
		setVarintSupportedProperty(false);
		assertEquals(new UShort(255), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort8() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b00000010});
		assertEquals(new UShort(256), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort9() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111111, (byte) 0b01111111});
		assertEquals(new UShort(16383), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort10() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b10000000, (byte) 0b00000001});
		assertEquals(new UShort(16384), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort11() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000001, (byte) 0b10000000, (byte) 0b00000001});
		assertEquals(new UShort(16385), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort12() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b10000000, (byte) 0b00000010});
		assertEquals(new UShort(32768), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort12b() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, 0});
		setVarintSupportedProperty(false);
		assertEquals(new UShort(32768), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort13() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111111, (byte) 0b11111111, (byte) 0b00000011});
		assertEquals(new UShort(65535), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort13b() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111111, (byte) 0b11111111});
		setVarintSupportedProperty(false);
		assertEquals(new UShort(65535), decoder.decodeUShort());
	}

	@Test
	public void testDecodeUShort14() throws Exception {
		newBuffer(new byte[]{(byte) 0b10101011, (byte) 0b10111011, (byte) 0b00000010});
		assertEquals(new UShort(40363), decoder.decodeUShort());
	}

	/**
	 * Test of decodeNullableUShort method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableUShort1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableUShort());
	}

	@Test
	public void testDecodeNullableUShort2() throws Exception {
		newBuffer(new byte[]{
			1, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b00000011,
			1, (byte) 0,
			0,
			1, (byte) 0b10110111, (byte) 0b11110101, (byte) 0b00000001,
			1, (byte) 0b10000000, (byte) 0b00000010
		});
		assertEquals(new UShort(65535), decoder.decodeNullableUShort());
		assertEquals(new UShort(0), decoder.decodeNullableUShort());
		assertNull(decoder.decodeNullableUShort());
		assertEquals(new UShort(31415), decoder.decodeNullableUShort());
		assertEquals(new UShort(256), decoder.decodeNullableUShort());
	}

	@Test
	public void testDecodeNullableUShort2b() throws Exception {
		newBuffer(new byte[]{
			1, (byte) 0b11111111, (byte) 0b11111111,
			1, 0, 0,
			0,
			1, (byte) 0b01111010, (byte) 0b10110111,
			1, (byte) 0b00000001, 0
		});
		setVarintSupportedProperty(false);
		assertEquals(new UShort(65535), decoder.decodeNullableUShort());
		assertEquals(new UShort(0), decoder.decodeNullableUShort());
		assertNull(decoder.decodeNullableUShort());
		assertEquals(new UShort(31415), decoder.decodeNullableUShort());
		assertEquals(new UShort(256), decoder.decodeNullableUShort());
	}

	/**
	 * Test of decodeUShort method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeUShortExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeUShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeUShortExpectedException2() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000});
		try {
			decoder.decodeUShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeUShortExpectedException2b() throws Exception {
		newBuffer(new byte[]{(byte) 0x01});
		setVarintSupportedProperty(false);
		try {
			decoder.decodeUShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeUShortExpectedException3() throws Exception {
		newBuffer(new byte[]{(byte) 0b10101010, (byte) 0b10101010});
		try {
			decoder.decodeUShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeUShortExpectedException4() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b10000000, (byte) 0b10000000});
		try {
			decoder.decodeUShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	@Test
	public void testDecodeUShortExpectedException5() throws Exception {
		newBuffer(new byte[]{(byte) 0b10101010, (byte) 0b10101010, (byte) 0b01010100});
		try {
			decoder.decodeUShort();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	/**
	 * Test of decodeInteger method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeInteger1() throws Exception {
		newBuffer(new byte[]{0});
		assertEquals(Integer.valueOf(0), decoder.decodeInteger());
	}

	@Test
	public void testDecodeInteger1b() throws Exception {
		newBuffer(new byte[]{0, 0, 0, 0});
		setVarintSupportedProperty(false);
		assertEquals(Integer.valueOf(0), decoder.decodeInteger());
	}

	@Test
	public void testDecodeInteger2() throws Exception {
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111});
		assertEquals(Integer.valueOf(-2147483648), decoder.decodeInteger());
	}

	@Test
	public void testDecodeInteger2b() throws Exception {
		newBuffer(new byte[]{(byte) 0x80, 0, 0, 0});
		setVarintSupportedProperty(false);
		assertEquals(Integer.valueOf(-2147483648), decoder.decodeInteger());
	}

	@Test
	public void testDecodeInteger3() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111});
		assertEquals(Integer.valueOf(2147483647), decoder.decodeInteger());
	}

	@Test
	public void testDecodeInteger3b() throws Exception {
		newBuffer(new byte[]{(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
		setVarintSupportedProperty(false);
		assertEquals(Integer.valueOf(2147483647), decoder.decodeInteger());
	}

	/**
	 * Test of decodeNullableInteger method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableInteger1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableInteger());
	}

	@Test
	public void testDecodeNullableInteger2() throws Exception {
		newBuffer(new byte[]{
			1, (byte) 0b00000010,
			0,
			1, (byte) 0b00000001,
			1, (byte) 0b10101100, (byte) 0b10110101, (byte) 0b11011110, (byte) 0b01110101
		});
		assertEquals(Integer.valueOf(1), decoder.decodeNullableInteger());
		assertNull(decoder.decodeNullableInteger());
		assertEquals(Integer.valueOf(-1), decoder.decodeNullableInteger());
		assertEquals(Integer.valueOf(123456854), decoder.decodeNullableInteger());
	}

	@Test
	public void testDecodeNullableInteger2b() throws Exception {
		newBuffer(new byte[]{
			1, 0, 0, 0, (byte) 0x01,
			0,
			1, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			1, (byte) 0x07, (byte) 0x5B, (byte) 0xCD, (byte) 0x56
		});
		setVarintSupportedProperty(false);
		assertEquals(Integer.valueOf(1), decoder.decodeNullableInteger());
		assertNull(decoder.decodeNullableInteger());
		assertEquals(Integer.valueOf(-1), decoder.decodeNullableInteger());
		assertEquals(Integer.valueOf(123456854), decoder.decodeNullableInteger());
	}

	/**
	 * Test of decodeInteger method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeIntegerExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeInteger();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeIntegerExpectedException2() throws Exception {
		newBuffer(new byte[]{(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80});
		try {
			decoder.decodeInteger();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeIntegerExpectedException2b() throws Exception {
		newBuffer(new byte[]{(byte) 0x80, (byte) 0x80, (byte) 0x80});
		setVarintSupportedProperty(false);
		try {
			decoder.decodeInteger();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeIntegerExpectedException3() throws Exception {
		newBuffer(new byte[]{(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80});
		try {
			decoder.decodeInteger();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	@Test
	public void testDecodeIntegerExpectedException4() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b01001111});
		try {
			decoder.decodeInteger();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	/**
	 * Test of decodeUInteger method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeUInteger1() throws Exception {
		newBuffer(new byte[]{0});
		assertEquals(new UInteger(0), decoder.decodeUInteger());
	}

	@Test
	public void testDecodeUInteger1b() throws Exception {
		newBuffer(new byte[]{0, 0, 0, 0});
		setVarintSupportedProperty(false);
		assertEquals(new UInteger(0), decoder.decodeUInteger());
	}

	@Test
	public void testDecodeUInteger2() throws Exception {
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111});
		assertEquals(new UInteger(4294967295L), decoder.decodeUInteger());
	}

	@Test
	public void testDecodeUInteger2b() throws Exception {
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
		setVarintSupportedProperty(false);
		assertEquals(new UInteger(4294967295L), decoder.decodeUInteger());
	}

	@Test
	public void testDecodeUInteger3() throws Exception {
		newBuffer(new byte[]{(byte) 0b10110111, (byte) 0b11110101, (byte) 0b00000001});
		assertEquals(new UInteger(31415), decoder.decodeUInteger());
	}

	@Test
	public void testDecodeUInteger3b() throws Exception {
		newBuffer(new byte[]{0, 0, (byte) 0x7A, (byte) 0xB7});
		setVarintSupportedProperty(false);
		assertEquals(new UInteger(31415), decoder.decodeUInteger());
	}

	/**
	 * Test of decodeNullableUInteger method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableUInteger1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableUInteger());
	}

	@Test
	public void testDecodeNullableUInteger2() throws Exception {
		newBuffer(new byte[]{
			1, (byte) 0b00000001,
			1, (byte) 0b11101011, (byte) 0b01111111,
			1, (byte) 0b11000101, (byte) 0b11010000, (byte) 0b10000100, (byte) 0b10000000, (byte) 0b00001000,
			0
		});
		assertEquals(new UInteger(1), decoder.decodeNullableUInteger());
		assertEquals(new UInteger(16363), decoder.decodeNullableUInteger());
		assertEquals(new UInteger(2147559493L), decoder.decodeNullableUInteger());
		assertNull(decoder.decodeNullableUInteger());
	}

	@Test
	public void testDecodeNullableUInteger2b() throws Exception {
		newBuffer(new byte[]{
			1, 0, 0, 0, (byte) 0x01,
			1, 0, 0, (byte) 0x3F, (byte) 0xEB,
			1, (byte) 0x80, (byte) 0x01, (byte) 0x28, (byte) 0x45,
			0
		});
		setVarintSupportedProperty(false);
		assertEquals(new UInteger(1), decoder.decodeNullableUInteger());
		assertEquals(new UInteger(16363), decoder.decodeNullableUInteger());
		assertEquals(new UInteger(2147559493L), decoder.decodeNullableUInteger());
		assertNull(decoder.decodeNullableUInteger());
	}

	/**
	 * Test of decodeUInteger method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeUIntegerExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeUInteger();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeUIntegerExpectedException2() throws Exception {
		newBuffer(new byte[]{(byte) 0xAA, (byte) 0xAA, (byte) 0xAA});
		try {
			decoder.decodeUInteger();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeUIntegerExpectedException2b() throws Exception {
		newBuffer(new byte[]{(byte) 0xAA, (byte) 0xAA, (byte) 0xAA});
		setVarintSupportedProperty(false);
		try {
			decoder.decodeUInteger();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeUIntegerExpectedException3() throws Exception {
		newBuffer(new byte[]{(byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA});
		try {
			decoder.decodeUInteger();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	@Test
	public void testDecodeUIntegerExpectedException4() throws Exception {
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b01001111});
		try {
			decoder.decodeUInteger();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	/**
	 * Test of decodeLong method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeLong1() throws Exception {
		newBuffer(new byte[]{0});
		assertEquals(Long.valueOf(0), decoder.decodeLong());
	}

	@Test
	public void testDecodeLong1b() throws Exception {
		newBuffer(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
		setVarintSupportedProperty(false);
		assertEquals(Long.valueOf(0), decoder.decodeLong());
	}

	@Test
	public void testDecodeLong2() throws Exception {
		newBuffer(new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000001
		});
		assertEquals(Long.valueOf(-9223372036854775808L), decoder.decodeLong());
	}

	@Test
	public void testDecodeLong2b() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x80, 0, 0, 0, 0, 0, 0, 0
		});
		setVarintSupportedProperty(false);
		assertEquals(Long.valueOf(-9223372036854775808L), decoder.decodeLong());
	}

	@Test
	public void testDecodeLong3() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000001
		});
		assertEquals(Long.valueOf(9223372036854775807L), decoder.decodeLong());
	}

	@Test
	public void testDecodeLong3b() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		});
		setVarintSupportedProperty(false);
		assertEquals(Long.valueOf(9223372036854775807L), decoder.decodeLong());
	}

	@Test
	public void testDecodeLong4() throws Exception {
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111});
		assertEquals(Long.valueOf(-2147483648L), decoder.decodeLong());
	}

	@Test
	public void testDecodeLong4b() throws Exception {
		newBuffer(new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0x80, 0, 0, 0
		});
		setVarintSupportedProperty(false);
		assertEquals(Long.valueOf(-2147483648L), decoder.decodeLong());
	}

	@Test
	public void testDecodeLong5() throws Exception {
		newBuffer(new byte[]{(byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111});
		assertEquals(Long.valueOf(2147483647L), decoder.decodeLong());
	}

	@Test
	public void testDecodeLong5b() throws Exception {
		newBuffer(new byte[]{0, 0, 0, 0, (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
		setVarintSupportedProperty(false);
		assertEquals(Long.valueOf(2147483647L), decoder.decodeLong());
	}

	/**
	 * Test of decodeNullableLong method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableLong1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableLong());
	}

	@Test
	public void testDecodeNullableLong2() throws Exception {
		newBuffer(new byte[]{
			0,
			1, (byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000001,
			1, (byte) 0b00000001,
			1, (byte) 0b10111110, (byte) 0b11001110, (byte) 0b10010111,
			(byte) 0b10111111, (byte) 0b11100100, (byte) 0b00000111
		});
		assertNull(decoder.decodeNullableLong());
		assertEquals(Long.valueOf(9223372036854775807L), decoder.decodeNullableLong());
		assertEquals(Long.valueOf(-1), decoder.decodeNullableLong());
		assertEquals(Long.valueOf(133747110815L), decoder.decodeNullableLong());
	}

	@Test
	public void testDecodeNullableLong2b() throws Exception {
		newBuffer(new byte[]{
			0,
			1, (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			1, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			1, 0, 0, 0, (byte) 0x1F,
			(byte) 0x23, (byte) 0xF2, (byte) 0xF3, (byte) 0x9F
		});
		setVarintSupportedProperty(false);
		assertNull(decoder.decodeNullableLong());
		assertEquals(Long.valueOf(9223372036854775807L), decoder.decodeNullableLong());
		assertEquals(Long.valueOf(-1), decoder.decodeNullableLong());
		assertEquals(Long.valueOf(133747110815L), decoder.decodeNullableLong());
	}

	/**
	 * Test of decodeLong method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeLongExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeLong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeLongExpectedException2() throws Exception {
		newBuffer(new byte[]{(byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB});
		try {
			decoder.decodeLong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeLongExpectedException2b() throws Exception {
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
		setVarintSupportedProperty(false);
		try {
			decoder.decodeLong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeLongExpectedException3() throws Exception {
		newBuffer(new byte[]{
			(byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB,
			(byte) 0xAB, (byte) 0xAB, (byte) 0xAB
		});
		try {
			decoder.decodeLong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeLongExpectedException4() throws Exception {
		newBuffer(new byte[]{
			(byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB,
			(byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB
		});
		try {
			decoder.decodeLong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	@Test
	public void testDecodeLongExpectedException5() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000010
		});
		try {
			decoder.decodeLong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	/**
	 * Test of decodeULong method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeULong1() throws Exception {
		newBuffer(new byte[]{0});
		assertEquals(new ULong(BigInteger.ZERO), decoder.decodeULong());
	}

	@Test
	public void testDecodeULong1b() throws Exception {
		newBuffer(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
		setVarintSupportedProperty(false);
		assertEquals(new ULong(BigInteger.ZERO), decoder.decodeULong());
	}

	@Test
	public void testDecodeULong2() throws Exception {
		newBuffer(new byte[]{(byte) 0b00000001});
		assertEquals(new ULong(BigInteger.ONE), decoder.decodeULong());
	}

	@Test
	public void testDecodeULong2b() throws Exception {
		newBuffer(new byte[]{0, 0, 0, 0, 0, 0, 0, (byte) 0b00000001});
		setVarintSupportedProperty(false);
		assertEquals(new ULong(BigInteger.ONE), decoder.decodeULong());
	}

	@Test
	public void testDecodeULong3() throws Exception {
		newBuffer(new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000001
		});
		assertEquals(new ULong(new BigInteger("18446744073709551615")), decoder.decodeULong());
	}

	@Test
	public void testDecodeULong3b() throws Exception {
		newBuffer(new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		});
		setVarintSupportedProperty(false);
		assertEquals(new ULong(new BigInteger("18446744073709551615")), decoder.decodeULong());
	}

	@Test
	public void testDecodeULong4() throws Exception {
		newBuffer(new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b01111111
		});
		assertEquals(new ULong(new BigInteger("9223372036854775807")), decoder.decodeULong());
	}

	@Test
	public void testDecodeULong4b() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		});
		setVarintSupportedProperty(false);
		assertEquals(new ULong(new BigInteger("9223372036854775807")), decoder.decodeULong());
	}

	@Test
	public void testDecodeULong5() throws Exception {
		newBuffer(new byte[]{(byte) 0b10010101, (byte) 0b10011010, (byte) 0b11101111, (byte) 0b00111010});
		assertEquals(new ULong(new BigInteger("123456789")), decoder.decodeULong());
	}

	@Test
	public void testDecodeULong5b() throws Exception {
		newBuffer(new byte[]{0, 0, 0, 0, (byte) 0x07, (byte) 0x5B, (byte) 0xCD, (byte) 0x15});
		setVarintSupportedProperty(false);
		assertEquals(new ULong(new BigInteger("123456789")), decoder.decodeULong());
	}

	/**
	 * Test of decodeNullableULong method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableULong1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableULong());
	}

	@Test
	public void testDecodeNullableULong2() throws Exception {
		newBuffer(new byte[]{
			0,
			1, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00000001,
			1, (byte) 0,
			1, (byte) 0b10011111, (byte) 0b11100111, (byte) 0b11001011,
			(byte) 0b10011111, (byte) 0b11110010, (byte) 0b00000011
		});
		assertNull(decoder.decodeNullableULong());
		assertEquals(new ULong(new BigInteger("18446744073709551615")), decoder.decodeNullableULong());
		assertEquals(new ULong(BigInteger.ZERO), decoder.decodeNullableULong());
		assertEquals(new ULong(new BigInteger("133747110815")), decoder.decodeNullableULong());
	}

	@Test
	public void testDecodeNullableULong2b() throws Exception {
		newBuffer(new byte[]{
			0,
			1, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			1, 0, 0, 0, 0, 0, 0, 0, 0,
			1, 0, 0, 0, (byte) 0x1F,
			(byte) 0x23, (byte) 0xF2, (byte) 0xF3, (byte) 0x9F
		});
		setVarintSupportedProperty(false);
		assertNull(decoder.decodeNullableULong());
		assertEquals(new ULong(new BigInteger("18446744073709551615")), decoder.decodeNullableULong());
		assertEquals(new ULong(BigInteger.ZERO), decoder.decodeNullableULong());
		assertEquals(new ULong(new BigInteger("133747110815")), decoder.decodeNullableULong());
	}

	/**
	 * Test of decodeULong method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeULongExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeULong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeULongExpectedException2() throws Exception {
		newBuffer(new byte[]{(byte) 0x99});
		try {
			decoder.decodeLong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeULongExpectedException2b() throws Exception {
		newBuffer(new byte[]{(byte) 0x99});
		setVarintSupportedProperty(false);
		try {
			decoder.decodeLong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeULongExpectedException3() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x99, (byte) 0x98, (byte) 0x97, (byte) 0x96, (byte) 0x95, (byte) 0x94,
			(byte) 0x93, (byte) 0x92, (byte) 0x91
		});
		try {
			decoder.decodeLong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeULongExpectedException4() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x80, (byte) 0x89, (byte) 0x81, (byte) 0x88, (byte) 0x82, (byte) 0x87,
			(byte) 0x83, (byte) 0x86, (byte) 0x84, (byte) 0x85
		});
		try {
			decoder.decodeLong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	@Test
	public void testDecodeULongExpectedException5() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b11111110, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00100000
		});
		try {
			decoder.decodeULong();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	/**
	 * Test of decodeString method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeString1() throws Exception {
		newBuffer(new byte[]{0});
		assertEquals("", decoder.decodeString());
	}

	@Test
	public void testDecodeString2() throws Exception {
		newBuffer(new byte[]{(byte) 3, (byte) 0x44, (byte) 0x4C, (byte) 0x52});
		assertEquals("DLR", decoder.decodeString());

	}

	@Test
	public void testDecodeString3() throws Exception {
		newBuffer(new byte[]{
			(byte) 42,
			(byte) 0x44, (byte) 0x65, (byte) 0x75, (byte) 0x74, (byte) 0x73, (byte) 0x63, (byte) 0x68, (byte) 0x65,
			(byte) 0x73, (byte) 0x20, (byte) 0x5A, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x72, (byte) 0x75,
			(byte) 0x6D, (byte) 0x20, (byte) 0x66, (byte) 0xC3, (byte) 0xBC, (byte) 0x72, (byte) 0x20, (byte) 0x4c,
			(byte) 0x75, (byte) 0x66, (byte) 0x74, (byte) 0x2D, (byte) 0x20, (byte) 0x75, (byte) 0x6E, (byte) 0x64,
			(byte) 0x20, (byte) 0x52, (byte) 0x61, (byte) 0x75, (byte) 0x6D, (byte) 0x66, (byte) 0x61, (byte) 0x68,
			(byte) 0x72, (byte) 0x74
		});
		assertEquals("Deutsches Zentrum für Luft- und Raumfahrt", decoder.decodeString());

	}

	@Test
	public void testDecodeString4() throws Exception {
		newBuffer(new byte[]{
			(byte) 8,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x93, (byte) 0xA1,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x91, (byte) 0xAC
		});
		assertEquals("\ud83d\udce1\ud83d\udc6c", decoder.decodeString());
	}

	/**
	 * Test of decodeNullableString method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableString1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableString());
	}

	@Test
	public void testDecodeNullableString2() throws Exception {
		newBuffer(new byte[]{
			(byte) 1, (byte) 8, (byte) 0xF0, (byte) 0x9F, (byte) 0x9A, (byte) 0x80, (byte) 0x20, (byte) 0x44, (byte) 0x4C, (byte) 0x52,
			(byte) 0,
			(byte) 1, (byte) 0,
			(byte) 1, (byte) 6, (byte) 0xE5, (byte) 0xAE, (byte) 0x87, (byte) 0xE5, (byte) 0xAE, (byte) 0x99
		});
		assertEquals("\ud83d\ude80 DLR", decoder.decodeNullableString());
		assertNull(decoder.decodeNullableString());
		assertEquals("", decoder.decodeNullableString());
		assertEquals("\u5b87\u5b99", decoder.decodeNullableString());
	}

	/**
	 * Test of decodeString method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeStringExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeString();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeStringExpectedException2() throws Exception {
		newBuffer(new byte[]{1});
		try {
			decoder.decodeString();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}

	}

	@Test
	public void testDecodeStringExpectedException3() throws Exception {
		newBuffer(new byte[]{2, 0x20});
		try {
			decoder.decodeString();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}

	}

/*	@Test
	public void testDecodeStringExpectedException4() throws Exception {
//		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001111});
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0b00001111});
		try {
			decoder.decodeString();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.LENGTH_NOT_SUPPORTED, ex.getMessage());
		}
	}
*/
	/**
	 * Test of decodeBlob method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeBlob1() throws Exception {
		newBuffer(new byte[]{0});
		assertEquals(new Blob(new byte[]{}), decoder.decodeBlob());
	}

	@Test
	public void testDecodeBlob2() throws Exception {
		newBuffer(new byte[]{1, 0});
		assertEquals(new Blob(new byte[]{0}), decoder.decodeBlob());

	}

	@Test
	public void testDecodeBlob3() throws Exception {
		newBuffer(new byte[]{4, (byte) 0x01, (byte) 0x02, (byte) 0xFF, (byte) 0xAB});
		assertEquals(new Blob(new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0xFF, (byte) 0xAB}), decoder.decodeBlob());

	}

	@Test
	public void testDecodeBlob4() throws Exception {
		final int randomArrLength = 65537;
		byte[] randomArr = new byte[randomArrLength + 3];
		new Random().nextBytes(randomArr);
		randomArr[0] = (byte) 0b10000001;
		randomArr[1] = (byte) 0b10000000;
		randomArr[2] = (byte) 0b00000100;
		newBuffer(randomArr);
		assertEquals(new Blob(Arrays.copyOfRange(randomArr, 3, randomArrLength + 3)), decoder.decodeBlob());
	}

	/**
	 * Test of decodeNullableBlob method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableBlob1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableBlob());
	}

	@Test
	public void testDecodeNullableBlob2() throws Exception {
		newBuffer(new byte[]{
			(byte) 1, (byte) 3, (byte) 0x44, (byte) 0x4C, (byte) 0x52,
			(byte) 0,
			(byte) 1, (byte) 0,
			(byte) 1, (byte) 1, (byte) 0x21
		});
		assertEquals(new Blob(new byte[]{(byte) 0x44, (byte) 0x4C, (byte) 0x52}), decoder.decodeNullableBlob());
		assertNull(decoder.decodeNullableBlob());
		assertEquals(new Blob(new byte[]{}), decoder.decodeNullableBlob());
		assertEquals(new Blob(new byte[]{(byte) 0x21}), decoder.decodeNullableBlob());
	}

	/**
	 * Test of decodeBlob method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeBlobExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeBlob();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeBlobExpectedException2() throws Exception {
		newBuffer(new byte[]{1});
		try {
			decoder.decodeBlob();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}

	}

	@Test
	public void testDecodeBlobExpectedException3() throws Exception {
		newBuffer(new byte[]{5, 0, 0, 0, 0});
		try {
			decoder.decodeBlob();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	/**
	 * Test of decodeDuration method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeDuration1() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b11111111
		});
		setTimeProperties("DURATION", "00100000", "9999-01-23T21:43:56", null);
		assertEquals(new Duration(-1), decoder.decodeDuration());
	}

	@Test
	public void testDecodeDuration2() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b00000001
		});
		setTimeProperties("DURATION", "00100000", "9999-01-23T21:43:56", null);
		assertEquals(new Duration(1), decoder.decodeDuration());

	}

	@Test
	public void testDecodeDuration3() throws Exception {
		newBuffer(new byte[]{
			0
		});
		setTimeProperties("DURATION", "00100000", "9999-01-23T21:43:56", null);
		assertEquals(new Duration(0), decoder.decodeDuration());

	}

	@Test
	public void testDecodeDuration4() throws Exception {
		newBuffer(new byte[]{
			(byte) 0xF0, (byte) 0xED, (byte) 0xFA
		});
		setTimeProperties("DURATION", "00101000", "9999-01-23T21:43:56", null);
		assertEquals(new Duration(-987654), decoder.decodeDuration());

	}

	@Test
	public void testDecodeDuration5() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		});
		setTimeProperties("DURATION", "00101100", "9999-01-23T21:43:56", null);
		assertEquals(new Duration(2147483647), decoder.decodeDuration());
	}

	/**
	 * Test of decodeNullableDuration method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableDuration() throws Exception {
		newBuffer(new byte[]{
			0, 1, 0
		});
		setTimeProperties("DURATION", "00100000", "9999-01-23T21:43:56", null);
		assertNull(decoder.decodeNullableDuration());
		assertEquals(new Duration(0), decoder.decodeNullableDuration());
	}

	/**
	 * Test of decodeDuration method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeDurationExpectedException() throws Exception {
		newBuffer(new byte[]{});
		try {
			setTimeProperties("DURATION", "00100000", "1970-01-01T00:00:00", null);
			decoder.decodeDuration();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	/**
	 * Test of decodeFineTime method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeFineTimeCUC1() throws Exception {
		newBuffer(new byte[]{
			0, 0, 0, 0, 0, 1
		});
		setTimeProperties("FINE_TIME", "1010001100001000", Configuration.MAL_FINE_TIME_EPOCH, Configuration.MAL_FINE_TIME_EPOCH_TIMESCALE);
		assertEquals(new FineTime(1), decoder.decodeFineTime());
	}

	@Test
	public void testDecodeFineTimeCUC2() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x43, (byte) 0xED, (byte) 0x70, (byte) 0x1F, (byte) 0x9A, (byte) 0xDD, (byte) 0x37, (byte) 0x47
		});
		setTimeProperties("FINE_TIME", "1010101100001000", Configuration.MAL_FINE_TIME_EPOCH, Configuration.MAL_FINE_TIME_EPOCH_TIMESCALE);
		assertEquals(new FineTime(4451696123456789012L), decoder.decodeFineTime());

	}

	@Test
	public void testDecodeFineTimeCUC3() throws Exception {
		newBuffer(new byte[]{
			0, (byte) 0x43, (byte) 0xED, (byte) 0x70, (byte) 0xFC, (byte) 0xD6, (byte) 0xE9, (byte) 0xE0, (byte) 0xDE
		});
		setTimeProperties("FINE_TIME", "1010111100001000", Configuration.MAL_FINE_TIME_EPOCH, Configuration.MAL_FINE_TIME_EPOCH_TIMESCALE);
		assertEquals(new FineTime(4451696987654321098L), decoder.decodeFineTime());
	}

	@Test
	public void testDecodeFineTimeCDS1() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0
		});
		setTimeProperties("FINE_TIME", "01001010", Configuration.MAL_FINE_TIME_EPOCH, "UTC");
		assertEquals(new FineTime(0 + 35000000000000L), decoder.decodeFineTime());
	}

	@Test
	public void testDecodeFineTimeCDS2() throws Exception {
		newBuffer(new byte[]{
			(byte) 0, (byte) 51, (byte) 0x02, (byte) 0xB3, (byte) 0x29, (byte) 0xFB, (byte) 0x1B, (byte) 0x3A, (byte) 0x0C, (byte) 0x14
		});
		setTimeProperties("FINE_TIME", "01001010", Configuration.MAL_FINE_TIME_EPOCH, "UTC");
		assertEquals(new FineTime(4451696123456789012L + 35000000000000L), decoder.decodeFineTime());

	}

	@Test
	public void testDecodeFineTimeCDS3() throws Exception {
		newBuffer(new byte[]{
			(byte) 0, (byte) 51, (byte) 0x02, (byte) 0xB3, (byte) 0x2D, (byte) 0x5B, (byte) 0x27, (byte) 0x00, (byte) 0x25, (byte) 0xCA
		});
		setTimeProperties("FINE_TIME", "01001010", Configuration.MAL_FINE_TIME_EPOCH, "UTC");
		assertEquals(new FineTime(4451696987654321098L + 35000000000000L), decoder.decodeFineTime());
	}

	@Test
	public void testDecodeFineTimeCCS1() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x07, (byte) 0xDD, 2, 21, 12, 34, 56, 98, 76, 54, 32, 10, 98
		});
		setTimeProperties("FINE_TIME", "01010110", "9999-01-23T21:43:56", null);
		assertEquals(new FineTime(4451696987654321098L + 35000000000000L), decoder.decodeFineTime());
	}

	@Test
	public void testDecodeFineTimeCCS2() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x07, (byte) 0xDD, (byte) 0x00, (byte) 52, 12, 34, 56, 12, 34, 56, 78, 90, 12
		});
		setTimeProperties("FINE_TIME", "01011110", "9999-01-23T21:43:56", null);
		assertEquals(new FineTime(4451696123456789012L + 35000000000000L), decoder.decodeFineTime());
	}

	/**
	 * Test of decodeNullableFineTime method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableFineTime() throws Exception {
		newBuffer(new byte[]{
			1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
		});
		setTimeProperties("FINE_TIME", "01001010", Configuration.MAL_FINE_TIME_EPOCH, "UTC");
		assertEquals(new FineTime(0 + 35000000000000L), decoder.decodeNullableFineTime());
		assertNull(decoder.decodeNullableFineTime());
	}

	/**
	 * Test of decodeFineTime method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeFineTimeExpectedException() throws Exception {
		newBuffer(new byte[]{});
		try {
			setTimeProperties("FINE_TIME", "00100000", "1970-01-01T00:00:00", "UTC");
			decoder.decodeFineTime();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	/**
	 * Test of decodeIdentifier method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeIdentifier1() throws Exception {
		newBuffer(new byte[]{0});
		assertEquals(new Identifier(""), decoder.decodeIdentifier());
	}

	@Test
	public void testDecodeIdentifier2() throws Exception {
		newBuffer(new byte[]{(byte) 3, (byte) 0x44, (byte) 0x4C, (byte) 0x52});
		assertEquals(new Identifier("DLR"), decoder.decodeIdentifier());

	}

	@Test
	public void testDecodeIdentifier3() throws Exception {
		newBuffer(new byte[]{
			(byte) 42,
			(byte) 0x44, (byte) 0x65, (byte) 0x75, (byte) 0x74, (byte) 0x73, (byte) 0x63, (byte) 0x68, (byte) 0x65,
			(byte) 0x73, (byte) 0x20, (byte) 0x5A, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x72, (byte) 0x75,
			(byte) 0x6D, (byte) 0x20, (byte) 0x66, (byte) 0xC3, (byte) 0xBC, (byte) 0x72, (byte) 0x20, (byte) 0x4c,
			(byte) 0x75, (byte) 0x66, (byte) 0x74, (byte) 0x2D, (byte) 0x20, (byte) 0x75, (byte) 0x6E, (byte) 0x64,
			(byte) 0x20, (byte) 0x52, (byte) 0x61, (byte) 0x75, (byte) 0x6D, (byte) 0x66, (byte) 0x61, (byte) 0x68,
			(byte) 0x72, (byte) 0x74
		});
		assertEquals(new Identifier("Deutsches Zentrum für Luft- und Raumfahrt"), decoder.decodeIdentifier());

	}

	@Test
	public void testDecodeIdentifier4() throws Exception {
		newBuffer(new byte[]{
			(byte) 8,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x93, (byte) 0xA1,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x91, (byte) 0xAC
		});
		assertEquals(new Identifier("\ud83d\udce1\ud83d\udc6c"), decoder.decodeIdentifier());
	}

	/**
	 * Test of decodeNullableIdentifier method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableIdentifier1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableIdentifier());
	}

	@Test
	public void testDecodeNullableIdentifier2() throws Exception {
		newBuffer(new byte[]{
			(byte) 1, (byte) 8, (byte) 0xF0, (byte) 0x9F, (byte) 0x9A, (byte) 0x80, (byte) 0x20, (byte) 0x44, (byte) 0x4C, (byte) 0x52,
			(byte) 1, (byte) 0,
			(byte) 1, (byte) 6, (byte) 0xE5, (byte) 0xAE, (byte) 0x87, (byte) 0xE5, (byte) 0xAE, (byte) 0x99,
			(byte) 0
		});
		assertEquals(new Identifier("\ud83d\ude80 DLR"), decoder.decodeNullableIdentifier());
		assertEquals(new Identifier(""), decoder.decodeNullableIdentifier());
		assertEquals(new Identifier("\u5b87\u5b99"), decoder.decodeNullableIdentifier());
		assertNull(decoder.decodeNullableIdentifier());
	}

	/**
	 * Test of decodeIdentifier method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeIdentifierExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeIdentifier();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeIdentifierExpectedException2() throws Exception {
		newBuffer(new byte[]{1});
		try {
			decoder.decodeIdentifier();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}

	}

	@Test
	public void testDecodeIdentifierExpectedException3() throws Exception {
		newBuffer(new byte[]{2, 0x20});
		try {
			decoder.decodeIdentifier();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}

	}

/*	@Test
	public void testDecodeIdentifierExpectedException4() throws Exception {
//		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001110});
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0b00001110});
		try {
			decoder.decodeIdentifier();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.LENGTH_NOT_SUPPORTED, ex.getMessage());
		}
	}
*/
        
	/**
	 * Test of decodeTime method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeTimeCUC1() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b00000000
		});
		setTimeProperties("TIME", "00100000", "1970-01-01T00:00:00", Configuration.JAVA_EPOCH_TIMESCALE);
		assertEquals(new Time(0), decoder.decodeTime());
	}

	@Test
	public void testDecodeTimeCUC2() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b00010110, (byte) 0b10010010, (byte) 0b01011110, (byte) 0b10001000
		});
		setTimeProperties("TIME", "00011100", "1958-01-01T00:00:00", "TAI");
		assertEquals(new Time(0), decoder.decodeTime());

	}

	@Test
	public void testDecodeTimeCUC3() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b00000000, (byte) 0b00000000, (byte) 0b01000010
		});
		setTimeProperties("TIME", "00100010", "1970-01-01T00:00:00", Configuration.JAVA_EPOCH_TIMESCALE);
		assertEquals(new Time(1), decoder.decodeTime());

	}

	@Test
	public void testDecodeTimeCUC4() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b01101000, (byte) 0b10100111, (byte) 0b00010010, (byte) 0b10010011, (byte) 0b00000001
		});
		setTimeProperties("TIME", "00011101", "1958-01-01T00:00:00", "TAI");
		assertEquals(new Time(1377088523004L), decoder.decodeTime());
	}

	@Test
	public void testDecodeTimeCUC5() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b00000011, (byte) 0b0001010, (byte) 0b00000111, (byte) 0b11001010, (byte) 0b00010011, (byte) 0b00011111, (byte) 0b01111101
		});
		setTimeProperties("TIME", "1010111000100000", "1600-01-01T00:00:00", "TAI");
		assertEquals(new Time(1377088523123L), decoder.decodeTime());
	}

	@Test
	public void testDecodeTimeCUC6() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b001101011, (byte) 0b01001111, (byte) 0b10010111, (byte) 0b11011001, (byte) 0b10110000, (byte) 0b01100010, (byte) 0b01001110
		});
		setTimeProperties("TIME", "00011111", "1958-01-01T00:00:00", "TAI");
		assertEquals(new Time(1421687121689L), decoder.decodeTime());
	}

	@Test
	public void testDecodeTimeCUC7() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b001101011, (byte) 0b01001111, (byte) 0b10010111, (byte) 0b11011001, (byte) 0b10110000, (byte) 0b10100011, (byte) 0b11010111
		});
		setTimeProperties("TIME", "00011111", "1958-01-01T00:00:00", "TAI");
		assertEquals(new Time(1421687121690L), decoder.decodeTime());
	}

	@Test
	public void testDecodeTimeCDS1() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0, (byte) 0b0
		});
		setTimeProperties("TIME", "01001000", "1970-01-01T00:00:00", Configuration.JAVA_EPOCH_TIMESCALE);
		assertEquals(new Time(0), decoder.decodeTime());
	}

	@Test
	public void testDecodeTimeCDS2() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b01001111, (byte) 0b01100001, (byte) 0b00000010, (byte) 0b10110011, (byte) 0b00101001, (byte) 0b11111011
		});
		setTimeProperties("TIME", "01000000", "1958-01-01T00:00:00", "TAI");
		assertEquals(new Time(1377088523123L), decoder.decodeTime());

	}

	@Test
	public void testDecodeTimeCDS3() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b00000010, (byte) 0b01001110, (byte) 0b00100110, (byte) 0b00000010, (byte) 0b10110011, (byte) 0b00101001, (byte) 0b10000100
		});
		setTimeProperties("TIME", "01001100", "1600-01-01T00:00:00", "TAI");
		assertEquals(new Time(1377088523004L), decoder.decodeTime());
	}

	@Test
	public void testDecodeTimeCCS1() throws Exception {
		newBuffer(new byte[]{
			(byte) 0b00000111, (byte) 0b10110010, 1, 1, 0, 0, 0
		});
		setTimeProperties("TIME", "01010000", "9999-01-23T21:43:56", null);
		assertEquals(new Time(0), decoder.decodeTime());
	}

	@Test
	public void testDecodeTimeCCS2() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x07, (byte) 0xDD, 8, 21, 12, 34, 56, 12, 30
		});
		setTimeProperties("TIME", "01010010", "9999-01-23T21:43:56", null);
		assertEquals(new Time(1377088523123L), decoder.decodeTime());

	}

	@Test
	public void testDecodeTimeCCS3() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x07, (byte) 0xB1, (byte) 0x01, (byte) 0x6D, 23, 59, 59, 99
		});
		setTimeProperties("TIME", "01011001", "9999-01-23T21:43:56", null);
		assertEquals(new Time(-10), decoder.decodeTime());

	}

	@Test
	public void testDecodeTimeCCS4() throws Exception {
		newBuffer(new byte[]{
			(byte) 0x07, (byte) 0xDD, (byte) 0x00, (byte) 0xE9, 12, 34, 56, 00, 40
		});
		setTimeProperties("TIME", "01011010", "9999-01-23T21:43:56", null);
		assertEquals(new Time(1377088523004L), decoder.decodeTime());
	}

	/**
	 * Test of decodeNullableTime method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableTime() throws Exception {
		newBuffer(new byte[]{
			0, 1, (byte) 0x07, (byte) 0xDD, (byte) 0x00, (byte) 0xE9, 12, 34, 56, 00, 40
		});
		setTimeProperties("TIME", "01011010", "9999-01-23T21:43:56", null);
		assertNull(decoder.decodeNullableTime());
		assertEquals(new Time(1377088523004L), decoder.decodeNullableTime());
	}

	/**
	 * Test of decodeTime method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeTimeExpectedException() throws Exception {
		newBuffer(new byte[]{});
		try {
			setTimeProperties("TIME", "00100000", "1970-01-01T00:00:00", Configuration.JAVA_EPOCH_TIMESCALE);
			decoder.decodeTime();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	/**
	 * Test of decodeURI method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeURI1() throws Exception {
		newBuffer(new byte[]{0});
		assertEquals(new URI(""), decoder.decodeURI());
	}

	@Test
	public void testDecodeURI2() throws Exception {
		newBuffer(new byte[]{
			(byte) 17,
			(byte) 0x68, (byte) 0x74, (byte) 0x74, (byte) 0x70, (byte) 0x3A, (byte) 0x2F, (byte) 0x2F, (byte) 0x77,
			(byte) 0x77, (byte) 0x77, (byte) 0x2E, (byte) 0x64, (byte) 0x6C, (byte) 0x72, (byte) 0x2E, (byte) 0x64,
			(byte) 0x65
		});
		assertEquals(new URI("http://www.dlr.de"), decoder.decodeURI());

	}

	@Test
	public void testDecodeURI3() throws Exception {
		newBuffer(new byte[]{
			(byte) 42,
			(byte) 0x44, (byte) 0x65, (byte) 0x75, (byte) 0x74, (byte) 0x73, (byte) 0x63, (byte) 0x68, (byte) 0x65,
			(byte) 0x73, (byte) 0x20, (byte) 0x5A, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x72, (byte) 0x75,
			(byte) 0x6D, (byte) 0x20, (byte) 0x66, (byte) 0xC3, (byte) 0xBC, (byte) 0x72, (byte) 0x20, (byte) 0x4c,
			(byte) 0x75, (byte) 0x66, (byte) 0x74, (byte) 0x2D, (byte) 0x20, (byte) 0x75, (byte) 0x6E, (byte) 0x64,
			(byte) 0x20, (byte) 0x52, (byte) 0x61, (byte) 0x75, (byte) 0x6D, (byte) 0x66, (byte) 0x61, (byte) 0x68,
			(byte) 0x72, (byte) 0x74
		});
		assertEquals(new URI("Deutsches Zentrum für Luft- und Raumfahrt"), decoder.decodeURI());

	}

	@Test
	public void testDecodeURI4() throws Exception {
		newBuffer(new byte[]{
			(byte) 8,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x93, (byte) 0xA1,
			(byte) 0xF0, (byte) 0x9F, (byte) 0x91, (byte) 0xAC
		});
		assertEquals(new URI("\ud83d\udce1\ud83d\udc6c"), decoder.decodeURI());
	}

	/**
	 * Test of decodeNullableURI method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableURI1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableURI());
	}

	@Test
	public void testDecodeNullableURI2() throws Exception {
		newBuffer(new byte[]{
			(byte) 0,
			(byte) 1, (byte) 8, (byte) 0xF0, (byte) 0x9F, (byte) 0x9A, (byte) 0x80, (byte) 0x20, (byte) 0x44, (byte) 0x4C, (byte) 0x52,
			(byte) 1, (byte) 0,
			(byte) 1, (byte) 6, (byte) 0xE5, (byte) 0xAE, (byte) 0x87, (byte) 0xE5, (byte) 0xAE, (byte) 0x99
		});
		assertNull(decoder.decodeNullableURI());
		assertEquals(new URI("\ud83d\ude80 DLR"), decoder.decodeNullableURI());
		assertEquals(new URI(""), decoder.decodeNullableURI());
		assertEquals(new URI("\u5b87\u5b99"), decoder.decodeNullableURI());
	}

	/**
	 * Test of decodeURI method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeURIExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeURI();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeURIExpectedException2() throws Exception {
		newBuffer(new byte[]{1});
		try {
			decoder.decodeIdentifier();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}

	}

	@Test
	public void testDecodeURIExpectedException3() throws Exception {
		newBuffer(new byte[]{4, (byte) 0x20, (byte) 0xAB, (byte) 0x00});
		try {
			decoder.decodeIdentifier();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}

	}

/*	@Test
	public void testDecodeURIExpectedException4() throws Exception {
//		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0b00001101});
		newBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0b00001101});
		try {
			decoder.decodeIdentifier();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.LENGTH_NOT_SUPPORTED, ex.getMessage());
		}
	}
*/
        
	/**
	 * Test of decodeElement method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeElement1() throws Exception {
		newBuffer(new byte[]{0});
		Element e = new URI();
		Element ret = decoder.decodeElement(e);
		assertEquals(new URI(""), ret);
		assertNotSame(e, ret);
	}

	@Test
	public void testDecodeElement2() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b00000001});
		Element e = new UShort();
		Element ret = decoder.decodeElement(e);
		assertEquals(new UShort(128), ret);
		assertNotSame(e, ret);

	}

	@Test
	public void testDecodeElement3() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000});
		Element e = new Union(new Byte((byte) 0));
		Element ret = decoder.decodeElement(e);
		assertEquals(new Union(new Byte((byte) -128)), ret);
		assertNotSame(e, ret);
	}

	/**
	 * Test of decodeNullableElement method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableElement1() throws Exception {
		newBuffer(new byte[]{0});
		Element e = new UShort();
		Element ret = decoder.decodeNullableElement(e);
		assertNull(ret);
		assertNotSame(e, ret);
	}

	@Test
	public void testDecodeNullableElement2() throws Exception {
		newBuffer(new byte[]{
			(byte) 1, (byte) 0,
			(byte) 0,
			(byte) 0,
			(byte) 1, (byte) 0
		});
		Element e1 = new Blob(new byte[]{});
		Element ret1 = decoder.decodeNullableElement(e1);
		assertEquals(e1, ret1);
		assertNotSame(e1, ret1);

		Element e2 = new ULong();
		Element ret2 = decoder.decodeNullableElement(e2);
		assertNull(ret2);
		assertNotSame(e2, ret2);

		Element e3 = new Identifier();
		Element ret3 = decoder.decodeNullableElement(e3);
		assertNull(ret3);
		assertNotSame(e3, ret3);

		Element e4 = new Union(new Integer(4711));
		Element ret4 = decoder.decodeNullableElement(e4);
		assertEquals(new Union(new Integer(0)), ret4);
		assertNotSame(e4, ret4);
	}

	/**
	 * Test of decodeElement method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeElementExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeElement(new URI());
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeElementExpectedException2() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeElement(null);
			fail("Expected exception, but none thrown.");
		} catch (IllegalArgumentException ex) {
			assertEquals(SPPEncoder.ILLEGAL_NULL_ARGUMENT, ex.getMessage());
		}

	}

	@Test
	public void testDecodeElementExpectedException3() throws Exception {
		newBuffer(new byte[]{1});
		try {
			decoder.decodeElement(new URI());
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}

	}

	@Test
	public void testDecodeElementExpectedException4() throws Exception {
		newBuffer(new byte[]{(byte) 0b10000000, (byte) 0b10000000, (byte) 0b10000000});
		try {
			decoder.decodeElement(new Union(Short.valueOf((short) 0)));
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INVALID_VALUE, ex.getMessage());
		}
	}

	/**
	 * Test of decodeAttribute method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeAttribute1() throws Exception {
		newBuffer(new byte[]{11, 42});
		assertEquals(new UInteger(42), decoder.decodeAttribute());
	}

	@Test
	public void testDecodeAttribute2() throws Exception {
		newBuffer(new byte[]{10, 84});
		assertEquals(new Union(new Integer(42)), decoder.decodeAttribute());
	}

	/**
	 * Test of decodeNullableAttribute method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeNullableAttribute1() throws Exception {
		newBuffer(new byte[]{0});
		assertNull(decoder.decodeNullableAttribute());
	}

	@Test
	public void testDecodeNullableAttribute2() throws Exception {
		newBuffer(new byte[]{
			(byte) 1, (byte) 17, (byte) 3, (byte) 0x44, (byte) 0x4C, (byte) 0x52,
			(byte) 1, (byte) 1, (byte) 1,
			(byte) 0,
			(byte) 1, (byte) 0, (byte) 2, (byte) 0x42, (byte) 0x0
		});
		assertEquals(new URI("DLR"), decoder.decodeNullableAttribute());
		assertEquals(new Union(Boolean.TRUE), decoder.decodeNullableAttribute());
		assertNull(decoder.decodeNullableAttribute());
		assertEquals(new Blob(new byte[]{0x42, 0x0}), decoder.decodeNullableAttribute());
	}

	/**
	 * Test of decodeAttribute method, of class SPPDecoder.
	 */
	@Test
	public void testDecodeAttributeExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			decoder.decodeAttribute();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testDecodeAttributeExpectedException2() throws Exception {
		newBuffer(new byte[]{0});
		try {
			decoder.decodeAttribute();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}

	}

	@Test
	public void testDecodeAttributeExpectedException3() throws Exception {
		newBuffer(new byte[]{111});
		try {
			decoder.decodeAttribute();
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals("Unknown attribute type short form: 112", ex.getMessage());
		}
	}

	/**
	 * Test of createListDecoder method, of class SPPDecoder.
	 */
	@Test
	public void testCreateListDecoder1() throws Exception {
		List<Element> list = new ArrayList<>();
		newBuffer(new byte[]{0});
		MALListDecoder listDecoder = decoder.createListDecoder(list);
		assertFalse(listDecoder.hasNext());
	}

	@Test
	public void testCreateListDecoder2() throws Exception {
		List<Element> list = new ArrayList<>();
		newBuffer(new byte[]{3});
		MALListDecoder listDecoder = decoder.createListDecoder(list);
		assertTrue(listDecoder.hasNext());

	}

	@Test
	public void testCreateListDecoder3() throws Exception {
		List<Element> list = new ArrayList<>();
		list.add(new Union(new Integer(42)));
		list.add(new URI("http://www.dlr.de"));
		newBuffer(new byte[]{2});
		MALListDecoder listDecoder = decoder.createListDecoder(list);
		assertFalse(listDecoder.hasNext());
	}

	/**
	 * Test of createListDecoder method, of class SPPDecoder.
	 */
	@Test
	public void testCreateListDecoderExpectedException1() throws Exception {
		newBuffer(new byte[]{});
		try {
			List<Element> list = new ArrayList<>();
			MALListDecoder listDecoder = decoder.createListDecoder(list);
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testCreateListDecoderExpectedException2() throws Exception {
		newBuffer(new byte[]{});
		try {
			MALListDecoder listDecoder = decoder.createListDecoder(null);
			fail("Expected exception, but none thrown.");
		} catch (IllegalArgumentException ex) {
			assertEquals(SPPEncoder.ILLEGAL_NULL_ARGUMENT, ex.getMessage());
		}
	}

	private void newBuffer(byte[] buffer) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("de.dlr.gsoc.mo.malspp.VARINT_SUPPORTED", "TRUE");
		this.decoder = new SPPDecoder(new ByteArrayInputStream(buffer), properties);
	}

	private void setTimeProperties(String timeFormat, String pField, String epoch, String timescale) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("de.dlr.gsoc.mo.malspp." + timeFormat + "_CODE_FORMAT", pField);
		properties.put("de.dlr.gsoc.mo.malspp." + timeFormat + "_EPOCH", epoch);
		if (null != timescale) {
			properties.put("de.dlr.gsoc.mo.malspp." + timeFormat + "_EPOCH_TIMESCALE", timescale);
		}
		decoder.setProperties(properties);
	}

	private void setVarintSupportedProperty(Boolean varintSupported) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("de.dlr.gsoc.mo.malspp.VARINT_SUPPORTED", varintSupported.toString());
		decoder.setProperties(properties);
	}
}
