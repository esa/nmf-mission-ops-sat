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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Blob;

public class SPPElementStreamFactory extends MALElementStreamFactory {

    private static final String ILLEGAL_NULL_ARGUMENT = "Argument may not be null.";
    private Map properties;

    @Override
    protected void init(final String protocol, final Map properties) throws IllegalArgumentException, MALException {
        if (protocol == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        this.properties = properties;
    }

    @Override
    public MALElementInputStream createInputStream(final InputStream is) throws IllegalArgumentException, MALException {
        if (is == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        return new SPPElementInputStream(is, properties);
    }

    @Override
    public MALElementInputStream createInputStream(final byte[] bytes, final int offset)
        throws IllegalArgumentException, MALException {
        if (bytes == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }
        return createInputStream(new ByteArrayInputStream(bytes, offset, bytes.length - offset));
    }

    @Override
    public MALElementOutputStream createOutputStream(final OutputStream os) throws IllegalArgumentException,
        MALException {
        return new SPPElementOutputStream(os, properties);
    }

    // PENDING: It is unclear, what the elements array is supposed to represent. Here: Ignore the
    // body element index of the context and set it to the element index for each element in
    // elements.
    @Override
    public Blob encode(final Object[] elements, final MALEncodingContext ctx) throws IllegalArgumentException,
        MALException {

        if (elements == null || ctx == null) {
            throw new IllegalArgumentException(ILLEGAL_NULL_ARGUMENT);
        }

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final MALElementOutputStream eos = createOutputStream(os);

        for (int i = 0; i < elements.length; i++) {
            ctx.setBodyElementIndex(i);
            try {
                eos.writeElement(elements[i], ctx);
            } catch (final IllegalArgumentException | MALException ex) {
                Logger.getLogger(SPPElementStreamFactory.class.getName()).log(Level.SEVERE, "The Element is type: " +
                    elements[i].getClass().getName() + " - " + elements[i].toString(), ex);
                throw ex;
            }
        }

        try {
            os.flush();
        } catch (final IOException ex) {
            throw new MALException(ex.getMessage(), ex);
        }
        return new Blob(os.toByteArray());
    }
}
