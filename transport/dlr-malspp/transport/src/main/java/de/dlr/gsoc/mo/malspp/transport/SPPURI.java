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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ccsds.moims.mo.mal.structures.URI;

/**
 * Class for representing valid MAL/SPP URIs as defined in section 3.2 of CCSDS 524.1 as immutable
 * objects.
 *
 * The general format of a MAL/SPP URI is 'malspp:{qualifier}/{APID}[/{identifier}]'. qualifier is
 * the APID qualifier according the Space Packet protocol. 0 &lt; APID qualifier &lt 65536. APID is
 * the Space Packet Protocol APID number. 0 &le; APID &lt; 2047. identifier is the source or
 * destination identifier number according to MAL/SPP. 0 &le; identifier &lt 256.
 *
 * All fields except the identifier are mandatory.
 */
public class SPPURI {

    private URI uri;
    private int qualifier;
    private short apid;
    private Short identifier;
    private static final String SCHEME_NAME = "malspp";
    private static final String INVALID_URI = "Not a valid MAL/SPP URI.";
    private static final Pattern URI_PATTERN = Pattern.compile("\\A" + SCHEME_NAME + ":(\\d{1,5})/(\\d{1,4})(?:/(\\d{1,3}))?\\z");

    public SPPURI(final int qualifier, final short apid, final Short identifier) {
        init(qualifier, apid, identifier);
    }

    public SPPURI(final int qualifier, final int apid, final Integer identifier) {
        init(qualifier, (short) apid, null == identifier ? null : identifier.shortValue());
    }

    public SPPURI(final int qualifier, final short apid) {
        init(qualifier, apid, null);
    }

    public SPPURI(final int qualifier, final int apid) {
        init(qualifier, (short) apid, null);
    }

    public SPPURI(final URI uri) {
        this(uri.getValue());
    }

    public SPPURI(final String uri) {
        final Matcher m = URI_PATTERN.matcher(uri);
        if (!m.matches()) {
            throw new IllegalArgumentException(INVALID_URI + "URI: " + uri);
        }
        init(Integer.parseInt(m.group(1)), Short.parseShort(m.group(2)), m.group(3) == null ? null : Short.valueOf(m.group(3)));
    }

    private void init(final int qualifier, final short apid, final Short identifier) {
        if (qualifier < 0 || qualifier > 65535
                || apid < 0 || apid >= 2047
                || (identifier != null && (identifier < 0 || identifier > 255))) {
            throw new IllegalArgumentException(INVALID_URI);
        }
        this.qualifier = qualifier;
        this.apid = apid;
        this.identifier = identifier;
        String u = SCHEME_NAME + ":" + qualifier + "/" + apid;
        if (identifier != null) {
            u += "/" + identifier;
        }
        this.uri = new URI(u);
    }

    public URI getURI() {
        return uri;
    }

    public short getAPID() {
        return apid;
    }

    public Short getIdentifier() {
        return identifier;
    }

    public int getQualifier() {
        return qualifier;
    }

    @Override
    public String toString() {
        return "SPPURI{" + "uri=" + uri + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.uri);
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.uri, ((SPPURI) obj).uri);
    }
}
