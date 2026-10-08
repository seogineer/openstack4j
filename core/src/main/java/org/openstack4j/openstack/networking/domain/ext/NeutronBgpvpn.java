package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.Bgpvpn;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("bgpvpn")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronBgpvpn implements Bgpvpn {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("type") private String type;
    @JsonProperty("route_targets") private List<String> routeTargets;
    @JsonProperty("import_targets") private List<String> importTargets;
    @JsonProperty("export_targets") private List<String> exportTargets;
    @JsonProperty("route_distinguishers") private List<String> routeDistinguishers;
    @JsonProperty("networks") private List<String> networks;
    @JsonProperty("routers") private List<String> routers;
    @JsonProperty("ports") private List<String> ports;
    @JsonProperty("vni") private Integer vni;
    @JsonProperty("local_pref") private Long localPref;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getType() { return type; }
    @Override public List<String> getRouteTargets() { return routeTargets; }
    @Override public List<String> getImportTargets() { return importTargets; }
    @Override public List<String> getExportTargets() { return exportTargets; }
    @Override public List<String> getRouteDistinguishers() { return routeDistinguishers; }
    @Override public List<String> getNetworks() { return networks; }
    @Override public List<String> getRouters() { return routers; }
    @Override public List<String> getPorts() { return ports; }
    @Override public Integer getVni() { return vni; }
    @Override public Long getLocalPref() { return localPref; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronBgpvpnList extends ListResult<NeutronBgpvpn> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("bgpvpns")
        private List<NeutronBgpvpn> list;

        @Override
        protected List<NeutronBgpvpn> value() {
            return list;
        }
    }
}
