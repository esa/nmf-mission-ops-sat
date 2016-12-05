/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO SPP Transport Framework
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
package esa.mo.mal.encoder.spp;

import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALListDecoder;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.EntityKeyList;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.Subscription;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.transport.MALEncodedElement;
import org.ccsds.moims.mo.mal.transport.MALEncodedElementList;

/**
 * Implements the MALElementInputStream interface for a fixed length binary encoding.
 */
public class SPPFixedBinaryElementInputStream extends esa.mo.mal.encoder.binary.fixed.FixedBinaryElementInputStream
{
  /**
   * Constructor.
   *
   * @param is Input stream to read from.
   * @param smallLengthField True if length field is 16bits, otherwise assumed to be 32bits.
   */
  public SPPFixedBinaryElementInputStream(final java.io.InputStream is,
          final boolean smallLengthField,
          final SPPTimeHandler timeHandler)
  {
    super(new SPPFixedBinaryDecoder(is, smallLengthField, timeHandler));
  }

  /**
   * Constructor.
   *
   * @param buf Byte buffer to read from.
   * @param offset Offset into buffer to start from.
   * @param smallLengthField True if length field is 16bits, otherwise assumed to be 32bits.
   */
  public SPPFixedBinaryElementInputStream(final byte[] buf,
          final int offset,
          final boolean smallLengthField,
          final SPPTimeHandler timeHandler)
  {
    super(new SPPFixedBinaryDecoder(buf, offset, smallLengthField, timeHandler));
  }

  @Override
  public Object readElement(final Object element, final MALEncodingContext ctx)
          throws IllegalArgumentException, MALException
  {
    if ((element != ctx.getHeader())
            && (!ctx.getHeader().getIsErrorMessage())
            && (InteractionType._PUBSUB_INDEX == ctx.getHeader().getInteractionType().getOrdinal()))
    {
      switch (ctx.getHeader().getInteractionStage().getValue())
      {
        case MALPubSubOperation._REGISTER_STAGE:
          return dec.decodeElement(new Subscription());
        case MALPubSubOperation._PUBLISH_REGISTER_STAGE:
          return dec.decodeElement(new EntityKeyList());
        case MALPubSubOperation._DEREGISTER_STAGE:
          return dec.decodeElement(new IdentifierList());
        case MALPubSubOperation._PUBLISH_STAGE:
        {
          int idx = ctx.getBodyElementIndex();
          if (0 == idx)
          {
            return dec.decodeElement(new UpdateHeaderList());
          }
          else
          {
            Object sf = ctx.getOperation().getOperationStage(ctx.getHeader().getInteractionStage()).getElementShortForms()[ctx.getBodyElementIndex()];
            return decodePubSubPublishUpdate((Long) sf);
          }
        }
        case MALPubSubOperation._NOTIFY_STAGE:
        {
          int idx = ctx.getBodyElementIndex();
          if (0 == idx)
          {
            return dec.decodeIdentifier();
          }
          else if (1 == idx)
          {
            return dec.decodeElement(new UpdateHeaderList());
          }
          else
          {
            Object sf = ctx.getOperation().getOperationStage(ctx.getHeader().getInteractionStage()).getElementShortForms()[ctx.getBodyElementIndex()];
            if (null == sf)
            {
              sf = dec.decodeAbstractElementType(true);
            }
            return decodeSubElement((Long) sf, ctx);
          }
        }
        default:
          return decodeSubElement(dec.decodeAbstractElementType(true), ctx);
      }
    }

    return super.readElement(element, ctx);
  }

  private Object decodePubSubPublishUpdate(Long sf) throws MALException
  {
    if (null == sf)
    {
      sf = dec.decodeAbstractElementType(false);
    }

    final Long sfv = (sf & ~0xFFFFFF) | (-(sf & 0xFFFFFF) & 0xFFFFFF);

    MALEncodedElementList encElemList = new MALEncodedElementList(sfv, 0);
    MALListDecoder listDecoder = dec.createListDecoder(encElemList);

    while (listDecoder.hasNext())
    {
      Blob b = listDecoder.decodeNullableBlob();
      encElemList.add((b == null) ? null : new MALEncodedElement(b));
    }

    return encElemList;
  }
}
