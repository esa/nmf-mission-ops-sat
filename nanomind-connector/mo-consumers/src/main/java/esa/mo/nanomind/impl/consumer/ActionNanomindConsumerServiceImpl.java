package esa.mo.nanomind.impl.consumer;

import esa.mo.helpertools.connections.ConnectionConsumer;
import esa.mo.helpertools.misc.ConsumerServiceImpl;
import esa.mo.helpertools.connections.SingleConnectionDetails;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import esa.opssat.nanomind.com.COMHelper;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.consumer.MALConsumer;
import esa.opssat.nanomind.mc.MCHelper;
import esa.opssat.nanomind.mc.action.ActionHelper;
import esa.opssat.nanomind.mc.action.consumer.ActionStub;

public class ActionNanomindConsumerServiceImpl extends ConsumerServiceImpl {

    private final ActionStub actionService;

    @Override
    public Object generateServiceStub(final MALConsumer tmConsumer) {
        return new ActionStub(tmConsumer);
    }

    public ActionStub getActionStub() {
        return actionService;
    }

    @Override
    public Object getStub() {
        return getActionStub();
    }

    public ActionNanomindConsumerServiceImpl(final SingleConnectionDetails connectionDetails) throws MALException, MalformedURLException {

        if (MALContextFactory.lookupArea(MALHelper.MAL_AREA_NAME, MALHelper.MAL_AREA_VERSION) == null) {
            MALHelper.init(MALContextFactory.getElementFactoryRegistry());
        }

        if (MALContextFactory.lookupArea(COMHelper.COM_AREA_NAME, COMHelper.COM_AREA_VERSION) == null) {
            COMHelper.init(MALContextFactory.getElementFactoryRegistry());
        }

        if (MALContextFactory.lookupArea(MCHelper.MC_AREA_NAME, MCHelper.MC_AREA_VERSION) == null) {
            MCHelper.init(MALContextFactory.getElementFactoryRegistry());
        }

        if (MALContextFactory.lookupArea(MCHelper.MC_AREA_NAME, MCHelper.MC_AREA_VERSION)
                             .getServiceByName(ActionHelper.ACTION_SERVICE_NAME) == null) {
            ActionHelper.init(MALContextFactory.getElementFactoryRegistry());
        }

        connection = new ConnectionConsumer();
        this.connectionDetails = connectionDetails;

        // Close old connection
        if (tmConsumer != null) {
            try {
                tmConsumer.close();
            } catch (final MALException ex) {
                Logger.getLogger(AggregationNanomindConsumerServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        tmConsumer = connection.startService(this.connectionDetails.getProviderURI(), this.connectionDetails
                                                                                                            .getBrokerURI(),
                                             this.connectionDetails.getDomain(), ActionHelper.ACTION_SERVICE);

        this.actionService = new ActionStub(tmConsumer);

    }
}
