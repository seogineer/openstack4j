package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.NeutronExtension;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("extension")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronExtensionEntity implements NeutronExtension {

    private static final long serialVersionUID = 1L;

    @JsonProperty("alias") private String alias;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("updated") private String updated;

    @Override public String getAlias() { return alias; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getUpdated() { return updated; }

    public static class Extensions extends ListResult<NeutronExtensionEntity> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("extensions")
        private List<NeutronExtensionEntity> list;

        @Override
        protected List<NeutronExtensionEntity> value() {
            return list;
        }
    }
}
