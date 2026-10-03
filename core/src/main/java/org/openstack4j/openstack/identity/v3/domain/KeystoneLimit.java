package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.Limit;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("limit")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneLimit implements Limit {

    private static final long serialVersionUID = 1L;

    private String id;
    @JsonProperty("service_id") private String serviceId;
    @JsonProperty("region_id") private String regionId;
    @JsonProperty("resource_name") private String resourceName;
    @JsonProperty("resource_limit") private Integer resourceLimit;
    private String description;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("domain_id") private String domainId;

    @Override public String getId() { return id; }
    @Override public String getServiceId() { return serviceId; }
    @Override public String getRegionId() { return regionId; }
    @Override public String getResourceName() { return resourceName; }
    @Override public Integer getResourceLimit() { return resourceLimit; }
    @Override public String getDescription() { return description; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getDomainId() { return domainId; }

    public static class Limits extends ListResult<KeystoneLimit> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("limits")
        private List<KeystoneLimit> list;

        @Override
        protected List<KeystoneLimit> value() {
            return list;
        }
    }
}
