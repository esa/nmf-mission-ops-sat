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
package de.dlr.gsoc.mo.malspp.transport;

import org.ccsds.moims.mo.mal.structures.URI;
import org.junit.Test;
import static org.junit.Assert.*;

public class SPPURITest {

    @Test
    public void testURI1() throws Exception {
        SPPURI uri = new SPPURI("malspp:417/0/2");
        assertEquals(0, uri.getAPID());
        assertEquals(Short.valueOf((short) 2), uri.getIdentifier());
        assertEquals(417, uri.getQualifier());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI2() throws Exception {
        SPPURI uri = new SPPURI("malspp:1234/255/");
    }

    @Test
    public void testURI3() throws Exception {
        SPPURI uri = new SPPURI("malspp:1/2046/0");
        assertEquals(2046, uri.getAPID());
        assertEquals(Short.valueOf((short) 0), uri.getIdentifier());
        assertEquals(1, uri.getQualifier());
    }

    @Test
    public void testURI4() throws Exception {
        SPPURI uri = new SPPURI("malspp:0/2046/0");
        assertEquals(2046, uri.getAPID());
        assertEquals(Short.valueOf((short) 0), uri.getIdentifier());
        assertEquals(0, uri.getQualifier());
    }

    @Test
    public void testURI5() throws Exception {
        SPPURI uri = new SPPURI("malspp:0/2046");
        assertEquals(2046, uri.getAPID());
        assertNull(uri.getIdentifier());
        assertEquals(0, uri.getQualifier());
    }

    @Test
    public void testURI6() throws Exception {
        SPPURI uri = new SPPURI("malspp:65535/2046/0");
        assertEquals(2046, uri.getAPID());
        assertEquals(Short.valueOf((short) 0), uri.getIdentifier());
        assertEquals(65535, uri.getQualifier());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI7() throws Exception {
        SPPURI uri = new SPPURI("malspp:65536/2046/0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI8() throws Exception {
        SPPURI uri = new SPPURI("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI9() throws Exception {
        SPPURI uri = new SPPURI("malspp:1/2047/0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI10() throws Exception {
        SPPURI uri = new SPPURI("malspp:1/123/256");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI11() throws Exception {
        SPPURI uri = new SPPURI("malspp://0/0");
    }

    @Test
    public void testURI12() throws Exception {
        SPPURI uri1 = new SPPURI("malspp:417/0/2");
        SPPURI uri2 = new SPPURI(new URI("malspp:417/0/2"));
        assertEquals(uri1, uri2);
    }

    @Test
    public void testURI13() throws Exception {
        SPPURI uri1 = new SPPURI("malspp:128/1111");
        SPPURI uri2 = new SPPURI(new URI("malspp:128/1111"));
        assertEquals(uri1, uri2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI14() throws Exception {
        SPPURI uri = new SPPURI("malspp:-1/1111/0");
    }

    @Test
    public void testURI15() throws Exception {
        SPPURI uri = new SPPURI(12345, 47, 0);
        assertEquals(uri, new SPPURI("malspp:12345/47/0"));
    }

    @Test
    public void testURI16() throws Exception {
        SPPURI uri = new SPPURI(0, 47, null);
        assertEquals(uri, new SPPURI("malspp:0/47"));
    }

    @Test
    public void testURI17() throws Exception {
        SPPURI uri = new SPPURI(0, 47, 0);
        assertEquals(uri, new SPPURI("malspp:0/47/0"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI18() throws Exception {
        SPPURI uri = new SPPURI(0, 2047, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI19() throws Exception {
        SPPURI uri = new SPPURI(0, 256, 256);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI20() throws Exception {
        SPPURI uri = new SPPURI(1, -1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI21() throws Exception {
        SPPURI uri = new SPPURI(1, 0, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURI22() throws Exception {
        SPPURI uri = new SPPURI(-1, 0, 1);
    }
}