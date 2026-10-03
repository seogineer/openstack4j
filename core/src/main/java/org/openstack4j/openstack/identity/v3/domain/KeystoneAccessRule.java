package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.AccessRule;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("access_rule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneAccessRule implements AccessRule {

    private static final long serialVersionUID = 1L;

    private String id;
    private String service;
    private String path;
    private String method;

    @Override public String getId() { return id; }
    @Override public String getService() { return service; }
    @Override public String getPath() { return path; }
    @Override public String getMethod() { return method; }

    public static class AccessRules extends ListResult<KeystoneAccessRule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("access_rules")
        private List<KeystoneAccessRule> list;

        @Override
        protected List<KeystoneAccessRule> value() {
            return list;
        }
    }
}
