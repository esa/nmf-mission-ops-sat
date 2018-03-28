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

import de.dlr.gsoc.mo.malspp.transport.SPPCounter;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.Test;
import static org.junit.Assert.*;

public class SPPCounterTest {

	@Test(expected = IllegalArgumentException.class)
	public void testIncrement1() {
		long wrap = 0;
		SPPCounter c = new SPPCounter(wrap);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncrement2() {
		long wrap = -1;
		SPPCounter c = new SPPCounter(wrap);
	}

	@Test
	public void testIncrement3() {
		long wrap = 1;
		int delta = 0;
		long[] exp = new long[]{};
		SPPCounter c = new SPPCounter(wrap);
		Iterator<Long> iter = c.increment(delta);
		assertTrue(checkIterator(exp, iter));
	}

	@Test
	public void testIncrement4() {
		long wrap = 1;
		int delta = 1;
		long[] exp = new long[]{0};
		SPPCounter c = new SPPCounter(wrap);
		Iterator<Long> iter = c.increment(delta);
		assertTrue(checkIterator(exp, iter));
	}

	@Test
	public void testIncrement5() {
		long wrap = 1;
		int delta = 2;
		long[] exp = new long[]{0, 0};
		SPPCounter c = new SPPCounter(wrap);
		Iterator<Long> iter = c.increment(delta);
		assertTrue(checkIterator(exp, iter));
	}

	@Test
	public void testIncrement6() {
		long wrap = 5;
		int delta = 10;
		long[] exp = new long[]{0, 1, 2, 3, 4, 0, 1, 2, 3, 4};
		SPPCounter c = new SPPCounter(wrap);
		Iterator<Long> iter = c.increment(delta);
		assertTrue(checkIterator(exp, iter));
	}

	@Test
	public void testIncrement7() {
		long wrap = 3;
		int delta1 = 10;
		int delta2 = 5;
		long[] exp1 = new long[]{0, 1, 2, 0, 1, 2, 0, 1, 2, 0};
		long[] exp2 = new long[]{1, 2, 0, 1, 2};
		SPPCounter c = new SPPCounter(wrap);
		Iterator<Long> iter1 = c.increment(delta1);
		Iterator<Long> iter2 = c.increment(delta2);
		assertTrue(checkIterator(exp2, iter2));
		assertTrue(checkIterator(exp1, iter1));
	}

	@Test
	public void testIncrement8() {
		long wrap = 10;
		int delta1 = 5;
		int delta2 = 15;
		long[] exp1 = new long[]{0, 1, 2, 3, 4};
		long[] exp2 = new long[]{5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
		SPPCounter c = new SPPCounter(wrap);
		Iterator<Long> iter1 = c.increment(delta1);
		Iterator<Long> iter2 = c.increment(delta2);
		assertTrue(checkIterator(exp1, iter1));
		assertTrue(checkIterator(exp2, iter2));
	}

	private boolean checkIterator(long[] expected, Iterator<Long> iterator) {
		int i = 0;
		for (; iterator.hasNext(); i++) {
			assertEquals("Error at position " + i, expected[i], (long) iterator.next());
		}
		assertEquals(expected.length, i);
		try {
			iterator.next();
		} catch (NoSuchElementException ex) {
			return true;
		}
		return false;
	}
}
