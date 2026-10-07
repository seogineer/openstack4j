package org.openstack4j.openstack.octavia.internal.ext;

import java.util.List;

import org.openstack4j.api.octavia.ext.QuotaService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.OctaviaQuota;
import org.openstack4j.model.octavia.options.OctaviaQuotaOptions;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaQuotaEntity;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaQuotaEntity.Quotas;

public class QuotaServiceImpl extends BaseOctaviaExtService implements QuotaService {

    private static final String QUOTAS = "/lbaas/quotas";

    @Override public List<? extends OctaviaQuota> list() { return listOf(Quotas.class, QUOTAS, null); }
    @Override public List<? extends OctaviaQuota> list(java.util.Map<String, String> filters) { return listOf(Quotas.class, QUOTAS, filters); }
    @Override public OctaviaQuota defaults() { return showStrict(OctaviaQuotaEntity.class, QUOTAS + "/defaults"); }
    @Override public OctaviaQuota get(String projectId) { return show(OctaviaQuotaEntity.class, QUOTAS + "/" + id(projectId)); }
    @Override public OctaviaQuota update(String projectId, OctaviaQuotaOptions options) { return update(OctaviaQuotaEntity.class, QUOTAS + "/" + id(projectId), "quota", options); }
    @Override public ActionResponse reset(String projectId) { return remove(QUOTAS + "/" + id(projectId)); }
}
