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
package esa.mo.nmf.groundmoproxy.helper;

import esa.mo.nmf.groundmoproxy.entities.LastArchiveSync;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;

/**
 * Last Archive Sync helper class.
 *
 * @author Lukasz.Stochlak
 */
public class LastArchiveSyncHelper
{
    private EntityManager em;

    private Semaphore emAvailability;

    /**
     * Default constructor.
     *
     * @param em EntityManager
     * @param emAvailability EntityManager availability Semaphore
     */
    public LastArchiveSyncHelper(EntityManager em, Semaphore emAvailability)
    {
        this.em = em;
        this.emAvailability = emAvailability;
    }

    /**
     * Returns LastArchiveSync object for given parameters.
     *
     * @param domain domain
     * @param uri URI
     * @return LastArchiveSync object
     */
    public synchronized LastArchiveSync findLastArchiveSync(String domain, String uri)
    {
        LastArchiveSync result = null;

        try
        {
            emAvailability.acquire();

            result = (LastArchiveSync) em.createNamedQuery("findLastArchiveSync").setParameter("domain", domain)
                    .setParameter("uri", uri).getSingleResult();

        }
        catch (InterruptedException e)
        {
            Logger.getLogger(LastArchiveSyncHelper.class.getName()).log(Level.SEVERE, null, e);
            result = null;
        }

        emAvailability.release();

        return result;
    }

    /**
     * Saves to Archive (file) given LastArchiveSync object.
     *
     * @param lastArchiveSync LastArchiveSync object
     * @return true if success, false otherwise
     */
    public synchronized boolean persistLastArchiveSync(LastArchiveSync lastArchiveSync)
    {
        boolean result = true;

        try
        {
            emAvailability.acquire();

            try
            {
                em.getTransaction().begin();
                em.persist(lastArchiveSync);
                em.getTransaction().commit();
            }
            finally
            {
                em.close();
            }
        }
        catch (InterruptedException e)
        {
            Logger.getLogger(LastArchiveSyncHelper.class.getName()).log(Level.SEVERE, null, e);
            result = false;
        }

        emAvailability.release();

        return result;
    }

}
//------------------------------------------------------------------------------
