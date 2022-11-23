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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListEncoder;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Element;
import org.ccsds.moims.mo.mal.structures.ElementList;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.structures.Union;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.transport.MALEncodedElement;
import org.ccsds.moims.mo.mal.transport.MALEncodedElementList;

public class SPPElementOutputStream implements MALElementOutputStream {

    private final OutputStream os;
    private final SPPEncoder encoder;
    protected static final String INVALID_ELEMENT_TYPE = "Supplied element type cannot be handled by the transport layer.";

    private enum ElementType {

        ELEMENT, JAVA_MAPPED, ENCODED_ELEMENT, ENCODED_LIST, NULL
    }

    public SPPElementOutputStream(final OutputStream os, final Map properties) {
        this.os = os;
        this.encoder = new SPPEncoder(os, properties);
    }

    @Override
    public void writeElement(final Object element, final MALEncodingContext ctx) throws IllegalArgumentException,
        MALException {
        if (ctx == null) {
            writeElementNullContext(element);
            return;
        }

        final ServiceInfo service = new ServiceInfo(ctx);
        final boolean isPubSub = service.getInteraction().equals(InteractionType.PUBSUB);

        // Element of type List< <<Update Value Type>> > means it is not of type Identifier or
        // List<UpdateHeader>. Thus isUpdateValueTypeList is only meaningful in the context of
        // PubSub-Publish or PubSub-Notify.
        final boolean isUpdateValueTypeList = (!Identifier.IDENTIFIER_SHORT_FORM.equals(service.getShortForm())) &&
            (!UpdateHeaderList.SHORT_FORM.equals(service.getShortForm()));

        // Condition checking according to 3.5.3.3 MALSPP book:
        if (isPubSub && service.getStage().equals(MALPubSubOperation.PUBLISH_STAGE) && isUpdateValueTypeList && !service
            .isErrorMessage()) {
            writeElementPubSubPublishUpdate(element, service);
        } else if (isPubSub && service.getStage().equals(MALPubSubOperation.NOTIFY_STAGE) && isUpdateValueTypeList &&
            !service.isErrorMessage()) {
            writeElementPubSubNotifyUpdate(element, service);
        } else if ((isPubSub && !service.isErrorMessage()) || (service.isErrorMessage() && service
            .getBodyElementIndex() == 0)) {
            writeElementStandard(element, service);
        } else {
            writeNullableElementStandard(element, service);
        }
    }

    private void writeElementNullContext(Object element) throws MALException {
        // Treat null context as non-nullable standard case with only concrete types allowed and
        // element one of Element, MALEncodedELement, MALEncodedELementList.
        switch (getElementType(element)) {
            case NULL:
                throw new IllegalArgumentException();
            case JAVA_MAPPED:
                element = getUnion(element);
                // fall through on purpose
            case ELEMENT:
                encoder.encodeElement((Element) element);
                break;
            case ENCODED_ELEMENT:
                encoder.write(((MALEncodedElement) element).getEncodedElement().getValue());
                break;
            case ENCODED_LIST:
                final MALEncodedElementList encodedList = (MALEncodedElementList) element;
                // listEncoder only needed for implicitly encoding the list size
                final MALListEncoder listEncoder = encoder.createListEncoder(encodedList);
                for (final MALEncodedElement encodedElement : encodedList) {
                    encoder.encodeNulltag(encodedElement);
                    if (encodedElement != null) {
                        encoder.write(encodedElement.getEncodedElement().getValue());
                    }
                }
                listEncoder.close();
                break;
        }
    }

    private void writeElementPubSubPublishUpdate(final Object element, final ServiceInfo service) throws MALException {
        // Only MAL element types need to be handled here, because this case does not happen
        // in a broker.
        // Body Element [PubSub/Publish/ListOfUpdates] -> (Type info) -> ListLength -> NullTag -> EncodedUpdateSize -> Element
        //                                                                                ^-------------------------------|
        switch (getElementType(element)) {
            case NULL:
                break;
            case ELEMENT:
                // the element in question here is the update list 
                final ElementList<Element> updateList = (ElementList<Element>) element;
                if (service.isDeclaredAbstract()) {
                    encodeShortForm(updateList.getShortForm());
                }
                // listEncoder only needed for implicitly encoding the list size
                final MALListEncoder listEncoder = encoder.createListEncoder(updateList);
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final SPPEncoder updateEncoder = new SPPEncoder(baos, encoder.getProperties());
                for (Object e : updateList) {
                    encoder.encodeNulltag(e);
                    switch (getElementType(e)) {
                        case NULL:
                            break;
                        case JAVA_MAPPED:
                            e = getUnion(e);
                            // fall through on purpose
                        case ELEMENT:
                            ((Element) e).encode(updateEncoder);
                            listEncoder.encodeUShort(new UShort(baos.size()));
                            encoder.write(baos.toByteArray());
                            baos.reset();
                            break;
                        default:
                            throw new MALException(INVALID_ELEMENT_TYPE);
                    }
                }
                listEncoder.close();
                break;
            default:
                throw new MALException(INVALID_ELEMENT_TYPE);
        }
    }

    private void writeElementPubSubNotifyUpdate(final Object element, final ServiceInfo service) throws MALException {
        // This case can only happen in a broker. We can handle three cases here, though
        // only the first case should actually happen.
        // 1. The updates in the list are already encoded, we just forward them:
        // Body Element [PubSub/Notify/ListOfUpdates] -> (Type info) -> ListLength -> NullTag -> MALEncodedElement
        //                                                                               ^--------------|
        // 2. The whole list is already encoded, we just forward it.
        // 3. The updates are not encoded, i.e. we have to encode them (no idea, when this could
        //    happen).
        switch (getElementType(element)) {
            case NULL:
                break;
            case ENCODED_LIST:
                final MALEncodedElementList encodedList = (MALEncodedElementList) element;
                final Long updateValueShortForm = (Long) encodedList.getShortForm();
                // updateValueShortForm denotes the element type, the corresponding list type is
                // sign flipped. Exchange the last 24 bits of the absolute short form with the sign
                // flipped type.
                final long updateListShortForm = (updateValueShortForm & ~0xFFFFFF) | (-(updateValueShortForm &
                    0xFFFFFF) & 0xFFFFFF);
                if (service.isDeclaredAbstract()) {
                    encodeShortForm(updateListShortForm);
                }
                // listEncoder only needed for implicitly encoding the list size
                final MALListEncoder listEncoder = encoder.createListEncoder(encodedList);
                for (final MALEncodedElement encodedElement : encodedList) {
                    encoder.encodeNulltag(encodedElement);
                    if (encodedElement != null) {
                        encoder.write(encodedElement.getEncodedElement().getValue());
                    }
                }
                listEncoder.close();
                break;
            case ENCODED_ELEMENT:
                encoder.write(((MALEncodedElement) element).getEncodedElement().getValue());
                break;
            case ELEMENT:
                // the element in question is the update list
                if (service.isDeclaredAbstract()) {
                    encodeShortForm(((Element) element).getShortForm());
                }
                encoder.encodeElement((Element) element);
                break;
            default:
                // A java mapped type should not occur, because we expect an update list here.
                throw new MALException(INVALID_ELEMENT_TYPE);
        }
    }

    private void writeElementStandard(Object element, final ServiceInfo service) throws MALException {
        // Only element types need to be handled here. They may be abstract.
        // Concrete: BodyElement [PubSub || IsErrorMessage] -> Element
        // Abstract: BodyElement [PubSub || IsErrorMessage] -> ShortForm -> Element
        switch (getElementType(element)) {
            case JAVA_MAPPED:
                element = getUnion(element);
                // fall through on purpose
            case ELEMENT:
                if (service.isDeclaredAttribute()) {
                    encoder.encodeAttribute((Attribute) element);
                } else {
                    if (service.isDeclaredAbstract()) {
                        encodeShortForm(((Element) element).getShortForm());
                    }
                    encoder.encodeElement((Element) element);
                }
                break;
            default:
                throw new MALException(INVALID_ELEMENT_TYPE);
        }
    }

    private void writeNullableElementStandard(Object element, final ServiceInfo service) throws MALException {
        // Only element types need to be handled here. They may be abstract.
        // Concrete: BodyElement -> NullTag -> Element
        // Abstract: BodyElement -> NullTag -> ShortForm -> Element
        encoder.encodeNulltag(element);
        switch (getElementType(element)) {
            case NULL:
                break;
            case JAVA_MAPPED:
                element = getUnion(element);
                // fall through on purpose
            case ELEMENT:
                if (service.isDeclaredAttribute()) {
                    encoder.encodeAttribute((Attribute) element);
                } else {
                    if (service.isDeclaredAbstract()) {
                        encodeShortForm(((Element) element).getShortForm());
                    }
                    encoder.encodeElement((Element) element);
                }
                break;
            default:
                throw new MALException(INVALID_ELEMENT_TYPE);
        }
    }

    /**
     * Encodes the short form of an element.
     *
     * @param shortForm The short form that will be encoded.
     * @throws MALException
     */
    private void encodeShortForm(final long shortForm) throws MALException {
        for (int i = 7; i >= 0; i--) {
            encoder.write((byte) (shortForm >>> (8 * i)));
        }
    }

    @Override
    public void flush() throws MALException {
        try {
            os.flush();
        } catch (final IOException ex) {
            throw new MALException(ex.getMessage(), ex);
        }
    }

    @Override
    public void close() throws MALException {
        try {
            os.close();
        } catch (final IOException ex) {
            throw new MALException(ex.getMessage(), ex);
        }
    }

    /**
     * Classifies the type of an element.
     *
     * @param element Element to classify.
     * @return NULL if the element is null, ELEMENT if the element is a MAL::Element that has no
     * direct Java mapping type, JAVA_MAPPED if the element is a MAL::Attribute with a direct Java
     * mapping type, ENCODED_LIST if the element is a MALEncodedList, ENCODED_ELEMENT if the element
     * is an MALEncodedElement.
     * @throws MALException
     */
    private static ElementType getElementType(final Object element) throws MALException {
        if (element instanceof Element) {
            return ElementType.ELEMENT;
        } else if (element == null) {
            return ElementType.NULL;
        } else if (element instanceof MALEncodedElementList) {
            return ElementType.ENCODED_LIST;
        } else if (element instanceof MALEncodedElement) {
            return ElementType.ENCODED_ELEMENT;
        } else if (element instanceof Boolean | element instanceof Float | element instanceof Double |
            element instanceof Byte | element instanceof Short | element instanceof Integer | element instanceof Long |
            element instanceof String) {
            return ElementType.JAVA_MAPPED;
        }
        throw new MALException(INVALID_ELEMENT_TYPE);
    }

    /**
     * Constructs a Union object for an element, that is of one of the Java mapping types.
     *
     * @param element Element that is one of the allowed Java mapping types. A MALException is
     * thrown, if it is of a different type.
     * @return Union object that wraps the the element in order to provide an Element interface.
     * @throws MALException
     */
    protected static Union getUnion(final Object element) throws MALException {
        if (element instanceof Boolean) {
            return new Union((Boolean) element);
        } else if (element instanceof Float) {
            return new Union((Float) element);
        } else if (element instanceof Double) {
            return new Union((Double) element);
        } else if (element instanceof Byte) {
            return new Union((Byte) element);
        } else if (element instanceof Short) {
            return new Union((Short) element);
        } else if (element instanceof Integer) {
            return new Union((Integer) element);
        } else if (element instanceof Long) {
            return new Union((Long) element);
        } else if (element instanceof String) {
            return new Union((String) element);
        }
        throw new MALException(INVALID_ELEMENT_TYPE);
    }
}
