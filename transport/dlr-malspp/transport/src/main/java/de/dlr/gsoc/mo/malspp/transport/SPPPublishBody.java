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

import java.util.List;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.transport.MALEncodedBody;
import org.ccsds.moims.mo.mal.transport.MALEncodedElement;
import org.ccsds.moims.mo.mal.transport.MALPublishBody;

public class SPPPublishBody extends SPPMessageBody implements MALPublishBody {

	protected int idx;

	public SPPPublishBody(final Object[] bodyElements, final MALElementStreamFactory esf, final MALEncodingContext ctx) {
		super(bodyElements, esf, ctx);
		idx = 0;
	}

	public SPPPublishBody(final MALEncodedBody encodedBody, final MALElementStreamFactory esf, final MALEncodingContext ctx) {
		super(encodedBody, esf, ctx);
		idx = 0;
	}

	@Override
	public UpdateHeaderList getUpdateHeaderList() throws MALException {
		return (UpdateHeaderList) getBodyElement(idx, new UpdateHeaderList());
	}

	@Override
	public List[] getUpdateLists(final List... updateLists) throws MALException {
		List[] lists = new List[getElementCount() - 1 - idx]; // Subtract 1 for UpdateHeaderList; subtract idx for Identifier in SPPNotifyBody.
		for (int i = 0; i < lists.length; i++) {
			lists[i] = getUpdateList(i, updateLists == null ? null : updateLists[i]);
		}
		return lists;
	}

	@Override
	public List getUpdateList(final int listIndex, final List updateList) throws MALException {
		// idx is the body element index of the UpdateHeaderList,
		// idx + 1 is the body element index of the first update value type list.
		return (List) getBodyElement(idx + 1 + listIndex, updateList);
	}

	@Override
	public int getUpdateCount() throws MALException {
		return getUpdateHeaderList().size();
	}

	@Override
	public Object getUpdate(final int listIndex, final int updateIndex) throws MALException {
		try {
			return getUpdateList(listIndex, null).get(updateIndex);
		} catch (IndexOutOfBoundsException ex) {
			throw new MALException(ex.getMessage(), ex);
		}
	}

	@Override
	public MALEncodedElement getEncodedUpdate(final int listIndex, final int updateIndex) throws MALException {
		if (ctx != null
				&& ctx.getHeader().getInteractionType().equals(InteractionType.PUBSUB)
				&& ctx.getHeader().getInteractionStage().equals(MALPubSubOperation.PUBLISH_STAGE)) {
			return (MALEncodedElement) getUpdate(listIndex, updateIndex);
		}
		throw new MALException(NOT_SUPPORTED);
	}
}
