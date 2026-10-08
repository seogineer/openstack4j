package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.BgpvpnRouterAssociation;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("router_association")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronBgpvpnRouterAssociation implements BgpvpnRouterAssociation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("router_id") private String routerId;
    @JsonProperty("advertise_extra_routes") private Boolean advertiseExtraRoutes;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getRouterId() { return routerId; }
    @Override public Boolean isAdvertiseExtraRoutes() { return advertiseExtraRoutes; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronBgpvpnRouterAssociationList extends ListResult<NeutronBgpvpnRouterAssociation> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("router_associations")
        private List<NeutronBgpvpnRouterAssociation> list;

        @Override
        protected List<NeutronBgpvpnRouterAssociation> value() {
            return list;
        }
    }
}
