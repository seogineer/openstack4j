package org.openstack4j.openstack.image.v2.domain.ext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.ImageUsage;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceImageUsageEntry implements ImageUsage {

    private static final long serialVersionUID = 1L;

    @JsonProperty("limit") private Long limit;
    @JsonProperty("usage") private Long usage;

    @Override public Long getLimit() { return limit; }
    @Override public Long getUsage() { return usage; }
}
