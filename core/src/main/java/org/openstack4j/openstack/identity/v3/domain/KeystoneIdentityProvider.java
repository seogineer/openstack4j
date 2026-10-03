package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.IdentityProvider;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("identity_provider")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneIdentityProvider implements IdentityProvider {

    private static final long serialVersionUID = 1L;

    private String id;
    private String description;
    private Boolean enabled;
    @JsonProperty("domain_id") private String domainId;
    @JsonProperty("remote_ids") private List<String> remoteIds;
    @JsonProperty("authorization_ttl") private Integer authorizationTtl;

    @Override public String getId() { return id; }
    @Override public String getDescription() { return description; }
    @Override public Boolean isEnabled() { return enabled; }
    @Override public String getDomainId() { return domainId; }
    @Override public List<String> getRemoteIds() { return remoteIds; }
    @Override public Integer getAuthorizationTtl() { return authorizationTtl; }

    public static class IdentityProviders extends ListResult<KeystoneIdentityProvider> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("identity_providers")
        private List<KeystoneIdentityProvider> list;

        @Override
        protected List<KeystoneIdentityProvider> value() {
            return list;
        }
    }
}
