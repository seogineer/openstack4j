package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.SubnetPool;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("subnetpool")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronSubnetPool implements SubnetPool {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("prefixes") private List<String> prefixes;
    @JsonProperty("default_prefixlen") private Integer defaultPrefixlen;
    @JsonProperty("min_prefixlen") private Integer minPrefixlen;
    @JsonProperty("max_prefixlen") private Integer maxPrefixlen;
    @JsonProperty("default_quota") private Integer defaultQuota;
    @JsonProperty("address_scope_id") private String addressScopeId;
    @JsonProperty("ip_version") private Integer ipVersion;
    @JsonProperty("shared") private Boolean shared;
    @JsonProperty("is_default") private Boolean isDefault;
    @JsonProperty("revision_number") private Integer revisionNumber;
    @JsonProperty("tags") private List<String> tags;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getProjectId() { return projectId; }
    @Override public List<String> getPrefixes() { return prefixes; }
    @Override public Integer getDefaultPrefixlen() { return defaultPrefixlen; }
    @Override public Integer getMinPrefixlen() { return minPrefixlen; }
    @Override public Integer getMaxPrefixlen() { return maxPrefixlen; }
    @Override public Integer getDefaultQuota() { return defaultQuota; }
    @Override public String getAddressScopeId() { return addressScopeId; }
    @Override public Integer getIpVersion() { return ipVersion; }
    @Override public Boolean isShared() { return shared; }
    @Override public Boolean isDefault() { return isDefault; }
    @Override public Integer getRevisionNumber() { return revisionNumber; }
    @Override public List<String> getTags() { return tags; }

    public static class SubnetPools extends ListResult<NeutronSubnetPool> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("subnetpools")
        private List<NeutronSubnetPool> list;

        @Override
        protected List<NeutronSubnetPool> value() {
            return list;
        }
    }
}
