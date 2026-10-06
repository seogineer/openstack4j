package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"usage": {"<quota>": {"limit", "usage"}}}} of {@code GET /v2/info/usage}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceImageUsage implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("usage")
    private Map<String, GlanceImageUsageEntry> usage;

    public Map<String, GlanceImageUsageEntry> getUsage() {
        return usage;
    }
}
