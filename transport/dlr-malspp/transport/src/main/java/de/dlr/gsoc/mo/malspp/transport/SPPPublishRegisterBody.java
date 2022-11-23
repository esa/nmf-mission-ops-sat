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
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.EntityKeyList;
import org.ccsds.moims.mo.mal.transport.MALEncodedBody;
import org.ccsds.moims.mo.mal.transport.MALPublishRegisterBody;

public class SPPPublishRegisterBody extends SPPMessageBody implements MALPublishRegisterBody {

    public SPPPublishRegisterBody(final Object[] bodyElements, final MALElementStreamFactory esf,
        final MALEncodingContext ctx) {
        super(bodyElements, esf, ctx);
    }

    public SPPPublishRegisterBody(final MALEncodedBody encodedBody, final MALElementStreamFactory esf,
        final MALEncodingContext ctx) {
        super(encodedBody, esf, ctx);
    }

    @Override
    public EntityKeyList getEntityKeyList() throws MALException {
        return (EntityKeyList) getBodyElement(0, new EntityKeyList());
    }
}
