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
import org.ccsds.moims.mo.mal.MALException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class SPPElementOutputStreamTest extends SPPElementIOStreamTestHelper {

    private static ByteArrayOutputStream os;
    private static SPPElementOutputStream eos;

    @BeforeClass
    public static void setUpClass() {
        os = new ByteArrayOutputStream();
        eos = new SPPElementOutputStream(os, fixedQosProperties);
    }

    @Before
    public void setUp() {
        os.reset();
    }

    @After
    public void tearDown() throws MALException {
        eos.flush();
        eos.close();
    }

    @Override
    protected void performTest(TestContext testContext, TestActualType actualType, TestDeclaredType declaredType,
        byte[] buffer) throws Exception {
        //System.out.println("OS testing: " + testContext.toString() + ", actual " + actualType.toString() + ", declared " + declaredType.toString());
        TestData d = constructTest(testContext, actualType, declaredType);
        eos.writeElement(d.element, d.ctx);
        os.flush();
        assertArrayEquals(buffer, os.toByteArray());
    }

    @Test
    public void testWriteElementPubSubPublish() throws Exception {
        performTest(TestContext.PUBSUB_PUBLISH_UPDATE, TestActualType.UPDATE_LIST, TestDeclaredType.CONCRETE,
            new byte[]{3, 1, 3, 2, (byte) 0xAB, (byte) 0xCD, 0, 1, 2, 1, (byte) 0xEF});
    }

    @Test
    public void testWriteElementPubSubNotify1() throws Exception {
        performTest(TestContext.PUBSUB_NOTIFY_UPDATE, TestActualType.MAL_ENCODED_ELEMENT, TestDeclaredType.CONCRETE,
            new byte[]{4, 1, 8, 1, 13, 0, 1, 21});
    }

    @Test
    public void testWriteElementPubSubNotify2() throws Exception {
        performTest(TestContext.PUBSUB_NOTIFY_UPDATE, TestActualType.MAL_ENCODED_ELEMENT_LIST,
            TestDeclaredType.CONCRETE, new byte[]{4, 0, 1, 11, 1, 13, 1, 17});
    }

    @Test
    public void testWriteElementPubSub1() throws Exception {
        try {
            performTest(TestContext.PUBSUB, TestActualType.NULL, TestDeclaredType.CONCRETE, new byte[]{1});
            fail("Expected exception, but none thrown.");
        } catch (MALException ex) {
            assertEquals(SPPElementOutputStream.INVALID_ELEMENT_TYPE, ex.getMessage());
        }
    }

    @Test
    public void testWriteElementPubSub2() throws Exception {
        try {
            performTest(TestContext.PUBSUB, TestActualType.NULL, TestDeclaredType.ABSTRACT_ATTRIBUTE, new byte[]{1});
            fail("Expected exception, but none thrown.");
        } catch (MALException ex) {
            assertEquals(SPPElementOutputStream.INVALID_ELEMENT_TYPE, ex.getMessage());
        }
    }

    @Test
    public void testWriteElementPubSub3() throws Exception {
        try {
            performTest(TestContext.PUBSUB, TestActualType.NULL, TestDeclaredType.ABSTRACT_ELEMENT, new byte[]{1});
            fail("Expected exception, but none thrown.");
        } catch (MALException ex) {
            assertEquals(SPPElementOutputStream.INVALID_ELEMENT_TYPE, ex.getMessage());
        }
    }

    @Test
    public void testWriteElementNullContext1() throws Exception {
        performTest(TestContext.NULL, TestActualType.MAL_ENCODED_ELEMENT, TestDeclaredType.CONCRETE, new byte[]{4, 1, 8,
                                                                                                                1, 13,
                                                                                                                0, 1,
                                                                                                                21});
    }

    @Test
    public void testWriteElementNullContext2() throws Exception {
        performTest(TestContext.NULL, TestActualType.MAL_ENCODED_ELEMENT_LIST, TestDeclaredType.CONCRETE, new byte[]{4,
                                                                                                                     0,
                                                                                                                     1,
                                                                                                                     11,
                                                                                                                     1,
                                                                                                                     13,
                                                                                                                     1,
                                                                                                                     17});
    }
}
