package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.IpsecPolicy;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("ipsecpolicy")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronIpsecPolicy implements IpsecPolicy {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("transform_protocol") private String transformProtocol;
    @JsonProperty("auth_algorithm") private String authAlgorithm;
    @JsonProperty("encapsulation_mode") private String encapsulationMode;
    @JsonProperty("encryption_algorithm") private String encryptionAlgorithm;
    @JsonProperty("pfs") private String pfs;
    @JsonProperty("lifetime") private Map<String, Object> lifetime;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getTransformProtocol() { return transformProtocol; }
    @Override public String getAuthAlgorithm() { return authAlgorithm; }
    @Override public String getEncapsulationMode() { return encapsulationMode; }
    @Override public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
    @Override public String getPfs() { return pfs; }
    @Override public Map<String, Object> getLifetime() { return lifetime; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronIpsecPolicyList extends ListResult<NeutronIpsecPolicy> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("ipsecpolicies")
        private List<NeutronIpsecPolicy> list;

        @Override
        protected List<NeutronIpsecPolicy> value() {
            return list;
        }
    }
}
