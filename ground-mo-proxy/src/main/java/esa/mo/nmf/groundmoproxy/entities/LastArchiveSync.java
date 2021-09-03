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
package esa.mo.nmf.groundmoproxy.entities;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Entity holding lat archive synchronization time for provider.
 *
 * @author Lukasz.Stochlak
 */
@Entity(name = "LastArchiveSync")
@Table(name = "LastArchiveSync", indexes = {
        @Index(name = "index_provider", columnList = "domain, provider_uri", unique = true) })
@NamedQuery(
        name="findLastArchiveSync",
        query="SELECT OBJECT(p) FROM LastArchiveSync p WHERE p.domain = (:domain) AND p.providerUri = (:uri)"
)
public class LastArchiveSync
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "provider_uri", nullable = false)
    private String providerUri;

    @Column(nullable = false)
    private String domain;

    @Column(name = "last_sync", nullable = false)
    private Long lastSync;

    public LastArchiveSync()
    {
    }

    public LastArchiveSync(String providerUri, String domain)
    {
        this.providerUri = providerUri;
        this.domain = domain;
        this.lastSync = lastSync;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getId(), getProviderUri(), getDomain(), getLastSync());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        LastArchiveSync that = (LastArchiveSync) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getProviderUri(), that.getProviderUri())
               && Objects.equals(getDomain(), that.getDomain())
               && Objects.equals(getLastSync(), that.getLastSync());
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getProviderUri()
    {
        return providerUri;
    }

    public void setProviderUri(String providerUri)
    {
        this.providerUri = providerUri;
    }

    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public Long getLastSync()
    {
        return lastSync;
    }

    public void setLastSync(Long lastSync)
    {
        this.lastSync = lastSync;
    }

    @Override
    public String toString()
    {
        return "LastArchiveSync{" + "id=" + id + ", providerUri='" + providerUri + '\'' + ", domain='" + domain + '\''
               + ", lastSync=" + lastSync + '}';
    }
}
//------------------------------------------------------------------------------
