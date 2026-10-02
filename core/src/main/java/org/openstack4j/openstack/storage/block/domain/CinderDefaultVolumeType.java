package org.openstack4j.openstack.storage.block.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.model.storage.block.DefaultVolumeType;

@JsonRootName("default_type")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderDefaultVolumeType implements DefaultVolumeType {

    private static final long serialVersionUID = 1L;

    @JsonProperty("project_id") private String projectId;
    @JsonProperty("volume_type_id") private String volumeTypeId;

    @Override public String getProjectId() { return projectId; }
    @Override public String getVolumeTypeId() { return volumeTypeId; }

    public static class DefaultVolumeTypes extends ListResult<CinderDefaultVolumeType> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("default_types")
        private List<CinderDefaultVolumeType> types;

        @Override
        protected List<CinderDefaultVolumeType> value() {
            return types;
        }
    }
}
