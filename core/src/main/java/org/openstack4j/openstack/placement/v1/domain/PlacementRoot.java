package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.openstack.internal.MicroVersion;

/** Response of {@code GET /} on the Placement endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementRoot implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("versions")
    private List<Entry> versions;

    public MicroVersion getMinVersion() {
        return parse(versions == null || versions.isEmpty() ? null : versions.get(0).minVersion);
    }

    public MicroVersion getMaxVersion() {
        return parse(versions == null || versions.isEmpty() ? null : versions.get(0).maxVersion);
    }

    private static MicroVersion parse(String value) {
        return value == null || value.isEmpty() ? new MicroVersion(1, 0) : new MicroVersion(value);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("min_version")
        String minVersion;
        @JsonProperty("max_version")
        String maxVersion;
    }
}
