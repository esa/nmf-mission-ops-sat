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

import de.dlr.gsoc.mo.malspp.encoding.SPPElementStreamFactory;
import java.io.InputStream;
import java.util.Map;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class SPPElementStreamFactoryTest extends SPPElementIOStreamTestHelper {

	/**
	 * Test of encode method, of class SPPElementStreamFactory.
	 */
	// By inheriting from SPPElementIOStreamTestHelper and overriding performTest() the encode()
	// method is tested.
	@Override
	protected void performTest(TestContext testContext, TestActualType actualType, TestDeclaredType declaredType, byte[] buffer) throws Exception {
		//System.out.println("OS testing: " + testContext.toString() + ", actual " + actualType.toString() + ", declared " + declaredType.toString());
		TestData d = constructTest(testContext, actualType, declaredType);
		Object[] elements = new Object[]{d.element};
		SPPElementStreamFactory esf = new SPPElementStreamFactory();
		esf.init("malspp", fixedQosProperties);
		Blob result = esf.encode(elements, d.ctx);
		assertArrayEquals(buffer, result.getValue());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEncodeExpectedExcpetion() throws Exception {
		Object[] elements = null;
		MALEncodingContext ctx = mock(MALEncodingContext.class, RETURNS_DEEP_STUBS);
		SPPElementStreamFactory instance = new SPPElementStreamFactory();
		instance.init("malspp", fixedQosProperties);
		Blob result = instance.encode(elements, ctx);
	}

	// Override null context methods to add an expected exception test because a null context is not
	// allowed here.
	@Test(expected = IllegalArgumentException.class)
	@Override
	public void testElementNullContext1() throws Exception {
		super.testElementNullContext1();
	}

	@Test(expected = IllegalArgumentException.class)
	@Override
	public void testElementNullContext2() throws Exception {
		super.testElementNullContext2();
	}

	@Test(expected = IllegalArgumentException.class)
	@Override
	public void testElementNullContext3() throws Exception {
		super.testElementNullContext3();
	}

	@Test(expected = IllegalArgumentException.class)
	@Override
	public void testElementNullContext4() throws Exception {
		super.testElementNullContext4();
	}

	/**
	 * Test of init method, of class SPPElementStreamFactory.
	 */
	@Test
	public void testInit() throws Exception {
		String protocol = "";
		Map properties = null;
		SPPElementStreamFactory instance = new SPPElementStreamFactory();
		instance.init(protocol, properties);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInitExpectedExcpetion() throws Exception {
		String protocol = null;
		Map properties = null;
		SPPElementStreamFactory instance = new SPPElementStreamFactory();
		instance.init(protocol, properties);
	}

	/**
	 * Test of createInputStream method, of class SPPElementStreamFactory.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateInputStream_InputStreamExpectedException() throws Exception {
		InputStream is = null;
		SPPElementStreamFactory instance = new SPPElementStreamFactory();
		instance.init("malspp", fixedQosProperties);
		MALElementInputStream result = instance.createInputStream(is);
	}

	/**
	 * Test of createInputStream method, of class SPPElementStreamFactory.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateInputStream_byteArrExpectedException() throws Exception {
		byte[] bytes = null;
		int offset = 0;
		SPPElementStreamFactory instance = new SPPElementStreamFactory();
		instance.init("malspp", fixedQosProperties);
		MALElementInputStream result = instance.createInputStream(bytes, offset);
	}
}