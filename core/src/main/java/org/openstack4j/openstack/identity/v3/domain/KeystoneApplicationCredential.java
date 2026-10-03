package org.openstack4j.openstack.identity.v3.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.ApplicationCredential;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("application_credential")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneApplicationCredential implements ApplicationCredential {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    @JsonProperty("user_id") private String userId;
    @JsonProperty("project_id") private String projectId;
    private Map<String, Object> system;
    @JsonProperty("expires_at") private Date expiresAt;
    private Boolean unrestricted;
    private List<KeystoneRole> roles;
    @JsonProperty("access_rules") private List<KeystoneAccessRule> accessRules;
    private String secret;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getUserId() { return userId; }
    @Override public String getProjectId() { return projectId; }
    @Override public Map<String, Object> getSystem() { return system; }
    @Override public Date getExpiresAt() { return expiresAt; }
    @Override public Boolean getUnrestricted() { return unrestricted; }
    @Override public List<KeystoneRole> getRoles() { return roles; }
    @Override public List<KeystoneAccessRule> getAccessRules() { return accessRules; }
    @Override public String getSecret() { return secret; }

    public static class ApplicationCredentials extends ListResult<KeystoneApplicationCredential> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("application_credentials")
        private List<KeystoneApplicationCredential> list;

        @Override
        protected List<KeystoneApplicationCredential> value() {
            return list;
        }
    }
}
