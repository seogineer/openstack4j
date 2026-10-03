package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.Mapping;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("mapping")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneMapping implements Mapping {

    private static final long serialVersionUID = 1L;

    private String id;
    private List<Map<String, Object>> rules;
    @JsonProperty("schema_version") private String schemaVersion;

    @Override public String getId() { return id; }
    @Override public List<Map<String, Object>> getRules() { return rules; }
    @Override public String getSchemaVersion() { return schemaVersion; }

    public static class Mappings extends ListResult<KeystoneMapping> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("mappings")
        private List<KeystoneMapping> list;

        @Override
        protected List<KeystoneMapping> value() {
            return list;
        }
    }
}
