package org.openstack4j.model.octavia.options;

import java.util.Objects;

/** Body of an Octavia availability zone profile create or update. */
public class AvailabilityZoneProfileOptions extends OctaviaAttributes<AvailabilityZoneProfileOptions> {

    public static AvailabilityZoneProfileOptions create(String name, String providerName, String availabilityZoneData) {
        return new AvailabilityZoneProfileOptions().put("name", Objects.requireNonNull(name)).put("provider_name", Objects.requireNonNull(providerName)).put("availability_zone_data", Objects.requireNonNull(availabilityZoneData));
    }

    /** An update that sends only the fields set afterwards. */
    public static AvailabilityZoneProfileOptions update() {
        return new AvailabilityZoneProfileOptions();
    }

    @Override
    protected AvailabilityZoneProfileOptions self() {
        return this;
    }

    public AvailabilityZoneProfileOptions name(String value) { return put("name", value); }
    public AvailabilityZoneProfileOptions providerName(String value) { return put("provider_name", value); }
    public AvailabilityZoneProfileOptions availabilityZoneData(String value) { return put("availability_zone_data", value); }
}
