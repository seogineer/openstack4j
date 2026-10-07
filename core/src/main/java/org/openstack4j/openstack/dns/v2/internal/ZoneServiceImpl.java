package org.openstack4j.openstack.dns.v2.internal;

import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.dns.v2.internal.ext.BaseDesignateExtService;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.dns.v2.ZoneService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.Nameserver;
import org.openstack4j.model.dns.v2.Zone;
import org.openstack4j.openstack.dns.v2.domain.DesignateNameserver;
import org.openstack4j.openstack.dns.v2.domain.DesignateZone;

import static org.openstack4j.core.transport.ClientConstants.PATH_NAMESERVERS;
import static org.openstack4j.core.transport.ClientConstants.PATH_ZONES;

public class ZoneServiceImpl extends BaseDNSServices implements ZoneService {

    @Override
    public Zone get(String zoneId) {
        Objects.requireNonNull(zoneId);
        return get(DesignateZone.class, PATH_ZONES, "/", zoneId).execute();
    }

    @Override
    public ActionResponse delete(String zoneId) {
        Objects.requireNonNull(zoneId);
        return deleteWithResponse(PATH_ZONES, "/", zoneId).execute();
    }

    @Override
    public List<? extends Nameserver> listNameservers(String zoneId) {
        Objects.requireNonNull(zoneId);
        return get(DesignateNameserver.Nameservers.class, PATH_ZONES, "/", zoneId, PATH_NAMESERVERS).execute().getList();
    }

    @Override
    public Zone update(Zone zone) {
        Objects.requireNonNull(zone);
        return patch(DesignateZone.class, PATH_ZONES, "/", zone.getId()).entity(zone).execute();
    }

    @Override
    public Zone create(Zone zone) {
        Objects.requireNonNull(zone);
        return post(DesignateZone.class, uri(PATH_ZONES)).entity(zone).execute();
    }

    @Override
    public Zone create(String name, String email) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(email);
        return create(DesignateZone.builder().name(name).email(email).build());
    }

    @Override
    public List<? extends Zone> list() {
        return get(DesignateZone.Zones.class, uri(PATH_ZONES)).execute().getList();
    }

    @Override
    public List<? extends Zone> list(Map<String, String> filters) {
        return get(DesignateZone.Zones.class, uri(PATH_ZONES)).params(filters == null ? Collections.emptyMap() : filters)
                .execute(BaseDesignateExtService.propagate404()).getList();
    }

    @Override
    public ActionResponse abandon(String zoneId) {
        return task(zoneId, "abandon", Map.of());
    }

    @Override
    public ActionResponse transferFromMaster(String zoneId) {
        return task(zoneId, "xfr", Map.of());
    }

    @Override
    public ActionResponse movePool(String zoneId, String poolId) {
        return task(zoneId, "pool_move", poolId == null ? Map.of() : Map.of("pool_id", poolId));
    }

    private ActionResponse task(String zoneId, String task, Map<String, ?> body) {
        Objects.requireNonNull(zoneId, "zoneId");
        return postWithResponse(PATH_ZONES, "/", zoneId, "/tasks/", task).entity(JsonBody.of(body)).execute();
    }

}
