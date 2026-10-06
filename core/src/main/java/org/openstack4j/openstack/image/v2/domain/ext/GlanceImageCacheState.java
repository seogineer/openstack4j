package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.ImageCacheState;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceImageCacheState implements ImageCacheState {

    private static final long serialVersionUID = 1L;

    @JsonProperty("cached_images") private List<GlanceCachedImageEntry> cachedImages;
    @JsonProperty("queued_images") private List<String> queuedImages;

    @Override public List<GlanceCachedImageEntry> getCachedImages() { return cachedImages == null ? Collections.emptyList() : cachedImages; }
    @Override public List<String> getQueuedImages() { return queuedImages == null ? Collections.emptyList() : queuedImages; }
}
