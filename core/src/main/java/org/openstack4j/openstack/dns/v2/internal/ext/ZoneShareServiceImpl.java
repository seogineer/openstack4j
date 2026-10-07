package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.dns.v2.ext.ZoneShareService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.ZoneShare;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateZoneShare;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateZoneShare.DesignateZoneShareList;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class ZoneShareServiceImpl extends BaseDesignateExtService implements ZoneShareService {

    private static String shares(String zoneId) {
        return "/zones/" + id(zoneId) + "/shares";
    }

    @Override
    public List<? extends ZoneShare> list(String zoneId) {
        return list(zoneId, null);
    }

    @Override
    public List<? extends ZoneShare> list(String zoneId, Map<String, String> filters) {
        return listOf(DesignateZoneShareList.class, shares(zoneId), filters);
    }

    @Override
    public ZoneShare get(String zoneId, String shareId) {
        return show(DesignateZoneShare.class, shares(zoneId) + "/" + id(shareId));
    }

    @Override
    public ZoneShare create(String zoneId, String targetProjectId) {
        return post(DesignateZoneShare.class, shares(zoneId))
                .entity(JsonBody.of(Map.of("target_project_id", Objects.requireNonNull(targetProjectId, "targetProjectId")))).execute(propagate404());
    }

    @Override
    public ActionResponse delete(String zoneId, String shareId) {
        return remove(shares(zoneId) + "/" + id(shareId));
    }
}
