package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.FederationProtocol;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("protocol")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneFederationProtocol implements FederationProtocol {

    private static final long serialVersionUID = 1L;

    private String id;
    @JsonProperty("mapping_id") private String mappingId;
    @JsonProperty("remote_id_attribute") private String remoteIdAttribute;

    @Override public String getId() { return id; }
    @Override public String getMappingId() { return mappingId; }
    @Override public String getRemoteIdAttribute() { return remoteIdAttribute; }

    public static class Protocols extends ListResult<KeystoneFederationProtocol> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("protocols")
        private List<KeystoneFederationProtocol> list;

        @Override
        protected List<KeystoneFederationProtocol> value() {
            return list;
        }
    }
}
