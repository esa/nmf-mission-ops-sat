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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListDecoder;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Element;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.transport.MALEncodedElement;
import org.ccsds.moims.mo.mal.transport.MALEncodedElementList;

public class SPPElementInputStream implements MALElementInputStream {

	private final InputStream is;
	private final SPPDecoder decoder;

	public SPPElementInputStream(final InputStream is, final Map properties) {
		this.is = is;
		this.decoder = new SPPDecoder(is, properties);
	}

	@Override
	public Object readElement(final Object element, final MALEncodingContext ctx) throws IllegalArgumentException, MALException {
		// Treat null context as non-nullable standard case with only concrete types allowed.
		if (ctx == null) {
			return decoder.decodeElement(getUnionizedElement(element));
		}

		final ServiceInfo service = new ServiceInfo(ctx);
		final boolean isPubSub = service.getInteraction().equals(InteractionType.PUBSUB);

		// Element of type List< <<Update Value Type>> > means it is not of type Identifier or
		// List<UpdateHeader>. Thus isUpdateValueTypeList is only meaningful in the context of
		// PubSub-Publish or PubSub-Notify.
		final boolean isUpdateValueTypeList
				= (!Identifier.IDENTIFIER_SHORT_FORM.equals(service.getShortForm()))
				&& (!UpdateHeaderList.SHORT_FORM.equals(service.getShortForm()));

		// Condition checking according to 3.5.3.3 MALSPP book:
		if (isPubSub
				&& service.getStage().equals(MALPubSubOperation.PUBLISH_STAGE)
				&& isUpdateValueTypeList
				&& !service.isErrorMessage()) {
			return readElementPubSubPublishUpdate(element, service);
		}

		if (isPubSub
				&& service.getStage().equals(MALPubSubOperation.NOTIFY_STAGE)
				&& isUpdateValueTypeList
				&& !service.isErrorMessage()) {
			return readElementStandard(element, service);
		}

		if ((isPubSub && !service.isErrorMessage())
				|| (service.isErrorMessage() && service.getBodyElementIndex() == 0)) {
			return readElementStandard(element, service);
		}

		return readNullableElementStandard(element, service);
	}

	private Object readElementPubSubPublishUpdate(final Object element, final ServiceInfo service) throws MALException {
		// The updates in a received Publish message do not need to be decoded, because only
		// brokers receive these messages.
		// Body Element [PubSub/Publish/ListOfUpdates] <- (Type Info) <- ListLength <- NullTag <- EncodedUpdateSize <- MALEncodedElement
		//                                                                                |-----------------------------------^
		//                                                               |<------------------------- BlobList ------------------------>|
		final Long updateListShortForm = service.isDeclaredAbstract()
				? getPrototype(element).getShortForm()
				: service.getShortForm();
		// updateListShortForm denotes the list type, the corresponding element type is sign
		// flipped. Exchange the last 24 bits of the absolute short form with the sign flipped type.
		final Long updateValueShortForm = (updateListShortForm & ~0xFFFFFF)
				| (-(updateListShortForm & 0xFFFFFF) & 0xFFFFFF);
		final MALEncodedElementList encElemList = new MALEncodedElementList(updateValueShortForm, 10); // initial list size set arbitrarily
		final MALListDecoder listDecoder = decoder.createListDecoder(encElemList);
		// Treat each list element as nullable Blob. This is compatible with 3.5.3.5.1 MALSPP.
		Blob b;
		while (listDecoder.hasNext()) {
			b = listDecoder.decodeNullableBlob();
			encElemList.add((b == null) ? null : new MALEncodedElement(b));
		}
		return encElemList;
	}

	 private Object readElementStandard(final Object element, final ServiceInfo service) throws MALException {
		if (service.isDeclaredAttribute()) {
			return decoder.decodeAttribute();
		}
		final Element e = getPrototype(element);
		return decoder.decodeElement(e);
	}

	private Object readNullableElementStandard(final Object element, final ServiceInfo service) throws MALException {
		if (decoder.isNull()) {
			return null;
		}
		return readElementStandard(element, service);
	}

	@Override
	public void close() throws MALException {
		try {
			is.close();
		} catch (final IOException ex) {
			throw new MALException(ex.getMessage(), ex);
		}
	}

	/**
	 * Returns a MAL Element for all accepted element types. If a Java mapping type is supplied it
	 * will be wrapped in a Union, otherwise the element is cast to Element.
	 *
	 * @param element Element to unionize.
	 * @return The unionized element or a MAL Element.
	 * @throws MALException
	 */
	private static Element getUnionizedElement(final Object element) throws MALException {
		if (element == null) {
			return null;
		} else if (!(element instanceof Element)) {
			return SPPElementOutputStream.getUnion(element);
		}
		try {
			return (Element) element;
		} catch (final ClassCastException ex) {
			throw new MALException(ex.getMessage(), ex);
		}
	}

	/**
	 * Returns a prototype element that is used to tell the decoder what to decode.
	 *
	 * @param element Either a prototype element or null.
	 * @return If element is non-null, it will be returned as prototype. If element is null the type
	 * information is read from the input stream and a prototype element is constructed.
	 */
	private Element getPrototype(final Object element) throws MALException {
		if (element != null) {
			// Concrete element: Element contains a template element that should be decoded.
			return getUnionizedElement(element);
		}
		// Abstract element: Element is null and the type has to be read from the input stream.
		// Absolute short form can be read directly from input stream because encoding defined in
		// MALSPP Book (5.2.3) coincides with absolute short form definition in MAL Java API
		// (4.5.5.2.1).
		final byte[] b = decoder.read(8);
		long shortForm = 0;
		for (int i = 0; i < 8; i++) {
			shortForm <<= 8;
			shortForm |= (long) b[i] & 0xFF;
		}
		try {
			return getUnionizedElement(MALContextFactory.getElementFactoryRegistry()
					.lookupElementFactory(shortForm).createElement());
		} catch (final NullPointerException ex) {
			throw new MALException(ex.getMessage(), ex);
		}
	}
}
