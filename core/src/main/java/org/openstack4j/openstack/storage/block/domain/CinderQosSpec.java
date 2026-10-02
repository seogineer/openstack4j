package org.openstack4j.openstack.storage.block.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.model.storage.block.QosSpec;

@JsonRootName("qos_specs")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderQosSpec implements QosSpec {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String consumer;
    private Map<String, String> specs;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getConsumer() { return consumer; }
    @Override public Map<String, String> getSpecs() { return specs; }

    public static class QosSpecs extends ListResult<CinderQosSpec> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("qos_specs")
        private List<CinderQosSpec> specs;

        @Override
        protected List<CinderQosSpec> value() {
            return specs;
        }
    }
}
