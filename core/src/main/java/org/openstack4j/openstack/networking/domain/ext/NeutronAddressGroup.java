package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.AddressGroup;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("address_group")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronAddressGroup implements AddressGroup {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("addresses") private List<String> addresses;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getProjectId() { return projectId; }
    @Override public List<String> getAddresses() { return addresses; }

    public static class AddressGroups extends ListResult<NeutronAddressGroup> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("address_groups")
        private List<NeutronAddressGroup> list;

        @Override
        protected List<NeutronAddressGroup> value() {
            return list;
        }
    }
}
