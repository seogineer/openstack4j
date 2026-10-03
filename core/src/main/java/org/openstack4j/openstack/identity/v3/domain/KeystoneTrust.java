package org.openstack4j.openstack.identity.v3.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.Trust;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("trust")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneTrust implements Trust {

    private static final long serialVersionUID = 1L;

    private String id;
    @JsonProperty("trustor_user_id") private String trustorUserId;
    @JsonProperty("trustee_user_id") private String trusteeUserId;
    @JsonProperty("project_id") private String projectId;
    private Boolean impersonation;
    @JsonProperty("expires_at") private Date expiresAt;
    @JsonProperty("remaining_uses") private Integer remainingUses;
    @JsonProperty("redelegation_count") private Integer redelegationCount;
    @JsonProperty("redelegated_trust_id") private String redelegatedTrustId;
    private List<KeystoneRole> roles;

    @Override public String getId() { return id; }
    @Override public String getTrustorUserId() { return trustorUserId; }
    @Override public String getTrusteeUserId() { return trusteeUserId; }
    @Override public String getProjectId() { return projectId; }
    @Override public Boolean getImpersonation() { return impersonation; }
    @Override public Date getExpiresAt() { return expiresAt; }
    @Override public Integer getRemainingUses() { return remainingUses; }
    @Override public Integer getRedelegationCount() { return redelegationCount; }
    @Override public String getRedelegatedTrustId() { return redelegatedTrustId; }
    @Override public List<KeystoneRole> getRoles() { return roles; }

    public static class Trusts extends ListResult<KeystoneTrust> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("trusts")
        private List<KeystoneTrust> list;

        @Override
        protected List<KeystoneTrust> value() {
            return list;
        }
    }
}
