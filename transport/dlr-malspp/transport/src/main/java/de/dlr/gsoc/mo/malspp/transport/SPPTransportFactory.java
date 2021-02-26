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

import java.util.Map;
import org.ccsds.moims.mo.mal.MALContext;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.transport.MALTransport;
import org.ccsds.moims.mo.mal.transport.MALTransportFactory;

public class SPPTransportFactory extends MALTransportFactory {

	private static final String SUPPORTED_PROTOCOL = "malspp";
	private static final String PROTOCOL_NOT_SUPPORTED = "Protocol not supported: ";
	private static SPPTransport transport;

	public SPPTransportFactory(final String protocol) throws IllegalArgumentException {
		super(protocol);
	}

	@Override
	public MALTransport createTransport(final MALContext malContext, final Map properties) throws MALException {
		final String protocol = getProtocol();

		if (!protocol.equals(SUPPORTED_PROTOCOL)) {
			throw new MALException(PROTOCOL_NOT_SUPPORTED + protocol);
		}

		if (null == transport || transport.isClosed()) {
			transport = new SPPTransport(protocol, properties);
			return transport;
		}
		return transport;
	}
}
