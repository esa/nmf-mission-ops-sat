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
import esa.opssat.nanomind.opssat_pf.OPSSAT_PFHelper;
import esa.opssat.nanomind.opssat_pf.experimentwd.ExperimentWDHelper;
import esa.opssat.nanomind.opssat_pf.experimentwd.consumer.ExperimentWDStub;

/**
 *
 * @author Cesar Coelho
 */
public class ExperimentWDNanomindConsumerServiceImpl extends ConsumerServiceImpl {

    private ExperimentWDStub experimentWDNanomindService = null;

    @Override
    public Object generateServiceStub(MALConsumer tmConsumer) {
        return new ExperimentWDStub(tmConsumer);
    }

    public ExperimentWDStub getExperimentWDNanomindStub() {
        return experimentWDNanomindService;
    }

    @Override
    public Object getStub() {
        return getExperimentWDNanomindStub();
    }

    public ExperimentWDNanomindConsumerServiceImpl(SingleConnectionDetails connectionDetails) throws MALException, MalformedURLException {

        if (MALContextFactory.lookupArea(MALHelper.MAL_AREA_NAME, MALHelper.MAL_AREA_VERSION) == null) {
            MALHelper.init(MALContextFactory.getElementFactoryRegistry());
        }

        if (MALContextFactory.lookupArea(COMHelper.COM_AREA_NAME, COMHelper.COM_AREA_VERSION) == null) {
            COMHelper.init(MALContextFactory.getElementFactoryRegistry());
        }

        if (MALContextFactory.lookupArea(OPSSAT_PFHelper.OPSSAT_PF_AREA_NAME, OPSSAT_PFHelper.OPSSAT_PF_AREA_VERSION) == null) {
            OPSSAT_PFHelper.init(MALContextFactory.getElementFactoryRegistry());
        }

        try {
            ExperimentWDHelper.init(MALContextFactory.getElementFactoryRegistry());
        } catch (MALException ex) {
        }

        connection = new ConnectionConsumer();
        this.connectionDetails = connectionDetails;

        // Close old connection
        if (tmConsumer != null) {
            try {
                tmConsumer.close();
            } catch (MALException ex) {
                Logger.getLogger(ExperimentWDNanomindConsumerServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        tmConsumer = connection.startService(
                this.connectionDetails.getProviderURI(),
                this.connectionDetails.getBrokerURI(),
                this.connectionDetails.getDomain(),
                ExperimentWDHelper.EXPERIMENTWD_SERVICE);

        this.experimentWDNanomindService = new ExperimentWDStub(tmConsumer);

    }

}
