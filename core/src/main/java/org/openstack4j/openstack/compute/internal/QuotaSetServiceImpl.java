package org.openstack4j.openstack.compute.internal;

import org.openstack4j.openstack.compute.domain.NovaQuotaSetUpdate;
import org.openstack4j.openstack.internal.MicroVersion;

import static org.openstack4j.openstack.compute.internal.ComputeMicroVersions.V;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.compute.QuotaSetService;
import org.openstack4j.model.compute.Limits;
import org.openstack4j.model.compute.QuotaSet;
import org.openstack4j.model.compute.QuotaSetUpdate;
import org.openstack4j.model.compute.SimpleTenantUsage;
import org.openstack4j.model.compute.ComputeQuotaDetail;
import org.openstack4j.openstack.compute.domain.NovaComputeQuotaDetail;
import org.openstack4j.openstack.compute.domain.NovaLimits;
import org.openstack4j.openstack.compute.domain.NovaQuotaSet;
import org.openstack4j.openstack.compute.domain.NovaQuotaSet.NovaQuotaSetClass;
import org.openstack4j.openstack.compute.domain.NovaQuotaSetUpdate.NovaQuotaSetUpdateClass;
import org.openstack4j.openstack.compute.domain.NovaQuotaSetUpdate.NovaQuotaSetUpdateTenant;
import org.openstack4j.openstack.compute.domain.NovaSimpleTenantUsage;
import org.openstack4j.openstack.compute.domain.NovaSimpleTenantUsage.NovaSimpleTenantUsages;

/**
 * OpenStack Quota-Set API Implementation
 *
 * @author Jeremy Unruh
 */
public class QuotaSetServiceImpl extends BaseComputeServices implements QuotaSetService {

    /**
     * {@inheritDoc}
     */
    @Override
    public QuotaSet get(String tenantId) {
        return get(tenantId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComputeQuotaDetail getDetail(String tenantId) {
        Objects.requireNonNull(tenantId);
        String uri = uri("/os-quota-sets/%s/detail", tenantId);
        return get(NovaComputeQuotaDetail.class, uri).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QuotaSet get(String tenantId, String userId) {
        Objects.requireNonNull(tenantId);
        String uri = (userId != null) ? uri("/os-quota-sets/%s?user_id=%s", tenantId, userId): uri("/os-quota-sets/%s", tenantId);
        return get(NovaQuotaSet.class, uri).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QuotaSet updateForClass(String classId, QuotaSetUpdate qs) {
        Objects.requireNonNull(classId);
        Objects.requireNonNull(qs);

        NovaQuotaSetUpdateClass update = NovaQuotaSetUpdateClass.from(qs);
        return capped(put(NovaQuotaSetClass.class, uri("/os-quota-class-sets/%s", classId)), quotaCeiling(update)).entity(update).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QuotaSet updateForTenant(String tenantId, QuotaSetUpdate qs) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(qs);

        NovaQuotaSetUpdateTenant update = NovaQuotaSetUpdateTenant.from(qs);
        return capped(put(NovaQuotaSet.class, uri("/os-quota-sets/%s", tenantId)), quotaCeiling(update)).entity(update).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Limits limits() {
        return get(NovaLimits.class, uri("/limits")).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SimpleTenantUsage> listTenantUsages() {
        return get(NovaSimpleTenantUsages.class, uri("/os-simple-tenant-usage")).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SimpleTenantUsage getTenantUsage(String tenantId) {
        Objects.requireNonNull(tenantId);
        return get(NovaSimpleTenantUsage.class, uri("/os-simple-tenant-usage/%s", tenantId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SimpleTenantUsage> listTenantUsages(String startTime,
            String endTime) {
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);
        return get(NovaSimpleTenantUsages.class, uri("/os-simple-tenant-usage"))
                .param("start", startTime)
                .param("end", endTime)
                .execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SimpleTenantUsage getTenantUsage(String tenantId, String startTime,
            String endTime) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);
        // the show query rejects "detailed" from 2.75
        return capped(get(NovaSimpleTenantUsage.class, uri("/os-simple-tenant-usage/%s", tenantId)), V(74))
                .param("start", startTime)
                .param("end", endTime)
                .param("detailed", "1")
                .execute();
    }

    @Override
    public QuotaSet defaults(String tenantId) {
        Objects.requireNonNull(tenantId);
        return get(NovaQuotaSet.class, uri("/os-quota-sets/%s/defaults", tenantId)).execute();
    }

    /** Network quotas were removed in 2.36 and injected-file quotas in 2.57. */
    private static MicroVersion quotaCeiling(NovaQuotaSetUpdate update) {
        if (update.hasNetworkQuotas())
            return V(35);
        if (update.hasInjectedFileQuotas())
            return V(56);
        return null;
    }
}
