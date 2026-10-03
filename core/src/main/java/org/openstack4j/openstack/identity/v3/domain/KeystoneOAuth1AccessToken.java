package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.OAuth1AccessToken;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("access_token")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneOAuth1AccessToken implements OAuth1AccessToken {

    private static final long serialVersionUID = 1L;

    private String id;
    @JsonProperty("consumer_id") private String consumerId;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("authorizing_user_id") private String authorizingUserId;
    @JsonProperty("expires_at") private String expiresAt;

    @Override public String getId() { return id; }
    @Override public String getConsumerId() { return consumerId; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getAuthorizingUserId() { return authorizingUserId; }
    @Override public String getExpiresAt() { return expiresAt; }

    public static class AccessTokens extends ListResult<KeystoneOAuth1AccessToken> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("access_tokens")
        private List<KeystoneOAuth1AccessToken> list;

        @Override
        protected List<KeystoneOAuth1AccessToken> value() {
            return list;
        }
    }
}
