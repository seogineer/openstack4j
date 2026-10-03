package org.openstack4j.openstack.networking.internal.ext;

import org.openstack4j.openstack.networking.domain.ext.NeutronQuotaDetails;
import org.openstack4j.model.network.ext.QuotaDetail;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.networking.ext.NetQuotaService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.NetQuota;
import org.openstack4j.openstack.networking.domain.NeutronNetQuota;
import org.openstack4j.openstack.networking.domain.NeutronNetQuota.NeutronNetQuotas;
import org.openstack4j.openstack.networking.internal.BaseNetworkingServices;

/**
 * Networking (Neutron) Quota Extension API
 *
 * @author Jeremy Unruh
 */
public class NetQuotaServiceImpl extends BaseNetworkingServices implements NetQuotaService {

    @Override
    public List<? extends NetQuota> get() {
        return get(NeutronNetQuotas.class, uri("/quotas")).execute().getList();
    }

    @Override
    public NetQuota get(String tenantId) {
        Objects.requireNonNull(tenantId, "TenantId must not be null");
        return get(NeutronNetQuota.class, uri("/quotas/%s", tenantId)).execute();
    }

    @Override
    public NetQuota updateForTenant(String tenantId, NetQuota netQuota) {
        Objects.requireNonNull(tenantId, "TenantId must not be null");
        Objects.requireNonNull(netQuota, "NetQuota must not be null");
        return put(NeutronNetQuota.class, uri("/quotas/%s", tenantId)).entity(netQuota).execute();
    }

    @Override
    public NetQuota update(NetQuota netQuota) {
        Objects.requireNonNull(netQuota, "NetQuota must not be null");
        return put(NeutronNetQuota.class, uri("/quotas")).entity(netQuota).execute();
    }

    @Override
    public ActionResponse reset() {
        return deleteWithResponse(uri("/quotas")).execute();
    }

    @Override
    public ActionResponse reset(String tenantId) {
        Objects.requireNonNull(tenantId, "TenantId must not be null");
        return deleteWithResponse(uri("/quotas/%s", tenantId)).execute();
    }


    @Override
    public NetQuota getDefault(String projectId) {
        return get(NeutronNetQuota.class, uri("/quotas/%s/default", Objects.requireNonNull(projectId))).execute();
    }

    @Override
    public Map<String, ? extends QuotaDetail> getDetails(String projectId) {
        NeutronQuotaDetails details = get(NeutronQuotaDetails.class, uri("/quotas/%s/details.json", Objects.requireNonNull(projectId))).execute(NeutronExecution.propagate404());
        return details == null || details.getQuota() == null ? Collections.emptyMap() : details.getQuota();
    }
}
