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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALElementFactoryRegistry;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.transport.MALEncodedBody;
import org.ccsds.moims.mo.mal.transport.MALEncodedElement;
import org.ccsds.moims.mo.mal.transport.MALMessageBody;

public class SPPMessageBody implements MALMessageBody {

	protected static final String NOT_SUPPORTED = "Operation not supported by MAL/SPP binding layer.";
	private static final String OUT_OF_BOUNDS = "Body element index out of bounds.";
	private List<Object> bodyElements;
	private MALEncodedBody encodedBody;
	private boolean isEncoded;
	private boolean isDecoded;
	protected MALEncodingContext ctx;
	private final MALElementStreamFactory esf;
	protected Object[] shortForms;

	public SPPMessageBody(final Object[] bodyElements, final MALElementStreamFactory esf, final MALEncodingContext ctx) {
		if (bodyElements == null) {
			this.bodyElements = null;
		} else if (bodyElements.length == 1 && bodyElements[0] instanceof MALStandardError) {
			final MALStandardError err = (MALStandardError) bodyElements[0];
			this.bodyElements = Arrays.asList(new Object[]{err.getErrorNumber(), err.getExtraInformation()});
		} else {
			this.bodyElements = Arrays.asList(bodyElements);
		}
		this.ctx = ctx;
		this.esf = esf;
		this.shortForms = ctx.getOperation()
				.getOperationStage(ctx.getHeader().getInteractionStage())
				.getElementShortForms();
		this.isEncoded = false;
		this.isDecoded = true;
	}

	public SPPMessageBody(final MALEncodedBody encodedBody, final MALElementStreamFactory esf, final MALEncodingContext ctx) {
		this.encodedBody = encodedBody;
		this.ctx = ctx;
		this.esf = esf;
		this.shortForms = ctx.getOperation()
				.getOperationStage(ctx.getHeader().getInteractionStage())
				.getElementShortForms();
		this.isEncoded = true;
		this.isDecoded = false;
	}

	@Override
	public int getElementCount() {
		return shortForms.length;
	}

	@Override
	public Object getBodyElement(final int index, final Object element) throws MALException {
		if (!isDecoded) {
			// Decode all body elements, though only one body element is requested here. But there
			// is no way of decoding a body element with an arbitrary index without decoding all
			// previous ones. Ignore the prototype element given as parameter here and use the
			// service provided list of short forms instead.
			if (getElementCount() != 0) {
				bodyElements = new ArrayList<>(getElementCount());
				final MALElementFactoryRegistry elementFactoryRegistry = MALContextFactory.getElementFactoryRegistry();
				final MALElementInputStream is = esf.createInputStream(
						encodedBody.getEncodedBody().getValue(),
						encodedBody.getEncodedBody().getOffset());
				for (int i = 0; i < shortForms.length; i++) {
					final Object shortForm = shortForms[i];
					Object e = null;
					if (shortForm != null) {
						e = elementFactoryRegistry.lookupElementFactory(shortForm).createElement();
					}
					ctx.setBodyElementIndex(i);
                                        try{
        					bodyElements.add(is.readElement(e, ctx));
                                        }catch(final org.ccsds.moims.mo.mal.MALException ex){
//                                                Logger.getLogger(SPPMessageBody.class.getName()).log(Level.INFO, "Unable to decode element with index: " + i, ex);
                                                throw new MALException("Unable to decode element with index: " + i, ex);
                                        }
				}
			}
			isDecoded = true;
		}
		if (getElementCount() == 0) {
			throw new MALException(OUT_OF_BOUNDS);
		}
		return bodyElements.get(index);
	}

	@Override
	public MALEncodedElement getEncodedBodyElement(final int index) throws MALException {
		throw new MALException(NOT_SUPPORTED);
	}

	@Override
	public MALEncodedBody getEncodedBody() throws MALException {
		if (!isEncoded) {
			if (getElementCount() == 0) {
				encodedBody = null;
			} else {
                                final Blob encodedStuff = esf.encode(bodyElements.toArray(), ctx);
				encodedBody = new MALEncodedBody(encodedStuff);
			}
			isEncoded = true;
		}
		return encodedBody;
	}

	/**
	 * Prepares the message body to be used for sending the message in-process.
	 *
	 * @throws MALException
	 */
	protected void prepareInProcessBody() throws MALException {
		// TODO: This is inefficient, because although the message is already (partly) decoded, we
		// discard the decoded message, trigger the encoding mechanism and then have to decode the
		// message again when it is received in the same process.
		getEncodedBody();
		isDecoded = false;
	}
}
