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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.MALRequestOperation;
import org.ccsds.moims.mo.mal.MALSubmitOperation;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.BlobList;
import org.ccsds.moims.mo.mal.structures.EntityRequest;
import org.ccsds.moims.mo.mal.structures.File;
import org.ccsds.moims.mo.mal.structures.IdBooleanPair;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.NamedValue;
import org.ccsds.moims.mo.mal.structures.OctetList;
import org.ccsds.moims.mo.mal.structures.Pair;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.UOctetList;
import org.ccsds.moims.mo.mal.structures.Union;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.transport.MALEncodedElement;
import org.ccsds.moims.mo.mal.transport.MALEncodedElementList;
import org.junit.Test;
import static org.mockito.Mockito.*;

public abstract class SPPElementIOStreamTestHelper {

	protected static Long[] ATTRIBUTES = new Long[]{
		Attribute.BOOLEAN_SHORT_FORM,
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
		Attribute.URI_SHORT_FORM
	};

	protected enum TestContext {

		PUBSUB_PUBLISH_UPDATE, PUBSUB_NOTIFY_UPDATE, PUBSUB, ERROR_NUMBER, ERROR_EXTRA_INFORMATION, STANDARD, NULL
	}

	protected enum TestDeclaredType {

		CONCRETE, ABSTRACT_ATTRIBUTE, ABSTRACT_ELEMENT
	}

	protected enum TestActualType {

		NULL, JAVA_ATTRIBUTE, MAL_ATTRIBUTE, MAL_ELEMENT, MAL_ENCODED_ELEMENT, MAL_ENCODED_ELEMENT_LIST, UPDATE_LIST, IDENTIFIER_LIST, ERROR_NUMBER
	}

	protected static class TestData {

		Object element;
		MALEncodingContext ctx;

		public TestData(Object element, MALEncodingContext ctx) {
			this.element = element;
			this.ctx = ctx;
		}
	}

	protected final static Map<String, Object> fixedQosProperties = new HashMap<>();

	static {
		fixedQosProperties.put("de.dlr.gsoc.mo.malspp.VARINT_SUPPORTED", "true");
	}

	protected abstract void performTest(TestContext testContext, TestActualType actualType, TestDeclaredType declaredType, byte[] buffer) throws Exception;

	protected static TestData constructTest(TestContext testContext, TestActualType actualType, TestDeclaredType declaredType) {
		ArrayList<Long> shortForms = new ArrayList<>();
		Long[] allowedShortForms = new Long[]{};
		Object value = null;
		switch (actualType) {
			case NULL:
				shortForms.add(IdentifierList.SHORT_FORM);
				allowedShortForms = new Long[]{IdentifierList.SHORT_FORM, Attribute.SHORT_SHORT_FORM, Attribute.ULONG_SHORT_FORM};
				value = null;
				break;
			case JAVA_ATTRIBUTE:
				shortForms.add(Union.INTEGER_SHORT_FORM);
				// PENDING: Unclear from MAL Java API Book when Java mapping types are used and when Union
				// types are used. Here: Do not unmap Union types to Java types (in order for the MAL tests
				// to succeed).
				value = new Union(new Integer(-42));
				allowedShortForms = new Long[]{Attribute.INTEGER_SHORT_FORM, Attribute.URI_SHORT_FORM, EntityRequest.SHORT_FORM};
				break;
			case MAL_ATTRIBUTE:
				shortForms.add(UOctet.UOCTET_SHORT_FORM);
				value = new UOctet((short) 210);
				allowedShortForms = new Long[]{Attribute.INTEGER_SHORT_FORM, Attribute.UOCTET_SHORT_FORM, File.SHORT_FORM};
				break;
			case MAL_ELEMENT:
				shortForms.add(IdBooleanPair.SHORT_FORM);
				value = new IdBooleanPair(new Identifier("DLR"), Boolean.TRUE);
				allowedShortForms = new Long[]{IdBooleanPair.SHORT_FORM, Pair.SHORT_FORM, NamedValue.SHORT_FORM};
				break;
			case MAL_ENCODED_ELEMENT:
				shortForms.add(OctetList.SHORT_FORM);
				value = new MALEncodedElement(new Blob(new byte[]{4, 1, 8, 1, 13, 0, 1, 21}));
				break;
			case MAL_ENCODED_ELEMENT_LIST:
				shortForms.add(UOctetList.SHORT_FORM);
				MALEncodedElementList encElemList = new MALEncodedElementList(UOctet.UOCTET_SHORT_FORM, 4);
				// null, UOctet(11), UOctet(13), UOctet(17)
				encElemList.add(null);
				encElemList.add(new MALEncodedElement(new Blob(new byte[]{11})));
				encElemList.add(new MALEncodedElement(new Blob(new byte[]{13})));
				encElemList.add(new MALEncodedElement(new Blob(new byte[]{17})));
				value = encElemList;
				break;
			case UPDATE_LIST:
				shortForms.add(BlobList.SHORT_FORM);
				BlobList blobList = new BlobList(3);
				// BlobList([0xAB, 0xCD], null, [0xEF])
				blobList.add(new Blob(new byte[]{(byte) 0xAB, (byte) 0xCD}));
				blobList.add(null);
				blobList.add(new Blob(new byte[]{(byte) 0xEF}));
				value = blobList;
				break;
			case IDENTIFIER_LIST:
				shortForms.add(IdentifierList.SHORT_FORM);
				IdentifierList idList = new IdentifierList(2);
				// IdentifierList ("a", "B")
				idList.add(new Identifier("a"));
				idList.add(new Identifier("B"));
				value = idList;
				break;
			case ERROR_NUMBER:
				shortForms.add(UInteger.UINTEGER_SHORT_FORM);
				value = new UInteger(65536);
		}

		switch (declaredType) {
			case CONCRETE:
				allowedShortForms = shortForms.toArray(new Long[]{});
				break;
			case ABSTRACT_ATTRIBUTE:
				shortForms.set(shortForms.size() - 1, null);
				allowedShortForms = ATTRIBUTES;
				break;
			case ABSTRACT_ELEMENT:
				shortForms.set(shortForms.size() - 1, null);
				break;
		}

		MALEncodingContext ctx = null;
		switch (testContext) {
			case PUBSUB_PUBLISH_UPDATE:
				shortForms.add(0, UpdateHeaderList.SHORT_FORM);
				ctx = prepareMockCtx(InteractionType.PUBSUB, MALPubSubOperation._PUBLISH_STAGE, 1, shortForms.toArray(new Long[]{}), allowedShortForms, false);
				break;
			case PUBSUB_NOTIFY_UPDATE:
				shortForms.add(0, UpdateHeaderList.SHORT_FORM);
				shortForms.add(1, Identifier.IDENTIFIER_SHORT_FORM);
				ctx = prepareMockCtx(InteractionType.PUBSUB, MALPubSubOperation._NOTIFY_STAGE, 2, shortForms.toArray(new Long[]{}), allowedShortForms, false);
				break;
			case PUBSUB:
				ctx = prepareMockCtx(InteractionType.PUBSUB, MALPubSubOperation._DEREGISTER_STAGE, 0, shortForms.toArray(new Long[]{}), allowedShortForms, false);
				break;
			case ERROR_NUMBER:
				shortForms = new ArrayList<>();
				allowedShortForms = new Long[]{};
				ctx = prepareMockCtx(InteractionType.SUBMIT, MALSubmitOperation._SUBMIT_ACK_STAGE, 0, shortForms.toArray(new Long[]{}), allowedShortForms, true);
				break;
			case ERROR_EXTRA_INFORMATION:
				shortForms = new ArrayList<>();
				allowedShortForms = new Long[]{};
				ctx = prepareMockCtx(InteractionType.SUBMIT, MALSubmitOperation._SUBMIT_ACK_STAGE, 1, shortForms.toArray(new Long[]{}), allowedShortForms, true);
				break;
			case STANDARD:
				ctx = prepareMockCtx(InteractionType.REQUEST, MALRequestOperation._REQUEST_RESPONSE_STAGE, 0, shortForms.toArray(new Long[]{}), allowedShortForms, false);
				break;
			case NULL:
				break;
		}
		return new TestData(value, ctx);
	}

	protected static MALEncodingContext prepareMockCtx(InteractionType interactionType, int stage, int bodyElementIndex, Long[] shortForms, Long[] allowedAbstractShortForms, boolean isError) {
		MALEncodingContext ctx = mock(MALEncodingContext.class, RETURNS_DEEP_STUBS);
		UOctet st = new UOctet((short) stage);
		when(ctx.getHeader().getInteractionStage()).thenReturn(st);
		when(ctx.getHeader().getInteractionType()).thenReturn(interactionType);
		when(ctx.getHeader().getIsErrorMessage()).thenReturn(isError);
		when(ctx.getOperation().getOperationStage(st).getElementShortForms()).thenReturn(shortForms);
		when(ctx.getOperation().getOperationStage(st).getLastElementShortForms()).thenReturn(allowedAbstractShortForms);
		when(ctx.getBodyElementIndex()).thenReturn(bodyElementIndex);
		return ctx;
	}

	@Test
	public void testElementPubSubNotify1() throws Exception {
		performTest(TestContext.PUBSUB_NOTIFY_UPDATE, TestActualType.UPDATE_LIST, TestDeclaredType.CONCRETE, new byte[]{
			3, 1, 2, (byte) 0xAB, (byte) 0xCD, 0, 1, 1, (byte) 0xEF});
	}

	@Test
	public void testElementPubSub() throws Exception {
		performTest(TestContext.PUBSUB, TestActualType.IDENTIFIER_LIST, TestDeclaredType.CONCRETE, new byte[]{
			2, 1, 1, 0x61, 1, 1, 0x42});
	}

	@Test
	public void testElementStandard1() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.JAVA_ATTRIBUTE, TestDeclaredType.CONCRETE, new byte[]{
			1, 83});
	}

	@Test
	public void testElementStandard2() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.JAVA_ATTRIBUTE, TestDeclaredType.ABSTRACT_ATTRIBUTE, new byte[]{
			1, 10, 83});
	}

	@Test
	public void testElementStandard3() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.JAVA_ATTRIBUTE, TestDeclaredType.ABSTRACT_ELEMENT, new byte[]{
			1, 0, 1, 0, 0, 1, 0, 0, 11, 83});
	}

	@Test
	public void testElementStandard4() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.MAL_ATTRIBUTE, TestDeclaredType.CONCRETE, new byte[]{
			1, (byte) 210});
	}

	@Test
	public void testElementStandard5() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.MAL_ATTRIBUTE, TestDeclaredType.ABSTRACT_ATTRIBUTE, new byte[]{
			1, 7, (byte) 210});
	}

	@Test
	public void testElementStandard6() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.MAL_ATTRIBUTE, TestDeclaredType.ABSTRACT_ELEMENT, new byte[]{
			1, 0, 1, 0, 0, 1, 0, 0, 8, (byte) 210});
	}

	@Test
	public void testElementStandard7() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.MAL_ELEMENT, TestDeclaredType.CONCRETE, new byte[]{
			1, 1, 3, 0x44, 0x4C, 0x52, 1, 1});
	}

	@Test
	public void testElementStandard8() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.MAL_ELEMENT, TestDeclaredType.ABSTRACT_ELEMENT, new byte[]{
			1, 0, 1, 0, 0, 1, 0, 0, 27, 1, 3, 0x44, 0x4C, 0x52, 1, 1});
	}

	@Test
	public void testElementStandard9() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.NULL, TestDeclaredType.CONCRETE, new byte[]{
			0});
	}

	@Test
	public void testElementStandard10() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.NULL, TestDeclaredType.ABSTRACT_ATTRIBUTE, new byte[]{
			0});
	}

	@Test
	public void testElementStandard11() throws Exception {
		performTest(TestContext.STANDARD, TestActualType.NULL, TestDeclaredType.ABSTRACT_ELEMENT, new byte[]{
			0});
	}

	@Test
	public void testElementError1() throws Exception {
		performTest(TestContext.ERROR_NUMBER, TestActualType.ERROR_NUMBER, TestDeclaredType.CONCRETE, new byte[]{
			(byte) 0x80, (byte) 0x80, 0x04});
	}

	@Test
	public void testElementError2() throws Exception {
		performTest(TestContext.ERROR_EXTRA_INFORMATION, TestActualType.JAVA_ATTRIBUTE, TestDeclaredType.ABSTRACT_ELEMENT, new byte[]{
			1, 0, 1, 0, 0, 1, 0, 0, 11, 83});
	}

	@Test
	public void testElementError3() throws Exception {
		performTest(TestContext.ERROR_EXTRA_INFORMATION, TestActualType.MAL_ATTRIBUTE, TestDeclaredType.ABSTRACT_ELEMENT, new byte[]{
			1, 0, 1, 0, 0, 1, 0, 0, 8, (byte) 210});
	}

	@Test
	public void testElementError4() throws Exception {
		performTest(TestContext.ERROR_EXTRA_INFORMATION, TestActualType.MAL_ELEMENT, TestDeclaredType.ABSTRACT_ELEMENT, new byte[]{
			1, 0, 1, 0, 0, 1, 0, 0, 27, 1, 3, 0x44, 0x4C, 0x52, 1, 1});
	}

	@Test
	public void testElementError5() throws Exception {
		performTest(TestContext.ERROR_EXTRA_INFORMATION, TestActualType.NULL, TestDeclaredType.ABSTRACT_ELEMENT, new byte[]{
			0});
	}

	@Test
	public void testElementNullContext1() throws Exception {
		performTest(TestContext.NULL, TestActualType.JAVA_ATTRIBUTE, TestDeclaredType.CONCRETE, new byte[]{
			83});
	}

	@Test
	public void testElementNullContext2() throws Exception {
		performTest(TestContext.NULL, TestActualType.MAL_ATTRIBUTE, TestDeclaredType.CONCRETE, new byte[]{
			(byte) 210});
	}

	@Test
	public void testElementNullContext3() throws Exception {
		performTest(TestContext.NULL, TestActualType.MAL_ELEMENT, TestDeclaredType.CONCRETE, new byte[]{
			1, 3, 0x44, 0x4C, 0x52, 1, 1});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testElementNullContext4() throws Exception {
		performTest(TestContext.NULL, TestActualType.NULL, TestDeclaredType.CONCRETE, new byte[]{
			0});
	}
}
