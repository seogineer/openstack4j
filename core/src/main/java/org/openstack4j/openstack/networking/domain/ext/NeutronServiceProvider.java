package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.network.ext.ServiceProvider;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronServiceProvider implements ServiceProvider {

    private static final long serialVersionUID = 1L;

    @JsonProperty("service_type") private String serviceType;
    @JsonProperty("name") private String name;
    @JsonProperty("default") private Boolean defaultValue;

    @Override public String getServiceType() { return serviceType; }
    @Override public String getName() { return name; }
    @Override public Boolean isDefault() { return defaultValue; }

    public static class ServiceProviders extends ListResult<NeutronServiceProvider> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("service_providers")
        private List<NeutronServiceProvider> list;

        @Override
        protected List<NeutronServiceProvider> value() {
            return list;
        }
    }
}
