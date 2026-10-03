package org.openstack4j.model.network.options;

import java.util.List;
import java.util.Objects;

/** Body of a subnet pool create or update. */
public class SubnetPoolOptions extends NeutronAttributes<SubnetPoolOptions> {

    public static SubnetPoolOptions create(String name, List<String> prefixes) {
        return new SubnetPoolOptions().name(Objects.requireNonNull(name)).prefixes(Objects.requireNonNull(prefixes));
    }

    public static SubnetPoolOptions update() {
        return new SubnetPoolOptions();
    }

    @Override
    protected SubnetPoolOptions self() {
        return this;
    }

    public SubnetPoolOptions name(String name) { return put("name", name); }
    public SubnetPoolOptions description(String description) { return put("description", description); }
    public SubnetPoolOptions prefixes(List<String> prefixes) { return put("prefixes", prefixes); }
    public SubnetPoolOptions defaultPrefixlen(Integer length) { return put("default_prefixlen", length); }
    public SubnetPoolOptions minPrefixlen(Integer length) { return put("min_prefixlen", length); }
    public SubnetPoolOptions maxPrefixlen(Integer length) { return put("max_prefixlen", length); }
    public SubnetPoolOptions defaultQuota(Integer quota) { return put("default_quota", quota); }
    public SubnetPoolOptions addressScopeId(String addressScopeId) { return put("address_scope_id", addressScopeId); }
    public SubnetPoolOptions shared(Boolean shared) { return put("shared", shared); }
    public SubnetPoolOptions isDefault(Boolean isDefault) { return put("is_default", isDefault); }
    public SubnetPoolOptions projectId(String projectId) { return put("project_id", projectId); }
}
