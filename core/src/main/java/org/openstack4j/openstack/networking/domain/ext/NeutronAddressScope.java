package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.AddressScope;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("address_scope")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronAddressScope implements AddressScope {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("ip_version") private Integer ipVersion;
    @JsonProperty("shared") private Boolean shared;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getProjectId() { return projectId; }
    @Override public Integer getIpVersion() { return ipVersion; }
    @Override public Boolean isShared() { return shared; }

    public static class AddressScopes extends ListResult<NeutronAddressScope> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("address_scopes")
        private List<NeutronAddressScope> list;

        @Override
        protected List<NeutronAddressScope> value() {
            return list;
        }
    }
}
