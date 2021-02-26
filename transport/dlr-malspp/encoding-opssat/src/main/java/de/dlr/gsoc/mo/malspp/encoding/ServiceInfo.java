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

import java.util.Arrays;
import java.util.Collection;
import org.ccsds.moims.mo.mal.MALOperationStage;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;

/**
 * Helper class for extracting information from the service.
 */
class ServiceInfo {

	private final InteractionType interaction;
	private final UOctet stage;
	private final int bodyElementIndex;
	private final Long shortForm;
	private final Boolean isError;
	private final Boolean isDeclaredAbstract;
	private Boolean isDeclaredAttribute;
	private static final Collection<Long> attributeShortForms = Arrays.asList(Attribute.BOOLEAN_SHORT_FORM,
			Attribute.FLOAT_SHORT_FORM,
			Attribute.DOUBLE_SHORT_FORM,
			Attribute.OCTET_SHORT_FORM,
			Attribute.SHORT_SHORT_FORM,
			Attribute.INTEGER_SHORT_FORM,
			Attribute.LONG_SHORT_FORM,
			Attribute.STRING_SHORT_FORM,
			Attribute.BLOB_SHORT_FORM,
			Attribute.DURATION_SHORT_FORM,
			Attribute.IDENTIFIER_SHORT_FORM,
			Attribute.TIME_SHORT_FORM,
			Attribute.FINETIME_SHORT_FORM,
			Attribute.UINTEGER_SHORT_FORM,
			Attribute.ULONG_SHORT_FORM,
			Attribute.UOCTET_SHORT_FORM,
			Attribute.USHORT_SHORT_FORM,
			Attribute.URI_SHORT_FORM);

	/**
	 * Retrieve service information by evaluating the encoding context.
	 *
	 * @param ctx The encoding context, which holds all relevant service information.
	 */
	protected ServiceInfo(final MALEncodingContext ctx) {
		// How to find out, if we are about to encode an element declared abstract by the service:
		// 1. Check, if we are at the last body element, because only the last one is allowed to be
		//    declared abstract.
		// 2. Get a list of short forms from the current operation and interaction stage declaring
		//    the types of the body elements. Short forms of error messages are specified here
		//    explicitly because operation and interaction stage are equal to the non-error case
		//    and thus the list of short forms would be the one for the non-error case.
		// 3. If the last short form in the list is null, then this is element was declared
		//    abstract. In this case we need to encode the absolute short form in the output stream.
		final MALMessageHeader header = ctx.getHeader();
		stage = header.getInteractionStage();
		interaction = header.getInteractionType();
		isError = header.getIsErrorMessage();
		final MALOperationStage operationStage = ctx.getOperation().getOperationStage(stage);
		bodyElementIndex = ctx.getBodyElementIndex();
		if (isError) {
			shortForm = bodyElementIndex == 0 ? Attribute.UINTEGER_SHORT_FORM : null;
		} else {
			shortForm = (Long) operationStage.getElementShortForms()[bodyElementIndex];
		}
		isDeclaredAbstract = shortForm == null;
		isDeclaredAttribute = false;
		if (isDeclaredAbstract && !isError) {
			// Declared type is abstract. Now check, if an Attribute was declared or some other
			// abstract type (like Element, Composite or an abstract composite).
			final Object[] allowedShortForms = operationStage.getLastElementShortForms();
			if (onlyAttributeTypesAllowed(allowedShortForms)) {
				isDeclaredAttribute = true;
			}
		}
	}

	/**
	 * Check, if all element short forms are exactly the attribute short forms.
	 *
	 * @param allowedShortForms Array of short forms to be checked.
	 * @return (True, if all elements in the array are attribute short forms, false if not.)
	 */
	private static boolean onlyAttributeTypesAllowed(final Object[] allowedShortForms) {
		return attributeShortForms.size() == allowedShortForms.length
				&& attributeShortForms.containsAll(Arrays.asList(allowedShortForms));
	}

	public InteractionType getInteraction() {
		return interaction;
	}

	public UOctet getStage() {
		return stage;
	}

	public int getBodyElementIndex() {
		return bodyElementIndex;
	}

	public Long getShortForm() {
		return shortForm;
	}

	public boolean isErrorMessage() {
		return isError;
	}

	/**
	 * Find out, if the declared type of the current element is abstract.
	 *
	 * @return True, if the declared type is abstract, false if it is concrete.
	 */
	public boolean isDeclaredAbstract() {
		return isDeclaredAbstract;
	}

	/**
	 * Find out, if the declared type of the current element is Attribute. This also means, that the
	 * declared type of the element is abstract.
	 *
	 * @return True, if the declared type is Attribute, false if not.
	 */
	public boolean isDeclaredAttribute() {
		return isDeclaredAttribute;
	}
}
