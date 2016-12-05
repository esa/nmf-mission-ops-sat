/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO Generic Transport Framework
 * ----------------------------------------------------------------------------
 * Licensed under the European Space Agency Public License, Version 2.0
 * You may not use this file except in compliance with the License.
 *
 * Except as expressly set forth in this License, the Software is provided to
 * You on an "as is" basis and without warranties of any kind, including without
 * limitation merchantability, fitness for a particular purpose, absence of
 * defects or errors, accuracy or non-infringement of intellectual property rights.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * ----------------------------------------------------------------------------
 */
package esa.mo.mal.transport.gen.body;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.transport.MALEncodedElement;
import org.ccsds.moims.mo.mal.transport.MALPublishBody;

/**
 * Implementation of the MALPublishBody interface.
 */
public class GENPublishBody extends GENMessageBody implements MALPublishBody
{
  private static final long serialVersionUID = 222222222222227L;
  private final int offset;
  private UpdateHeaderList hdrList = null;

  /**
   * Constructor.
   *
   * @param ctx The encoding context to use.
   * @param encFactory The encoder stream factory to use.
   * @param messageParts The message parts that compose the body.
   */
  public GENPublishBody(final MALEncodingContext ctx,
          final MALElementStreamFactory encFactory, 
          final Object[] messageParts)
  {
    super(ctx, encFactory, messageParts);
    offset = 0;
  }

  /**
   * Constructor.
   *
   * @param ctx The encoding context to use.
   * @param encFactory The encoder stream factory to use.
   * @param messageParts The message parts that compose the body.
   * @param offset The offset in the message parts where the updates start.
   */
  public GENPublishBody(final MALEncodingContext ctx,
          final MALElementStreamFactory encFactory, 
          final Object[] messageParts, final int offset)
  {
    super(ctx, encFactory, messageParts);
    this.offset = offset;
  }

  /**
   * Constructor.
   *
   * @param ctx The encoding context to use.
   * @param wrappedBodyParts True if the encoded body parts are wrapped in BLOBs.
   * @param encFactory The encoder stream factory to use.
   * @param encBodyElements The input stream that holds the encoded body parts.
   */
  public GENPublishBody(final MALEncodingContext ctx, 
          final boolean wrappedBodyParts,
          final MALElementStreamFactory encFactory,
          final ByteArrayInputStream encBodyBytes,
          final MALElementInputStream encBodyElements)
  {
    super(ctx, wrappedBodyParts, encFactory, encBodyBytes, encBodyElements);
    offset = 0;
  }

  /**
   * Constructor.
   *
   * @param ctx The encoding context to use.
   * @param wrappedBodyParts True if the encoded body parts are wrapped in BLOBs.
   * @param encFactory The encoder stream factory to use.
   * @param encBodyElements The input stream that holds the encoded body parts.
   * @param offset The offset in the message parts where the updates start.
   */
  public GENPublishBody(final MALEncodingContext ctx, 
          final boolean wrappedBodyParts,
          final MALElementStreamFactory encFactory,
          final ByteArrayInputStream encBodyBytes,
          final MALElementInputStream encBodyElements,
          final int offset)
  {
    super(ctx, wrappedBodyParts, encFactory, encBodyBytes, encBodyElements);
    this.offset = offset;
  }

  @Override
  public int getUpdateCount() throws MALException
  {
    if (null == hdrList)
    {
      getUpdateHeaderList();
    }

    return hdrList.size();
  }

  @Override
  public UpdateHeaderList getUpdateHeaderList() throws MALException
  {
    hdrList = (UpdateHeaderList) getBodyElement(offset, new UpdateHeaderList());
    return hdrList;
  }

  @Override
  public List getUpdateList(final int listIndex, final List updateList) throws MALException
  {
    return (List) getBodyElement(offset + listIndex + 1, updateList);
  }

  @Override
  public List[] getUpdateLists(final List... updateLists) throws MALException
  {
    decodeMessageBody();

    final List[] rv = new List[messageParts.length - offset - 1];

    for (int i = 0; i < rv.length; i++)
    {
      rv[i] = (List) messageParts[i + offset + 1];
    }

    return rv;
  }

  @Override
  public Object getUpdate(final int listIndex, final int updateIndex) throws MALException
  {
    decodeMessageBody();

    return ((List) (messageParts[offset + 1 + listIndex])).get(updateIndex);
  }

  @Override
  public MALEncodedElement getEncodedUpdate(final int listIndex, final int updateIndex) throws MALException
  {
    //ToDo
    throw new MALException("Not supported yet.");
  }
}
