package org.openstack4j.openstack.image.v2.domain.ext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.CachedImageEntry;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceCachedImageEntry implements CachedImageEntry {

    private static final long serialVersionUID = 1L;

    @JsonProperty("image_id") private String imageId;
    @JsonProperty("hits") private Integer hits;
    @JsonProperty("last_accessed") private Double lastAccessed;
    @JsonProperty("last_modified") private Double lastModified;
    @JsonProperty("size") private Long size;

    @Override public String getImageId() { return imageId; }
    @Override public Integer getHits() { return hits; }
    @Override public Double getLastAccessed() { return lastAccessed; }
    @Override public Double getLastModified() { return lastModified; }
    @Override public Long getSize() { return size; }
}
