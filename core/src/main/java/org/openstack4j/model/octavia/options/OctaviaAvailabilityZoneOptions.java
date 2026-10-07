package org.openstack4j.model.octavia.options;

import java.util.Objects;

/** Body of an Octavia availability zone create or update. */
public class OctaviaAvailabilityZoneOptions extends OctaviaAttributes<OctaviaAvailabilityZoneOptions> {

    public static OctaviaAvailabilityZoneOptions create(String name, String availabilityZoneProfileId) {
        return new OctaviaAvailabilityZoneOptions().put("name", Objects.requireNonNull(name)).put("availability_zone_profile_id", Objects.requireNonNull(availabilityZoneProfileId));
    }

    /** An update that sends only the fields set afterwards. */
    public static OctaviaAvailabilityZoneOptions update() {
        return new OctaviaAvailabilityZoneOptions();
    }

    @Override
    protected OctaviaAvailabilityZoneOptions self() {
        return this;
    }

    public OctaviaAvailabilityZoneOptions description(String value) { return put("description", value); }
    public OctaviaAvailabilityZoneOptions enabled(Boolean value) { return put("enabled", value); }
}
