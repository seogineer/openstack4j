package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.RegisteredLimit;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("registered_limit")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneRegisteredLimit implements RegisteredLimit {

    private static final long serialVersionUID = 1L;

    private String id;
    @JsonProperty("service_id") private String serviceId;
    @JsonProperty("region_id") private String regionId;
    @JsonProperty("resource_name") private String resourceName;
    @JsonProperty("default_limit") private Integer defaultLimit;
    private String description;

    @Override public String getId() { return id; }
    @Override public String getServiceId() { return serviceId; }
    @Override public String getRegionId() { return regionId; }
    @Override public String getResourceName() { return resourceName; }
    @Override public Integer getDefaultLimit() { return defaultLimit; }
    @Override public String getDescription() { return description; }

    public static class RegisteredLimits extends ListResult<KeystoneRegisteredLimit> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("registered_limits")
        private List<KeystoneRegisteredLimit> list;

        @Override
        protected List<KeystoneRegisteredLimit> value() {
            return list;
        }
    }
}
