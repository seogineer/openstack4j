package org.openstack4j.openstack.heat.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.heat.ext.TemplateFunction;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HeatTemplateFunction implements TemplateFunction {

    private static final long serialVersionUID = 1L;

    @JsonProperty("functions") private String functions;
    @JsonProperty("description") private String description;

    @Override public String getFunctions() { return functions; }
    @Override public String getDescription() { return description; }

    public static class TemplateFunctions extends ListResult<HeatTemplateFunction> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("template_functions")
        private List<HeatTemplateFunction> list;

        @Override
        protected List<HeatTemplateFunction> value() {
            return list;
        }
    }
}
