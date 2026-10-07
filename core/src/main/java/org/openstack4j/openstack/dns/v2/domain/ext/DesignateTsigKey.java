package org.openstack4j.openstack.dns.v2.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.dns.v2.ext.TsigKey;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DesignateTsigKey implements TsigKey {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("algorithm") private String algorithm;
    @JsonProperty("secret") private String secret;
    @JsonProperty("scope") private String scope;
    @JsonProperty("resource_id") private String resourceId;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getAlgorithm() { return algorithm; }
    @Override public String getSecret() { return secret; }
    @Override public String getScope() { return scope; }
    @Override public String getResourceId() { return resourceId; }
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

    public static class DesignateTsigKeyList extends ListResult<DesignateTsigKey> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("tsigkeys")
        private List<DesignateTsigKey> list;

        @Override
        protected List<DesignateTsigKey> value() {
            return list;
        }
    }
}
