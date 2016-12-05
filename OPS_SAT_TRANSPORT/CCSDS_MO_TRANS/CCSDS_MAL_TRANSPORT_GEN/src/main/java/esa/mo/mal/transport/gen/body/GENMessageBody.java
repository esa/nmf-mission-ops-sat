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

import esa.mo.mal.encoder.gen.GENElementInputStream;
import esa.mo.mal.transport.gen.GENTransport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.ccsds.moims.mo.mal.MALArea;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALElementFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALOperation;
import org.ccsds.moims.mo.mal.MALService;
import org.ccsds.moims.mo.mal.encoding.MALElementInputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementOutputStream;
import org.ccsds.moims.mo.mal.encoding.MALElementStreamFactory;
import org.ccsds.moims.mo.mal.encoding.MALEncodingContext;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Element;
import org.ccsds.moims.mo.mal.structures.ElementList;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.Union;
import org.ccsds.moims.mo.mal.transport.MALEncodedBody;
import org.ccsds.moims.mo.mal.transport.MALEncodedElement;
import org.ccsds.moims.mo.mal.transport.MALEncodedElementList;
import org.ccsds.moims.mo.mal.transport.MALMessageBody;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;

/**
 * Implementation of the MALMessageBody interface.
 */
public class GENMessageBody implements MALMessageBody, java.io.Serializable
{
  /**
   * Factory used to create encoders/decoders.
   */
  protected MALElementStreamFactory encFactory;
  /**
   * Input stream that holds the encoded message body parts.
   */
  protected ByteArrayInputStream encBodyBytes;
  /**
   * Input stream that holds the encoded message body parts.
   */
  protected MALElementInputStream encBodyElements;
  /**
   * True if we have already decoded the body.
   */
  protected boolean decodedBody = false;
  protected final MALEncodingContext ctx;
  /**
   * Number of body parts.
   */
  protected int bodyPartCount;
  /**
   * The decoded body parts.
   */
  protected Object[] messageParts;
  private final boolean wrappedBodyParts;
  private static final long serialVersionUID = 222222222222223L;

  /**
   * Constructor.
   *
   * @param ctx The encoding context to use.
   * @param encFactory The encoder stream factory to use.
   * @param messageParts The message body parts.
   */
  public GENMessageBody(final MALEncodingContext ctx,
          final MALElementStreamFactory encFactory,
          final Object[] messageParts)
  {
    wrappedBodyParts = false;
    if (null != messageParts)
    {
      this.bodyPartCount = messageParts.length;
    }
    else
    {
      this.bodyPartCount = 0;
    }

    this.messageParts = messageParts;
    this.ctx = ctx;
    this.encFactory = encFactory;
    decodedBody = true;
  }

  /**
   * Constructor.
   *
   * @param ctx The encoding context to use.
   * @param wrappedBodyParts True if the encoded body parts are wrapped in BLOBs.
   * @param encFactory The encoder stream factory to use.
   * @param encBodyElements The input stream that holds the encoded body parts.
   */
  public GENMessageBody(final MALEncodingContext ctx,
          final boolean wrappedBodyParts,
          final MALElementStreamFactory encFactory,
          final ByteArrayInputStream encBodyBytes,
          final MALElementInputStream encBodyElements)
  {
    this.ctx = ctx;
    this.wrappedBodyParts = wrappedBodyParts;
    this.encFactory = encFactory;
    this.encBodyBytes = encBodyBytes;
    this.encBodyElements = encBodyElements;
  }

  @Override
  public int getElementCount()
  {
    decodeMessageBody();
    return bodyPartCount;
  }

  @Override
  public MALEncodedBody getEncodedBody() throws MALException
  {
    if (!decodedBody && (encBodyElements instanceof GENElementInputStream))
    {
      byte[] rd = ((GENElementInputStream) encBodyElements).getRemainingEncodedData();
      if ((null != encBodyBytes) && (0 < encBodyBytes.available()))
      {
        byte[] c = new byte[rd.length + encBodyBytes.available()];
        System.arraycopy(rd, 0, c, 0, rd.length);
        encBodyBytes.mark(0);
        encBodyBytes.read(c, rd.length, encBodyBytes.available());
        encBodyBytes.reset();
        
        rd = c;
      }
      return new MALEncodedBody(new Blob(rd));
    }
    else
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }

  @Override
  public Object getBodyElement(final int index, final Object element) throws IllegalArgumentException, MALException
  {
    decodeMessageBody();

    Object rv = messageParts[index];

    if (rv instanceof MALEncodedElementList)
    {
      System.out.println("getBodyElement requesting encoded body list : " + this.decodedBody + " : " + ((MALEncodedElementList) rv).getShortForm());
      rv = decodeEncodedElementListBodyPart(((MALEncodedElementList) rv));
      messageParts[index] = rv;
    }
    if (rv instanceof MALEncodedElement)
    {
      System.out.println("getBodyElement requesting encoded body : " + this.decodedBody);
    }

    return rv;
  }

  @Override
  public MALEncodedElement getEncodedBodyElement(final int index) throws MALException
  {
    if (-1 == index)
    {
      // want the complete message body
      return new MALEncodedElement((Blob) encBodyElements.readElement(new Blob(), null));
    }
    else
    {
      return null;
    }
  }

  /**
   * Encodes the contents of the message body into the provided stream
   *
   * @param streamFactory The stream factory to use for encoder creation.
   * @param enc The output stream to use for encoding.
   * @param lowLevelOutputStream Low level output stream to use when have an already encoded body.
   * @param stage The operation stage being encoded.
   * @param ctx The encoding context.
   * @throws MALException On encoding error.
   */
  public void encodeMessageBody(final MALElementStreamFactory streamFactory,
          final MALElementOutputStream enc,
          final OutputStream lowLevelOutputStream,
          final UOctet stage,
          final MALEncodingContext ctx) throws MALException
  {
    // first check to see if we have an already encoded body
    if ((null != messageParts) && (1 == messageParts.length) && (messageParts[0] instanceof MALEncodedBody))
    {
      enc.flush();

      try
      {
        lowLevelOutputStream.write(((MALEncodedBody) messageParts[0]).getEncodedBody().getValue());
        lowLevelOutputStream.flush();
      }
      catch (IOException ex)
      {
        throw new MALException("MAL encoded body encoding error", ex);
      }
    }
    else if (!decodedBody)
    {
      enc.flush();

      try
      {
        lowLevelOutputStream.write(getEncodedBody().getEncodedBody().getValue());
        lowLevelOutputStream.flush();
      }
      catch (IOException ex)
      {
        throw new MALException("MAL encoded body encoding error", ex);
      }
    }
    else
    {
      final int count = getElementCount();

      GENTransport.LOGGER.log(Level.FINE, "GEN Message encoding body ... pc ({0})", count);

      // if we only have a single body part then encode that directly
      if (count == 1)
      {
        ctx.setBodyElementIndex(0);
        Object sf = ctx.getOperation().getOperationStage(stage).getElementShortForms()[0];
        encodeBodyPart(streamFactory, enc, wrappedBodyParts, sf, getBodyElement(0, null), ctx);
      }
      else if (count > 1)
      {
        MALElementOutputStream benc = enc;
        ByteArrayOutputStream bbaos = null;

        if (wrappedBodyParts)
        {
          // we have more than one body part, therefore encode each part into a separate byte buffer, and then encode
          // that byte buffer as a whole. This allows use to be able to return the complete body of the message as a
          // single unit if required.
          bbaos = new ByteArrayOutputStream();
          benc = streamFactory.createOutputStream(bbaos);
        }

        for (int i = 0; i < count; i++)
        {
          Object sf = null;
          if (null != ctx)
          {
            ctx.setBodyElementIndex(i);

            if (!ctx.getHeader().getIsErrorMessage())
            {
              sf = ctx.getOperation().getOperationStage(stage).getElementShortForms()[i];
            }
          }
          encodeBodyPart(streamFactory, benc, wrappedBodyParts, sf, getBodyElement(i, null), ctx);
        }

        if (wrappedBodyParts)
        {
          benc.flush();
          benc.close();

          enc.writeElement(new Blob(bbaos.toByteArray()), null);
        }
      }
    }

    enc.flush();
    enc.close();
  }

  protected void encodeBodyPart(final MALElementStreamFactory streamFactory,
          final MALElementOutputStream enc,
          final boolean wrapBodyParts,
          final Object sf, final Object o, final MALEncodingContext ctx) throws MALException
  {
    // if it is already an encoded element then just write it directly
    if (o instanceof MALEncodedElement)
    {
      enc.writeElement(((MALEncodedElement) o).getEncodedElement(), ctx);
    }
    // else if it is a MAL data type object
    else if ((null == o) || (o instanceof Element))
    {
      MALElementOutputStream lenc = enc;
      ByteArrayOutputStream lbaos = null;

      if (wrapBodyParts)
      {
        // we encode it into a byte buffer so that it can be extracted as a MALEncodedElement if required
        lbaos = new ByteArrayOutputStream();
        lenc = streamFactory.createOutputStream(lbaos);
      }

      // now encode the element
      lenc.writeElement((Element) o, ctx);

      if (wrapBodyParts)
      {
        lenc.flush();
        lenc.close();

        // write the encoded blob to the stream
        enc.writeElement(new Blob(lbaos.toByteArray()), null);
      }
    }
    // else if it is a JAXB XML object
    else if (o.getClass().isAnnotationPresent(javax.xml.bind.annotation.XmlType.class))
    {
      try
      {
        // get the XML tags for the object
        final String ssf = (String) sf;
        final String schemaURN = ssf.substring(0, ssf.lastIndexOf(':'));
        final String schemaEle = ssf.substring(ssf.lastIndexOf(':') + 1);

        // create the marshaller
        final JAXBContext jc = JAXBContext.newInstance(o.getClass().getPackage().getName());
        final Marshaller marshaller = jc.createMarshaller();

        // encode the XML into a string
        final StringWriter ow = new StringWriter();
        marshaller.marshal(new JAXBElement(new QName(schemaURN, schemaEle), o.getClass(), null, o), ow);
        GENTransport.LOGGER.log(Level.FINE, "GEN Message encoding XML body part : {0}", ow.toString());

        MALElementOutputStream lenc = enc;
        ByteArrayOutputStream lbaos = null;

        if (wrapBodyParts)
        {
          // we encode it into a byte buffer so that it can be extracted as a MALEncodedElement if required
          lbaos = new ByteArrayOutputStream();
          lenc = streamFactory.createOutputStream(lbaos);
        }

        // encode the short form
        lenc.writeElement(new Union(ssf), null);
        // now encode the element
        lenc.writeElement(new Union(ow.toString()), null);

        if (wrapBodyParts)
        {
          lenc.flush();
          lenc.close();

          // write the encoded blob to the stream
          enc.writeElement(new Blob(lbaos.toByteArray()), null);
        }
      }
      catch (JAXBException ex)
      {
        throw new MALException("XML Encoding error", ex);
      }
    }
    else
    {
      throw new MALException("ERROR: Unable to encode body object of type: " + o.getClass().getSimpleName());
    }
  }

  /**
   * Decodes the message body.
   */
  protected void decodeMessageBody()
  {
    if (!decodedBody)
    {
      decodedBody = true;

      try
      {
        if (null == ctx.getOperation())
        {
          MALMessageHeader header = ctx.getHeader();
          MALArea area = MALContextFactory.lookupArea(header.getServiceArea(), header.getAreaVersion());
          if (null != area)
          {
            MALService service = area.getServiceByNumber(header.getService());
            if (null != service)
            {
              MALOperation op = service.getOperationByNumber(header.getOperation());

              if (null != op)
              {
                ctx.setOperation(op);
              }
              else
              {
                GENTransport.LOGGER.log(Level.SEVERE, "Operation for unknown area/version/service/op received ({0}, {1}, {2}, {3})", new Object[]
                {
                  header.getServiceArea(), header.getAreaVersion(), header.getService(), header.getOperation()
                });
              }
            }
            else
            {
              GENTransport.LOGGER.log(Level.SEVERE, "Operation for unknown area/version/service received ({0}, {1}, {2})", new Object[]
              {
                header.getServiceArea(), header.getAreaVersion(), header.getService()
              });
            }
          }
          else
          {
            GENTransport.LOGGER.log(Level.SEVERE, "Operation for unknown area/version received ({0}, {1})", new Object[]
            {
              header.getServiceArea(), header.getAreaVersion()
            });
          }
        }

        if (ctx.getHeader().getIsErrorMessage())
        {
          bodyPartCount = 2;
        }
        else
        {
          bodyPartCount = ctx.getOperation().getOperationStage(ctx.getHeader().getInteractionStage()).getElementShortForms().length;
        }
        GENTransport.LOGGER.log(Level.FINE, "GEN Message decoding body ... pc ({0})", bodyPartCount);
        messageParts = new Object[bodyPartCount];

        if (bodyPartCount == 1)
        {
          Object sf = ctx.getOperation().getOperationStage(ctx.getHeader().getInteractionStage()).getElementShortForms()[0];
          messageParts[0] = decodeBodyPart(encBodyElements, ctx, sf);
        }
        else if (bodyPartCount > 1)
        {
          MALElementInputStream benc = encBodyElements;
          if (wrappedBodyParts)
          {
            GENTransport.LOGGER.fine("GEN Message decoding body wrapper");
            final Blob body = (Blob) encBodyElements.readElement(new Blob(), null);
            final ByteArrayInputStream bais = new ByteArrayInputStream(body.getValue());
            benc = encFactory.createInputStream(bais);
          }

          for (int i = 0; i < bodyPartCount; i++)
          {
            GENTransport.LOGGER.log(Level.FINE, "GEN Message decoding body part : {0}", i);
            Object sf = null;

            ctx.setBodyElementIndex(i);

            if (!ctx.getHeader().getIsErrorMessage())
            {
              sf = ctx.getOperation().getOperationStage(ctx.getHeader().getInteractionStage()).getElementShortForms()[i];
            }

            messageParts[i] = decodeBodyPart(benc, ctx, sf);
          }
        }

        GENTransport.LOGGER.fine("GEN Message decoded body");
      }
      catch (MALException ex)
      {
        GENTransport.LOGGER.log(Level.WARNING, "GEN Message body ERROR on decode : {0}", ex);
      }
    }
  }

  /**
   * Decodes a single part of the message body.
   *
   * @param meel The encoded element list.
   * @return The decoded chunk.
   * @throws MALException if any error detected.
   */
  protected Object decodeEncodedElementListBodyPart(final MALEncodedElementList meel) throws MALException
  {
    long sf = (Long) meel.getShortForm();

    final MALElementFactory ef = MALContextFactory.getElementFactoryRegistry().lookupElementFactory(sf);
    if (null == ef)
    {
      throw new MALException("GEN transport unable to find element factory for short type: " + sf);
    }

    // create list of correct type
    long lsf = (-((sf) & 0xFFFFFFL)) & 0xFFFFFFL + (sf & 0xFFFFFFFFFF000000L);

    ElementList rv = (ElementList) MALContextFactory.getElementFactoryRegistry().lookupElementFactory(lsf).createElement();

    for (MALEncodedElement ele : meel)
    {
      final ByteArrayInputStream lbais = new ByteArrayInputStream(ele.getEncodedElement().getValue());
      MALElementInputStream lenc = encFactory.createInputStream(lbais);

      rv.add(lenc.readElement(ef.createElement(), ctx));
    }

    return rv;
  }

  /**
   * Decodes a single part of the message body.
   *
   * @param decoder The decoder to use.
   * @param ctx The encoding context to use.
   * @param sf The type short form.
   * @return The decoded chunk.
   * @throws MALException if any error detected.
   */
  protected Object decodeBodyPart(final MALElementInputStream decoder, MALEncodingContext ctx, Object sf) throws MALException
  {
    Object rv = null;

    MALElementInputStream lenc = decoder;
    if (wrappedBodyParts)
    {
      final Blob ele = (Blob) decoder.readElement(new Blob(), null);
      final ByteArrayInputStream lbais = new ByteArrayInputStream(ele.getValue());
      lenc = encFactory.createInputStream(lbais);
    }

    // work out whether it is a MAL element or JAXB element we have received
    if (!(sf instanceof String))
    {
      Object element = null;
      if (null != sf)
      {
        Long shortForm = (Long) sf;
        GENTransport.LOGGER.log(Level.FINER, "GEN Message decoding body part : Type = {0}", shortForm);
        final MALElementFactory ef
                = MALContextFactory.getElementFactoryRegistry().lookupElementFactory(shortForm);
        if (null != ef)
        {
          element = (Element) ef.createElement();
        }
        else
        {
          throw new MALException("GEN transport unable to find element factory for short type: " + shortForm);
        }
      }

      rv = lenc.readElement(element, ctx);
    }
    else
    {
      final Union u = (Union) lenc.readElement(new Union(""), null);
      if (null != u)
      {
        final String shortForm = u.getStringValue();
        GENTransport.LOGGER.log(Level.FINER, "GEN Message decoding XML body part : Type = {0}", shortForm);

        try
        {
          final String schemaURN = shortForm.substring(0, shortForm.lastIndexOf(':'));
          final String packageName = (String) ctx.getEndpointQosProperties().get(schemaURN);

          final JAXBContext jc = JAXBContext.newInstance(packageName);
          final Unmarshaller unmarshaller = jc.createUnmarshaller();

          final String srcString = ((Union) lenc.readElement(new Union(""), null)).getStringValue();
          final StringReader ir = new StringReader(srcString);
          final JAXBElement rootElement = (JAXBElement) unmarshaller.unmarshal(ir);
          rv = rootElement.getValue();
        }
        catch (JAXBException ex)
        {
          throw new MALException("XML Decoding error", ex);
        }
      }
    }

    return rv;
  }
}
