package org.openstack4j.model.octavia.options;

import java.util.Objects;

/** Body of an Octavia flavor profile create or update. */
public class FlavorProfileOptions extends OctaviaAttributes<FlavorProfileOptions> {

    public static FlavorProfileOptions create(String name, String providerName, String flavorData) {
        return new FlavorProfileOptions().put("name", Objects.requireNonNull(name)).put("provider_name", Objects.requireNonNull(providerName)).put("flavor_data", Objects.requireNonNull(flavorData));
    }

    /** An update that sends only the fields set afterwards. */
    public static FlavorProfileOptions update() {
        return new FlavorProfileOptions();
    }

    @Override
    protected FlavorProfileOptions self() {
        return this;
    }

    public FlavorProfileOptions name(String value) { return put("name", value); }
    public FlavorProfileOptions providerName(String value) { return put("provider_name", value); }
    public FlavorProfileOptions flavorData(String value) { return put("flavor_data", value); }
}
