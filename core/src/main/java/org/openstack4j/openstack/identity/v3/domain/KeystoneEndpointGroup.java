package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.EndpointGroup;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("endpoint_group")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneEndpointGroup implements EndpointGroup {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private Map<String, Object> filters;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public Map<String, Object> getFilters() { return filters; }

    public static class EndpointGroups extends ListResult<KeystoneEndpointGroup> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("endpoint_groups")
        private List<KeystoneEndpointGroup> list;

        @Override
        protected List<KeystoneEndpointGroup> value() {
            return list;
        }
    }
}
