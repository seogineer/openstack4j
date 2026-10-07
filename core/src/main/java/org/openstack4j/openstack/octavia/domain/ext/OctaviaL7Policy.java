package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.octavia.ext.L7Policy;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("l7policy")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaL7Policy implements L7Policy {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("listener_id") private String listenerId;
    @JsonProperty("action") private String action;
    @JsonProperty("position") private Integer position;
    @JsonProperty("redirect_pool_id") private String redirectPoolId;
    @JsonProperty("redirect_url") private String redirectUrl;
    @JsonProperty("redirect_prefix") private String redirectPrefix;
    @JsonProperty("redirect_http_code") private Integer redirectHttpCode;
    @JsonProperty("admin_state_up") private Boolean adminStateUp;
    @JsonProperty("provisioning_status") private String provisioningStatus;
    @JsonProperty("operating_status") private String operatingStatus;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("rules") private List<Map<String, Object>> rules;
    @JsonProperty("tags") private List<String> tags;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getListenerId() { return listenerId; }
    @Override public String getAction() { return action; }
    @Override public Integer getPosition() { return position; }
    @Override public String getRedirectPoolId() { return redirectPoolId; }
    @Override public String getRedirectUrl() { return redirectUrl; }
    @Override public String getRedirectPrefix() { return redirectPrefix; }
    @Override public Integer getRedirectHttpCode() { return redirectHttpCode; }
    @Override public Boolean isAdminStateUp() { return adminStateUp; }
    @Override public String getProvisioningStatus() { return provisioningStatus; }
    @Override public String getOperatingStatus() { return operatingStatus; }
    @Override public String getProjectId() { return projectId; }
    @Override public List<Map<String, Object>> getRules() { return rules; }
    @Override public List<String> getTags() { return tags; }

    public static class L7Policies extends ListResult<OctaviaL7Policy> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("l7policies")
        private List<OctaviaL7Policy> list;

        @Override
        protected List<OctaviaL7Policy> value() {
            return list;
        }
    }
}
