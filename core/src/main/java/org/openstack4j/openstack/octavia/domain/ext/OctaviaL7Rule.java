package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.octavia.ext.L7Rule;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("rule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaL7Rule implements L7Rule {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("type") private String type;
    @JsonProperty("compare_type") private String compareType;
    @JsonProperty("key") private String key;
    @JsonProperty("value") private String value;
    @JsonProperty("invert") private Boolean invert;
    @JsonProperty("admin_state_up") private Boolean adminStateUp;
    @JsonProperty("provisioning_status") private String provisioningStatus;
    @JsonProperty("operating_status") private String operatingStatus;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("tags") private List<String> tags;

    @Override public String getId() { return id; }
    @Override public String getType() { return type; }
    @Override public String getCompareType() { return compareType; }
    @Override public String getKey() { return key; }
    @Override public String getValue() { return value; }
    @Override public Boolean isInvert() { return invert; }
    @Override public Boolean isAdminStateUp() { return adminStateUp; }
    @Override public String getProvisioningStatus() { return provisioningStatus; }
    @Override public String getOperatingStatus() { return operatingStatus; }
    @Override public String getProjectId() { return projectId; }
    @Override public List<String> getTags() { return tags; }

    public static class L7Rules extends ListResult<OctaviaL7Rule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("rules")
        private List<OctaviaL7Rule> list;

        @Override
        protected List<OctaviaL7Rule> value() {
            return list;
        }
    }
}
