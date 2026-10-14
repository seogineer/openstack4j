package org.openstack4j.openstack.blazar.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.blazar.ReservableHostService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.blazar.ReservableHost;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.blazar.domain.BlazarReservableHost;
import org.openstack4j.openstack.blazar.domain.BlazarReservableHost.BlazarReservableHostList;

public class ReservableHostServiceImpl extends BaseBlazarService implements ReservableHostService {

    private static final String PATH = "/os-hosts";

    @Override
    public List<? extends ReservableHost> list() {
        return listOf(BlazarReservableHostList.class, PATH, null);
    }

    @Override
    public ReservableHost get(String hostId) {
        return show(BlazarReservableHost.class, PATH + "/" + id(hostId));
    }

    @Override
    public ReservableHost create(Map<String, ?> host) {
        return create(BlazarReservableHost.class, PATH, host);
    }

    @Override
    public ReservableHost update(String hostId, Map<String, ?> capabilities) {
        return update(BlazarReservableHost.class, PATH + "/" + id(hostId), capabilities);
    }

    @Override
    public ActionResponse delete(String hostId) {
        return remove(PATH + "/" + id(hostId));
    }

    @Override
    public List<Map<String, Object>> listAllocations(Map<String, String> filters) {
        return mapsOf(PATH + "/allocations", "allocations", filters);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getAllocation(String hostId) {
        return objectOf(get(Map.class, PATH + "/" + id(hostId) + "/allocation").execute(propagate404()), "allocation");
    }

    @Override
    public List<Map<String, Object>> listProperties(boolean detail) {
        return listProperties(detail, false);
    }

    @Override
    public List<Map<String, Object>> listProperties(boolean detail, boolean all) {
        Map<String, String> query = new LinkedHashMap<>();
        if (detail)
            query.put("detail", "True");
        if (all)
            query.put("all", "True");
        return mapsOf(PATH + "/properties", "resource_properties", query.isEmpty() ? null : query);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> updateProperty(String propertyName, boolean isPrivate) {
        Map<String, Object> body = patch(Map.class, PATH + "/properties/" + id(propertyName)).entity(JsonBody.of(Map.of("private", isPrivate)))
                .execute(propagate404());
        return objectOf(body, "resource_property");
    }
}
