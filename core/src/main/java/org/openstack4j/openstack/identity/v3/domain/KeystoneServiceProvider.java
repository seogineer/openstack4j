package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.ServiceProvider;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("service_provider")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneServiceProvider implements ServiceProvider {

    private static final long serialVersionUID = 1L;

    private String id;
    private String description;
    private Boolean enabled;
    @JsonProperty("auth_url") private String authUrl;
    @JsonProperty("sp_url") private String spUrl;
    @JsonProperty("relay_state_prefix") private String relayStatePrefix;

    @Override public String getId() { return id; }
    @Override public String getDescription() { return description; }
    @Override public Boolean isEnabled() { return enabled; }
    @Override public String getAuthUrl() { return authUrl; }
    @Override public String getSpUrl() { return spUrl; }
    @Override public String getRelayStatePrefix() { return relayStatePrefix; }

    public static class ServiceProviders extends ListResult<KeystoneServiceProvider> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("service_providers")
        private List<KeystoneServiceProvider> list;

        @Override
        protected List<KeystoneServiceProvider> value() {
            return list;
        }
    }
}
