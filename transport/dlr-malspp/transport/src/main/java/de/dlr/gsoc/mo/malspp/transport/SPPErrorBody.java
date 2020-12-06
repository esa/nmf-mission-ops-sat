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

import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.transport.MALEncodedBody;
import org.ccsds.moims.mo.mal.transport.MALErrorBody;

public class SPPErrorBody extends SPPMessageBody implements MALErrorBody {

    public SPPErrorBody(final Object[] bodyElements, final MALElementStreamFactory esf, final MALEncodingContext ctx) {
        super(bodyElements, esf, ctx);
        // Explicitly set the short forms here because querying the service as is done in the super
        // class yields the short forms of the non-error body of the same operation and stage, which
        // is not what we want here.
        shortForms = new Object[]{UInteger.UINTEGER_SHORT_FORM, null};
    }

    public SPPErrorBody(final MALEncodedBody encodedBody, final MALElementStreamFactory esf, final MALEncodingContext ctx) {
        super(encodedBody, esf, ctx);
        shortForms = new Object[]{UInteger.UINTEGER_SHORT_FORM, null};
    }

    @Override
    public MALStandardError getError() throws MALException {
        UInteger errorNumber = (UInteger) getBodyElement(0, new UInteger());
        Object extraInformation = getBodyElement(1, null);
        return new MALStandardError(errorNumber, extraInformation);
    }
}
