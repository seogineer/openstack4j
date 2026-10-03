package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.MeteringLabel;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("metering_label")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronMeteringLabel implements MeteringLabel {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("shared") private Boolean shared;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getProjectId() { return projectId; }
    @Override public Boolean isShared() { return shared; }

    public static class MeteringLabels extends ListResult<NeutronMeteringLabel> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("metering_labels")
        private List<NeutronMeteringLabel> list;

        @Override
        protected List<NeutronMeteringLabel> value() {
            return list;
        }
    }
}
