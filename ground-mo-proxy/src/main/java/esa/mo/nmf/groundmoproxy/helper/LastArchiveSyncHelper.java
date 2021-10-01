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

import esa.mo.com.impl.archive.db.DatabaseBackend;
import esa.mo.com.impl.archive.entities.LastArchiveSyncEntity;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

/**
 * Last Archive Sync helper class.
 *
 * @author Lukasz.Stochlak
 */
public class LastArchiveSyncHelper
{
    final private DatabaseBackend dbBackend;

    /**
     * Default constructor.
     *
     * @param dbBackend DatabaseBackend
     */
    public LastArchiveSyncHelper(DatabaseBackend dbBackend)
    {
        this.dbBackend = dbBackend;
    }

    /**
     * Returns LastArchiveSyncEntity object for given parameters.
     *
     * @param domain domain
     * @param uri URI
     * @return LastArchiveSyncEntity object
     */
    public synchronized LastArchiveSyncEntity findLastArchiveSync(String domain, String uri)
    {
        EntityManager em = dbBackend.getEmf().createEntityManager();

        LastArchiveSyncEntity result = null;
        try
        {
            result = (LastArchiveSyncEntity) em.createNamedQuery("findLastArchiveSync").setParameter("domain", domain)
                    .setParameter("uri", uri).getSingleResult();
        }
        catch (NoResultException e)
        {
            result = null;
        }

        em.close();

        return result;
    }

    /**
     * Saves to Archive (file) given LastArchiveSyncEntity object.
     *
     * @param lastArchiveSyncEntity LastArchiveSyncEntity object
     * @return true if success, false otherwise
     */
    public synchronized boolean persistLastArchiveSync(LastArchiveSyncEntity lastArchiveSyncEntity)
    {
        EntityManager em = dbBackend.getEmf().createEntityManager();

        boolean result = true;

            try
            {
                em.getTransaction().begin();

            if (null != lastArchiveSyncEntity.getId())
            {
                LastArchiveSyncEntity tmp = em.find(LastArchiveSyncEntity.class, lastArchiveSyncEntity.getId());

                if (null != tmp)
                {
                    em.merge(lastArchiveSyncEntity);
                }
                else
                {
                    em.persist(lastArchiveSyncEntity);
                }
            }
            else
            {
                em.persist(lastArchiveSyncEntity);
            }

                em.getTransaction().commit();
            }
            finally
            {
                em.close();
            }

        return result;
    }
}
//------------------------------------------------------------------------------
