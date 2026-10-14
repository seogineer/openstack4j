package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openstack4j.api.types.Facing;

/** Filters of {@code GET /endpoints}. */
public class EndpointListOptions {

    private final Map<String, Object> options = new LinkedHashMap<>();

    public static EndpointListOptions create() {
        return new EndpointListOptions();
    }

    /** The {@code interface} filter ({@code public}, {@code internal} or {@code admin}). */
    public EndpointListOptions iface(Facing facing) { options.put("interface", facing.value()); return this; }
    public EndpointListOptions serviceId(String serviceId) { options.put("service_id", serviceId); return this; }
    public EndpointListOptions regionId(String regionId) { options.put("region_id", regionId); return this; }

    public Map<String, Object> getOptions() {
        return options;
    }
}
