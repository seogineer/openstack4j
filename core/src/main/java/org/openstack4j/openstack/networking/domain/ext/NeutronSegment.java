package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.Segment;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("segment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronSegment implements Segment {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("network_id") private String networkId;
    @JsonProperty("network_type") private String networkType;
    @JsonProperty("physical_network") private String physicalNetwork;
    @JsonProperty("segmentation_id") private Integer segmentationId;
    @JsonProperty("revision_number") private Integer revisionNumber;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getNetworkId() { return networkId; }
    @Override public String getNetworkType() { return networkType; }
    @Override public String getPhysicalNetwork() { return physicalNetwork; }
    @Override public Integer getSegmentationId() { return segmentationId; }
    @Override public Integer getRevisionNumber() { return revisionNumber; }

    public static class Segments extends ListResult<NeutronSegment> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("segments")
        private List<NeutronSegment> list;

        @Override
        protected List<NeutronSegment> value() {
            return list;
        }
    }
}
