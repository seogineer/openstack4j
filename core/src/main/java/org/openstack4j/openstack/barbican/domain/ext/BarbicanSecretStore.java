package org.openstack4j.openstack.barbican.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.barbican.ext.SecretStore;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BarbicanSecretStore implements SecretStore {

    private static final long serialVersionUID = 1L;

    @JsonProperty("secret_store_ref") private String secretStoreRef;
    @JsonProperty("name") private String name;
    @JsonProperty("status") private String status;
    @JsonProperty("global_default") private Boolean globalDefault;
    @JsonProperty("secret_store_plugin") private String secretStorePlugin;
    @JsonProperty("crypto_plugin") private String cryptoPlugin;
    @JsonProperty("created") private String created;
    @JsonProperty("updated") private String updated;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getSecretStoreRef() { return secretStoreRef; }
    @Override public String getName() { return name; }
    @Override public String getStatus() { return status; }
    @Override public Boolean isGlobalDefault() { return globalDefault; }
    @Override public String getSecretStorePlugin() { return secretStorePlugin; }
    @Override public String getCryptoPlugin() { return cryptoPlugin; }
    @Override public String getCreated() { return created; }
    @Override public String getUpdated() { return updated; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class BarbicanSecretStoreList extends ListResult<BarbicanSecretStore> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("secret_stores")
        private List<BarbicanSecretStore> list;

        @Override
        protected List<BarbicanSecretStore> value() {
            return list;
        }
    }
}
