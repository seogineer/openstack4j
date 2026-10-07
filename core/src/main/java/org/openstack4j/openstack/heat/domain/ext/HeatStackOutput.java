package org.openstack4j.openstack.heat.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.heat.ext.StackOutput;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("output")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeatStackOutput implements StackOutput {

    private static final long serialVersionUID = 1L;

    @JsonProperty("output_key") private String outputKey;
    @JsonProperty("output_value") private Object outputValue;
    @JsonProperty("description") private String description;
    @JsonProperty("output_error") private String outputError;

    @Override public String getOutputKey() { return outputKey; }
    @Override public Object getOutputValue() { return outputValue; }
    @Override public String getDescription() { return description; }
    @Override public String getOutputError() { return outputError; }

    public static class Outputs extends ListResult<HeatStackOutput> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("outputs")
        private List<HeatStackOutput> list;

        @Override
        protected List<HeatStackOutput> value() {
            return list;
        }
    }
}
