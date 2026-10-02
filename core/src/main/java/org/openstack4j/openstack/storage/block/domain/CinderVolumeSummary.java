package org.openstack4j.openstack.storage.block.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.storage.block.VolumeSummary;

@JsonRootName("volume-summary")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderVolumeSummary implements VolumeSummary {

    private static final long serialVersionUID = 1L;

    @JsonProperty("total_count") private Long totalCount;
    @JsonProperty("total_size") private Long totalSize;
    private Map<String, List<String>> metadata;

    @Override public Long getTotalCount() { return totalCount; }
    @Override public Long getTotalSize() { return totalSize; }
    @Override public Map<String, List<String>> getMetadata() { return metadata; }
}
