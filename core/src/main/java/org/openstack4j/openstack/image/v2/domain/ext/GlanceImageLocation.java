package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.ImageLocation;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceImageLocation implements ImageLocation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("url") private String url;
    @JsonProperty("metadata") private Map<String, Object> metadata;

    @Override public String getUrl() { return url; }
    @Override public Map<String, Object> getMetadata() { return metadata; }
}
