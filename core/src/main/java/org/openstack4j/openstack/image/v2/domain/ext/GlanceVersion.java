package org.openstack4j.openstack.image.v2.domain.ext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.ImageVersion;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceVersion implements ImageVersion {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("status") private String status;

    @Override public String getId() { return id; }
    @Override public String getStatus() { return status; }
}
