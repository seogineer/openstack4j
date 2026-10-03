package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.SecurityGroupDefaultStatefulness;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("security_groups_default_statefulness")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronSecurityGroupDefaultStatefulness implements SecurityGroupDefaultStatefulness {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("stateful") private Boolean stateful;

    @Override public String getId() { return id; }
    @Override public String getProjectId() { return projectId; }
    @Override public Boolean isStateful() { return stateful; }

    public static class Items extends ListResult<NeutronSecurityGroupDefaultStatefulness> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("security_groups_default_statefulness")
        private List<NeutronSecurityGroupDefaultStatefulness> list;

        @Override
        protected List<NeutronSecurityGroupDefaultStatefulness> value() {
            return list;
        }
    }
}
