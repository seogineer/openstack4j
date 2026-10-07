package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ExportLocation;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("export_location")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaExportLocation implements ExportLocation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("path") private String path;
    @JsonProperty("preferred") private Boolean preferred;
    @JsonProperty("is_admin_only") private Boolean isAdminOnly;
    @JsonProperty("share_instance_id") private String shareInstanceId;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getPath() { return path; }
    @Override public Boolean isPreferred() { return preferred; }
    @Override public Boolean isAdminOnly() { return isAdminOnly; }
    @Override public String getShareInstanceId() { return shareInstanceId; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class ManilaExportLocationList extends ListResult<ManilaExportLocation> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("export_locations")
        private List<ManilaExportLocation> list;

        @Override
        protected List<ManilaExportLocation> value() {
            return list;
        }
    }
}
