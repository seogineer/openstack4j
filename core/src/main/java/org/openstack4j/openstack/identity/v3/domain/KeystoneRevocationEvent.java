package org.openstack4j.openstack.identity.v3.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.identity.v3.RevocationEvent;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneRevocationEvent implements RevocationEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty("user_id") private String userId;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("domain_id") private String domainId;
    @JsonProperty("audit_id") private String auditId;
    @JsonProperty("audit_chain_id") private String auditChainId;
    @JsonProperty("role_id") private String roleId;
    @JsonProperty("trust_id") private String trustId;
    @JsonProperty("consumer_id") private String consumerId;
    @JsonProperty("access_token_id") private String accessTokenId;
    @JsonProperty("expires_at") private String expiresAt;
    @JsonProperty("issued_before") private Date issuedBefore;
    @JsonProperty("revoked_at") private Date revokedAt;

    @Override public String getUserId() { return userId; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getDomainId() { return domainId; }
    @Override public String getAuditId() { return auditId; }
    @Override public String getAuditChainId() { return auditChainId; }
    @Override public String getRoleId() { return roleId; }
    @Override public String getTrustId() { return trustId; }
    @Override public String getConsumerId() { return consumerId; }
    @Override public String getAccessTokenId() { return accessTokenId; }
    @Override public String getExpiresAt() { return expiresAt; }
    @Override public Date getIssuedBefore() { return issuedBefore; }
    @Override public Date getRevokedAt() { return revokedAt; }

    public static class RevocationEvents extends ListResult<KeystoneRevocationEvent> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("events")
        private List<KeystoneRevocationEvent> list;

        @Override
        protected List<KeystoneRevocationEvent> value() {
            return list;
        }
    }
}
