/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO Generic Encoder Framework
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
package esa.mo.mal.encoder.gen;

import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALElementFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Element;
import org.ccsds.moims.mo.mal.structures.EntityKeyList;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.Subscription;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;

/**
 * Extends the MALElementInputStream interface to enable aware transport access to the encoded data stream.
 */
public abstract class GENElementInputStream implements MALElementInputStream
{
  protected final GENDecoder dec;

  /**
   * Sub class constructor.
   *
   * @param pdec Decoder to use.
   */
  protected GENElementInputStream(GENDecoder pdec)
  {
    dec = pdec;
  }

  @Override
  public Object readElement(final Object element, final MALEncodingContext ctx)
          throws IllegalArgumentException, MALException
  {
    if (element == ctx.getHeader())
    {
      return dec.decodeElement((Element) element);
    }
    else
    {
      if (ctx.getHeader().getIsErrorMessage())
      {
        // error messages have a standard format
        if (0 == ctx.getBodyElementIndex())
        {
          return dec.decodeUInteger();
        }
        else
        {
          return decodeSubElement(dec.decodeAbstractElementType(true), ctx);
        }
      }
      else if (InteractionType._PUBSUB_INDEX == ctx.getHeader().getInteractionType().getOrdinal())
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

              // element is defined as an abstract type
              if (null == sf)
              {
                sf = dec.decodeAbstractElementType(true);
              }
              
              return decodeSubElement((Long) sf, ctx);
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
              
              // element is defined as an abstract type
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
      else
      {
        if (null == element)
        {
          Long shortForm;

          // dirty check to see if we are trying to decode an abstract Attribute (and not a list of them either)
          Object[] finalEleShortForms = ctx.getOperation().getOperationStage(ctx.getHeader().getInteractionStage()).getLastElementShortForms();

          if ((null != finalEleShortForms) && (Attribute._URI_TYPE_SHORT_FORM == finalEleShortForms.length) && ((((Long) finalEleShortForms[0]) & 0x800000L) == 0))
          {
            Byte sf = dec.decodeNullableOctet();
            if (null == sf)
            {
              return null;
            }

            shortForm = Attribute.ABSOLUTE_AREA_SERVICE_NUMBER + dec.internalDecodeAttributeType(sf);
          }
          else
          {
            shortForm = dec.decodeAbstractElementType(true);
          }

          return decodeSubElement(shortForm, ctx);
        }
        else
        {
          return dec.decodeNullableElement((Element) element);
        }
      }
    }
  }

  /**
   * Returns a new byte array containing the remaining encoded data for this stream. Expected to be used for creating an
   * MAL encoded body object.
   *
   * @return a byte array containing the remaining encoded data for this stream.
   * @throws MALException On error.
   */
  public byte[] getRemainingEncodedData() throws MALException
  {
    return dec.getRemainingEncodedData();
  }

  @Override
  public void close() throws MALException
  {
    // Nothing to do for this decoder
  }

  protected Object decodeSubElement(final Long shortForm, final MALEncodingContext ctx) throws MALException
  {
    if (null == shortForm)
    {
      return null;
    }

    final MALElementFactory ef = MALContextFactory.getElementFactoryRegistry().lookupElementFactory(shortForm);

    if (null == ef)
    {
      throw new MALException("GEN transport unable to find element factory for short type: " + shortForm);
    }

    return dec.decodeElement((Element) ef.createElement());
  }
}
