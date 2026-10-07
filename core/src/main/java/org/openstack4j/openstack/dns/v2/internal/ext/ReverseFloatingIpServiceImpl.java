package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.dns.v2.ext.ReverseFloatingIpService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.ReverseFloatingIp;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateReverseFloatingIp;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateReverseFloatingIp.DesignateReverseFloatingIpList;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class ReverseFloatingIpServiceImpl extends BaseDesignateExtService implements ReverseFloatingIpService {

    private static final String PATH = "/reverse/floatingips";

    private static String fip(String region, String floatingIpId) {
        return PATH + "/" + id(region) + ":" + id(floatingIpId);
    }

    @Override
    public List<? extends ReverseFloatingIp> list() {
        return list(null);
    }

    @Override
    public List<? extends ReverseFloatingIp> list(Map<String, String> filters) {
        return listOf(DesignateReverseFloatingIpList.class, PATH, filters);
    }

    @Override
    public ReverseFloatingIp get(String region, String floatingIpId) {
        return show(DesignateReverseFloatingIp.class, fip(region, floatingIpId));
    }

    @Override
    public ReverseFloatingIp set(String region, String floatingIpId, String ptrdname, String description, Integer ttl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ptrdname", Objects.requireNonNull(ptrdname, "ptrdname"));
        if (description != null)
            body.put("description", description);
        if (ttl != null)
            body.put("ttl", ttl);
        return patch(DesignateReverseFloatingIp.class, fip(region, floatingIpId)).entity(JsonBody.of(body)).execute(propagate404());
    }

    @Override
    public ActionResponse unset(String region, String floatingIpId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ptrdname", null);
        return patchWithResponse(fip(region, floatingIpId)).entity(JsonBody.of(body)).execute();
    }
}
