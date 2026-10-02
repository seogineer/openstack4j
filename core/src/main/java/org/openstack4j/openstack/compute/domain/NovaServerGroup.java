package org.openstack4j.openstack.compute.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.util.ToStringHelper;
import org.openstack4j.model.compute.ServerGroup;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("server_group")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaServerGroup implements ServerGroup {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private List<String> members;
    private Map<String, String> metadata;
    @JsonProperty("policies")
    private List<String> policies;
    private String policy;
    private Map<String, Object> rules;
    @JsonProperty("project_id")
    private String projectId;
    @JsonProperty("user_id")
    private String userId;

    public static NovaServerGroup create(String name, String policy, Map<String, Object> rules) {
        NovaServerGroup ns = new NovaServerGroup();
        ns.name = name;
        ns.policy = policy;
        ns.rules = rules;
        return ns;
    }

    @Override
    public String getPolicy() {
        return policy;
    }

    @Override
    public Map<String, Object> getRules() {
        return rules;
    }

    @Override
    public String getProjectId() {
        return projectId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    public static NovaServerGroup create(String name, String policy) {
        NovaServerGroup ns = new NovaServerGroup();
        List<String> policyList = new ArrayList<String>();
        policyList.add(policy);
        ns.name = name;
        ns.policies = policyList;
        return ns;
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<String> getMembers() {
        return members;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    @JsonIgnore
    public List<String> getPolicies() {
        if (policies == null && policy != null)
            return Collections.singletonList(policy);    // 2.64+ returns a single policy
        return policies;
    }

    public void setPolicies(List<String> policies) {
        this.policies = policies;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
                .add("id", id).add("name", name).add("members", members)
                .add("policies", policies).add("metadata", metadata)
                .toString();
    }

    public static class ServerGroups extends ListResult<NovaServerGroup> {

        private static final long serialVersionUID = 1L;

        @JsonProperty("server_groups")
        private List<NovaServerGroup> serverGroups;

        @Override
        protected List<NovaServerGroup> value() {
            return serverGroups;
        }

    }


}
