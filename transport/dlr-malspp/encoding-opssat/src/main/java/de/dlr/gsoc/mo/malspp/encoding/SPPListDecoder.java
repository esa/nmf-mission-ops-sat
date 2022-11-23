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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListDecoder;

public class SPPListDecoder extends SPPDecoder implements MALListDecoder {

    private final List list;
    private final int size;

    public SPPListDecoder(final InputStream inputStream, final List list, final Map properties) throws MALException {
        super(inputStream, properties);
        final int listSize = decodeUShort().getValue();
        if (listSize > 65535) {
            throw new MALException(LENGTH_NOT_SUPPORTED);
        }
        this.size = listSize;
        this.list = list;
    }

    @Override
    public boolean hasNext() {
        return list.size() < size;
    }

    @Override
    public int size() {
        return size;
    }

}
