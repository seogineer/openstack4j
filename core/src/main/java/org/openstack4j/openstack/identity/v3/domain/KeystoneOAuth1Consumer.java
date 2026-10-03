package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.OAuth1Consumer;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("consumer")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneOAuth1Consumer implements OAuth1Consumer {

    private static final long serialVersionUID = 1L;

    private String id;
    private String description;
    private String secret;

    @Override public String getId() { return id; }
    @Override public String getDescription() { return description; }
    @Override public String getSecret() { return secret; }

    public static class Consumers extends ListResult<KeystoneOAuth1Consumer> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("consumers")
        private List<KeystoneOAuth1Consumer> list;

        @Override
        protected List<KeystoneOAuth1Consumer> value() {
            return list;
        }
    }
}
