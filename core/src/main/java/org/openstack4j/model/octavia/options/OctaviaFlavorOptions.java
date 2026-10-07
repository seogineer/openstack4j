package org.openstack4j.model.octavia.options;

import java.util.Objects;

/** Body of an Octavia flavor create or update. */
public class OctaviaFlavorOptions extends OctaviaAttributes<OctaviaFlavorOptions> {

    public static OctaviaFlavorOptions create(String name, String flavorProfileId) {
        return new OctaviaFlavorOptions().put("name", Objects.requireNonNull(name)).put("flavor_profile_id", Objects.requireNonNull(flavorProfileId));
    }

    /** An update that sends only the fields set afterwards. */
    public static OctaviaFlavorOptions update() {
        return new OctaviaFlavorOptions();
    }

    @Override
    protected OctaviaFlavorOptions self() {
        return this;
    }

    public OctaviaFlavorOptions name(String value) { return put("name", value); }
    public OctaviaFlavorOptions description(String value) { return put("description", value); }
    public OctaviaFlavorOptions enabled(Boolean value) { return put("enabled", value); }
}
