package org.openstack4j.openstack.heat.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.heat.ext.TemplateVersion;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HeatTemplateVersion implements TemplateVersion {

    private static final long serialVersionUID = 1L;

    @JsonProperty("version") private String version;
    @JsonProperty("type") private String type;
    @JsonProperty("aliases") private List<String> aliases;

    @Override public String getVersion() { return version; }
    @Override public String getType() { return type; }
    @Override public List<String> getAliases() { return aliases; }

    public static class TemplateVersions extends ListResult<HeatTemplateVersion> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("template_versions")
        private List<HeatTemplateVersion> list;

        @Override
        protected List<HeatTemplateVersion> value() {
            return list;
        }
    }
}
