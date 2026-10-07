package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.octavia.ext.OctaviaProvider;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaProviderEntity implements OctaviaProvider {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;

    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }

    public static class Providers extends ListResult<OctaviaProviderEntity> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("providers")
        private List<OctaviaProviderEntity> list;

        @Override
        protected List<OctaviaProviderEntity> value() {
            return list;
        }
    }
}
