/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under European Space Agency Public License (ESA-PL) Weak Copyleft – v2.4
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
package esa.mo.nanomind.impl.consumer;

import esa.mo.helpertools.connections.SingleConnectionDetails;
import esa.mo.helpertools.helpers.HelperAttributes;
import esa.mo.helpertools.misc.ConsumerServiceImpl;
import esa.mo.nanomind.impl.util.EventCOMObject;
import esa.mo.nanomind.impl.util.HelperCOM;
import esa.opssat.nanomind.com.COMHelper;
import esa.opssat.nanomind.com.event.EventHelper;
import esa.opssat.nanomind.com.event.consumer.EventAdapter;
import esa.opssat.nanomind.com.event.consumer.EventStub;
import esa.opssat.nanomind.com.structures.ObjectDetailsList;
import esa.opssat.nanomind.com.structures.ObjectType;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.consumer.MALConsumer;
import org.ccsds.moims.mo.mal.structures.Element;
import org.ccsds.moims.mo.mal.structures.ElementList;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.Subscription;
import org.ccsds.moims.mo.mal.structures.SubscriptionList;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;

/**
 *
 * @author Cesar Coelho
 */
public class EventNanomindConsumerServiceImpl extends ConsumerServiceImpl {

    private static final Logger LOGGER = Logger.getLogger(EventNanomindConsumerServiceImpl.class.getName());

    private final EventStub eventService;
    private final SubscriptionList subs = new SubscriptionList();

    @Override
    public Object generateServiceStub(final MALConsumer tmConsumer) {
        return new EventStub(tmConsumer);
    }

    @Override
    public Object getStub() {
        return this.getEventStub();
    }

    public EventStub getEventStub() {
        return eventService;
    }

    public EventNanomindConsumerServiceImpl(final SingleConnectionDetails connectionDetails) throws MALException, MalformedURLException {
        if (MALContextFactory.lookupArea(MALHelper.MAL_AREA_NAME, MALHelper.MAL_AREA_VERSION) == null) {
            MALHelper.init(MALContextFactory.getElementFactoryRegistry());
        }

        if (MALContextFactory.lookupArea(COMHelper.COM_AREA_NAME, COMHelper.COM_AREA_VERSION) == null) {
            COMHelper.init(MALContextFactory.getElementFactoryRegistry());
        }

        if (MALContextFactory.lookupArea(COMHelper.COM_AREA_NAME, COMHelper.COM_AREA_VERSION)
                             .getServiceByName(EventHelper.EVENT_SERVICE_NAME) == null) {
            EventHelper.init(MALContextFactory.getElementFactoryRegistry());
        }

        this.connectionDetails = connectionDetails;

        // Close old connection
        if (tmConsumer != null) {
            try {
                tmConsumer.close();
            } catch (final MALException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        tmConsumer = connection.startService(this.connectionDetails.getProviderURI(), this.connectionDetails
                                                                                                            .getBrokerURI(),
                                             this.connectionDetails.getDomain(), EventHelper.EVENT_SERVICE);

        this.eventService = new EventStub(tmConsumer);
    }

    public void registerDefaultEventHandler() throws MALInteractionException, MALException {

        // Make the event adapter to call the eventReceivedListener when there's a new object available
        class EventReceivedAdapter extends EventAdapter {

            @Override
            public void monitorEventNotifyReceived(final MALMessageHeader msgHeader, final Identifier lIdentifier,
                                                   final UpdateHeaderList lUpdateHeaderList,
                                                   final ObjectDetailsList objectDetailsList,
                                                   final ElementList elementList, final Map qosProperties) {
                if (objectDetailsList.size() == lUpdateHeaderList.size()) {
                    for (int i = 0; i < lUpdateHeaderList.size(); i++) {

                        final Identifier entityKey1 = lUpdateHeaderList.get(i).getKey().getFirstSubKey();
                        final Long entityKey2 = lUpdateHeaderList.get(i).getKey().getSecondSubKey();
                        final Long entityKey3 = lUpdateHeaderList.get(i).getKey().getThirdSubKey();
                        final Long entityKey4 = lUpdateHeaderList.get(i).getKey().getFourthSubKey(); // ObjType of the source

                        // (UShort area, UShort service, UOctet version, UShort number)
                        // (UShort area, UShort service, UOctet version, 0)
                        final ObjectType objType = HelperCOM.objectTypeId2objectType(entityKey2);
                        objType.setNumber(new UShort(Integer.parseInt(entityKey1.toString())));

                        final Object nativeBody = ((elementList == null) ? null : elementList.get(i));
                        final Element body = (Element) HelperAttributes.javaType2Attribute(nativeBody);

                        // ----
                        final EventCOMObject newEvent = new EventCOMObject();
                        //                        newEvent.setDomain(msgHeader.getDomain());
                        newEvent.setDomain(connectionDetails.getDomain());
                        newEvent.setObjType(objType);
                        newEvent.setObjId(entityKey3);

                        newEvent.setSource(objectDetailsList.get(i).getSource());
                        newEvent.setRelated(objectDetailsList.get(i).getRelated());
                        newEvent.setBody(body);

                        newEvent.setTimestamp(lUpdateHeaderList.get(i).getTimestamp());
                        newEvent.setSourceURI(lUpdateHeaderList.get(i).getSourceURI());
                        newEvent.setNetworkZone(msgHeader.getNetworkZone());
                        LOGGER.log(Level.INFO, "COM Event from the Nanomind: {0}", newEvent.toString());
                    }
                }
            }
        }
        // Register with the subscription key provided
        // FIXME: Causes an exception at SPPMessageHeader.java:115
        //  this.getEventStub().monitorEventRegister(ConnectionConsumer.subscriptionWildcard(
        //      new Identifier("SUB")), new EventReceivedAdapter());
    }

    /**
     * Closes the tmConsumer connection
     *
     */
    @Override
    protected void closeConnection() {
        // Close old connection
        if (tmConsumer != null) {
            try {
                final IdentifierList subLst = new IdentifierList();

                for (final Subscription sub : subs) {
                    subLst.add(sub.getSubscriptionId());
                }

                if (eventService != null) {
                    try {
                        eventService.monitorEventDeregister(subLst);
                    } catch (final MALInteractionException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                }

                tmConsumer.close();
            } catch (final MALException ex) {
                Logger.getLogger(ConsumerServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
