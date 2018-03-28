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

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SPPCounter {

	private final long wrap;
	private long counter;

	/**
	 * Creates new counter, that starts at 0 and wraps around at wrap (i.e. this value is never
	 * reached).
	 *
	 * @param wrap The value where the counter wraps around to start at 0 again.
	 */
	public SPPCounter(final long wrap) {
		if (wrap < 1) {
			throw new IllegalArgumentException();
		}
		this.counter = -1;
		this.wrap = wrap;
	}

	/**
	 * Increment the counter by delta steps, wrapping around correctly and returning an iterator for
	 * accessing the corresponding counter values. The number of generated values is delta.
	 *
	 * @param delta The number of counter values to generate. If negative, the counter is decreased
	 * accordingly, but the iterator will not return any value.
	 * @return Iterator for accessing the generated counter values.
	 */
	public synchronized Iterator<Long> increment(final int delta) {
		final long old = counter;
		counter = (counter + delta) % wrap;

		// No synchronization of the Iterator methods because it should only be accessed from a
		// single thread.
		return new Iterator<Long>() {
			private long c = old;
			private int d = delta;

			@Override
			public boolean hasNext() {
				return d > 0;
			}

			@Override
			public Long next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				d--;
				c = (c + 1) % wrap;
				return c;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Not supported.");
			}
		};
	}
}
