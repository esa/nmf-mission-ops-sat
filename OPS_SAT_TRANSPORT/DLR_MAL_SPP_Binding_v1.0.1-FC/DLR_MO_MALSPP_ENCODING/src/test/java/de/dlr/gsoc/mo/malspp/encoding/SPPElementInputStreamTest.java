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

import de.dlr.gsoc.mo.malspp.encoding.SPPElementInputStream;
import de.dlr.gsoc.mo.malspp.encoding.SPPDecoder;
import java.io.ByteArrayInputStream;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import static de.dlr.gsoc.mo.malspp.encoding.SPPElementOutputStreamTest.*;
import java.util.ListIterator;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALElementFactory;
import org.ccsds.moims.mo.mal.MALElementFactoryRegistry;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Element;
import org.ccsds.moims.mo.mal.structures.IdBooleanPair;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.transport.MALEncodedElement;
import org.ccsds.moims.mo.mal.transport.MALEncodedElementList;
import org.junit.Test;

public class SPPElementInputStreamTest extends SPPElementIOStreamTestHelper {

	private static SPPElementInputStream eis;

	@BeforeClass
	public static void setUpClass() {
		MALElementFactory elementFactoryInteger = new MALElementFactory() {
			@Override
			public Object createElement() {
				return new Integer(0);
			}
		};
		MALElementFactory elementFactoryUOctet = new MALElementFactory() {
			@Override
			public Object createElement() {
				return new UOctet();
			}
		};
		MALElementFactory elementFactoryIdBooleanPair = new MALElementFactory() {
			@Override
			public Object createElement() {
				return new IdBooleanPair();
			}
		};
		MALElementFactoryRegistry elementFactoryRegistry = MALContextFactory.getElementFactoryRegistry();
		elementFactoryRegistry.registerElementFactory(Attribute.INTEGER_SHORT_FORM, elementFactoryInteger);
		elementFactoryRegistry.registerElementFactory(Attribute.UOCTET_SHORT_FORM, elementFactoryUOctet);
		elementFactoryRegistry.registerElementFactory(IdBooleanPair.SHORT_FORM, elementFactoryIdBooleanPair);
	}

	@Override
	protected void performTest(TestContext testContext, TestActualType actualType, TestDeclaredType declaredType, byte[] buffer) throws Exception {
		//System.out.println("IS testing: " + testContext.toString() + ", actual " + actualType.toString() + ", declared " + declaredType.toString());
		TestData d = constructTest(testContext, actualType, declaredType);
		Object e;
		if (declaredType == TestDeclaredType.ABSTRACT_ATTRIBUTE
				|| declaredType == TestDeclaredType.ABSTRACT_ELEMENT
				|| actualType == TestActualType.MAL_ENCODED_ELEMENT_LIST
				|| d.element == null) {
			e = null;
		} else if (d.element instanceof Integer) {
			e = new Integer(0);
		} else {
			e = ((Element) d.element).createElement();
		}
		newBuffer(buffer);
		Object element = eis.readElement(e, d.ctx);
		// PENDING: MALEncodedElement.equals() is not implemented properly and thus uses the
		// standard Object.equals() to test for identity, not for equality. Therefore a workaround
		// is needed here.
		if (actualType == TestActualType.MAL_ENCODED_ELEMENT_LIST) {
			assertTrue(element instanceof MALEncodedElementList);
			assertEquals(((MALEncodedElementList) d.element).size(), ((MALEncodedElementList) element).size());
			ListIterator<MALEncodedElement> e1 = ((MALEncodedElementList) d.element).listIterator();
			ListIterator<MALEncodedElement> e2 = ((MALEncodedElementList) element).listIterator();
			while (e1.hasNext() && e2.hasNext()) {
				MALEncodedElement o1 = e1.next();
				MALEncodedElement o2 = e2.next();
				if (o1 == null) {
					assertNull(o2);
				} else {
					assertEquals(o1.getEncodedElement(), o2.getEncodedElement());
				}
			}
			return;
		}
		assertEquals(d.element, element);
		if (d.element != null) {
			assertNotSame(d.element, element);
			assertTrue(element instanceof Boolean
					|| element instanceof Float
					|| element instanceof Double
					|| element instanceof Byte
					|| element instanceof Short
					|| element instanceof Integer
					|| element instanceof Long
					|| element instanceof String
					|| element instanceof Element
					|| element instanceof MALEncodedElement
					|| element instanceof MALEncodedElementList);
		}
	}

	private static void newBuffer(byte[] buffer) {
		eis = new SPPElementInputStream(new ByteArrayInputStream(buffer), fixedQosProperties);
	}

	@Test
	public void testReadElementPubSub1() throws Exception {
		try {
			performTest(TestContext.PUBSUB, TestActualType.NULL, TestDeclaredType.CONCRETE, new byte[]{
				0});
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testReadElementPubSub2() throws Exception {
		try {
			performTest(TestContext.PUBSUB, TestActualType.NULL, TestDeclaredType.ABSTRACT_ATTRIBUTE, new byte[]{
				0});
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testReadElementPubSub3() throws Exception {
		try {
			performTest(TestContext.PUBSUB, TestActualType.NULL, TestDeclaredType.ABSTRACT_ELEMENT, new byte[]{
				0});
			fail("Expected exception, but none thrown.");
		} catch (MALException ex) {
			assertEquals(SPPDecoder.INSUFFICIENT_DATA, ex.getMessage());
		}
	}

	@Test
	public void testReadElementPubSubPublish() throws Exception {
		performTest(TestContext.PUBSUB_PUBLISH_UPDATE, TestActualType.MAL_ENCODED_ELEMENT_LIST, TestDeclaredType.CONCRETE, new byte[]{
			4, 0, 1, 1, 11, 1, 1, 13, 1, 1, 17});
	}
}