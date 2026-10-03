package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.NetworkSegmentRange;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("network_segment_range")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronNetworkSegmentRange implements NetworkSegmentRange {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("default") private Boolean defaultValue;
    @JsonProperty("shared") private Boolean shared;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("network_type") private String networkType;
    @JsonProperty("physical_network") private String physicalNetwork;
    @JsonProperty("minimum") private Integer minimum;
    @JsonProperty("maximum") private Integer maximum;
    @JsonProperty("available") private List<Integer> available;
    @JsonProperty("used") private Map<String, String> used;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public Boolean isDefault() { return defaultValue; }
    @Override public Boolean isShared() { return shared; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getNetworkType() { return networkType; }
    @Override public String getPhysicalNetwork() { return physicalNetwork; }
    @Override public Integer getMinimum() { return minimum; }
    @Override public Integer getMaximum() { return maximum; }
    @Override public List<Integer> getAvailable() { return available; }
    @Override public Map<String, String> getUsed() { return used; }

    public static class NetworkSegmentRanges extends ListResult<NeutronNetworkSegmentRange> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("network_segment_ranges")
        private List<NeutronNetworkSegmentRange> list;

        @Override
        protected List<NeutronNetworkSegmentRange> value() {
            return list;
        }
    }
}
