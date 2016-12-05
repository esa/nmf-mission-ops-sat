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

import java.io.IOException;
import java.io.OutputStream;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Element;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.UOctet;

/**
 * Extends the MALElementOutputStream interface to enable aware transport access to the encoded data stream.
 */
public abstract class GENElementOutputStream implements MALElementOutputStream
{
  protected final OutputStream dos;
  protected GENEncoder enc = null;

  /**
   * Constructor.
   *
   * @param os Output stream to write to.
   */
  protected GENElementOutputStream(final OutputStream os)
  {
    this.dos = os;
  }

  @Override
  public void writeElement(final Object element, final MALEncodingContext ctx) throws MALException
  {
    if (null == enc)
    {
      this.enc = createEncoder(dos);
    }

    if (element == ctx.getHeader())
    {
      ((Element) element).encode(enc);
    }
    else
    {
      if (null == element)
      {
        enc.encodeNullableElement(null);
      }
      else
      {
        if (ctx.getHeader().getIsErrorMessage())
        {
          // error messages have a standard format
          if (0 == ctx.getBodyElementIndex())
          {
            ((Element) element).encode(enc);
          }
          else
          {
            encodeSubElement((Element) element, null, null);
          }
        }
        else if (InteractionType._PUBSUB_INDEX == ctx.getHeader().getInteractionType().getOrdinal())
        {
          switch (ctx.getHeader().getInteractionStage().getValue())
          {
            case MALPubSubOperation._REGISTER_STAGE:
            case MALPubSubOperation._PUBLISH_REGISTER_STAGE:
            case MALPubSubOperation._DEREGISTER_STAGE:
              ((Element) element).encode(enc);
              return;
            case MALPubSubOperation._PUBLISH_STAGE:
              if ((0 < ctx.getBodyElementIndex()) && (null == ctx.getOperation().getOperationStage(ctx.getHeader().getInteractionStage()).getElementShortForms()[ctx.getBodyElementIndex()]))
              {
                encodeSubElement((Element) element, null, null);
              }
              else
              {
                ((Element) element).encode(enc);
              }
              return;
            case MALPubSubOperation._NOTIFY_STAGE:
              if ((1 < ctx.getBodyElementIndex()) && (null == ctx.getOperation().getOperationStage(ctx.getHeader().getInteractionStage()).getElementShortForms()[ctx.getBodyElementIndex()]))
              {
                encodeSubElement((Element) element, null, null);
              }
              else
              {
                ((Element) element).encode(enc);
              }
              return;
            default:
              encodeSubElement((Element) element, null, null);
          }
        }
        else
        {
          if (element instanceof Element)
          {
            // encode the short form if it is not fixed in the operation
            final Element e = (Element) element;

            UOctet stage = ctx.getHeader().getInteractionStage();
            Object sf = ctx.getOperation().getOperationStage(stage).getElementShortForms()[ctx.getBodyElementIndex()];

            encodeSubElement(e, sf, ctx);
          }
        }
      }
    }
  }

  @Override
  public void flush() throws MALException
  {
    try
    {
      dos.flush();
    }
    catch (IOException ex)
    {
      throw new MALException("IO exception flushing Element stream", ex);
    }
  }

  @Override
  public void close() throws MALException
  {
    try
    {
      dos.close();
      if (null != enc)
      {
        enc.close();
      }
    }
    catch (IOException ex)
    {
      throw new MALException(ex.getLocalizedMessage(), ex);
    }
  }

  protected void encodeSubElement(final Element e, final Object sf, final MALEncodingContext ctx) throws MALException
  {
    if (null == sf)
    {
      // dirty check to see if we are trying to decode an abstract Attribute (and not a list of them either)
      Object[] finalEleShortForms = null;
      if (null != ctx)
      {
        finalEleShortForms = ctx.getOperation().getOperationStage(ctx.getHeader().getInteractionStage()).getLastElementShortForms();
      }

      if ((null != finalEleShortForms) && (Attribute._URI_TYPE_SHORT_FORM == finalEleShortForms.length) && ((((Long) finalEleShortForms[0]) & 0x800000L) == 0))
      {
        enc.encodeNullableOctet(enc.internalEncodeAttributeType(e.getTypeShortForm().byteValue()));
      }
      else
      {
        enc.encodeAbstractElementType(e.getShortForm(), true);
      }

      // now encode the element
      enc.encodeElement(e);
    }
    else
    {
      enc.encodeNullableElement(e);
    }
  }

  /**
   * Over-ridable factory function.
   *
   * @param os Output stream to wrap.
   * @return the new encoder.
   */
  protected abstract GENEncoder createEncoder(OutputStream os);
}
