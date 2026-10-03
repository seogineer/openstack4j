package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters for {@code GET /v3/projects}. */
public class ProjectListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();

    public static ProjectListOptions create() {
        return new ProjectListOptions();
    }

    private ProjectListOptions put(String key, Object value) {
        params.put(key, String.valueOf(value));
        return this;
    }

    public ProjectListOptions name(String name) { return put("name", name); }
    public ProjectListOptions domainId(String domainId) { return put("domain_id", domainId); }
    public ProjectListOptions parentId(String parentId) { return put("parent_id", parentId); }
    public ProjectListOptions enabled(boolean enabled) { return put("enabled", enabled); }
    public ProjectListOptions isDomain(boolean isDomain) { return put("is_domain", isDomain); }
    /** Projects that have all of these tags. */
    public ProjectListOptions tags(String... tags) { return put("tags", String.join(",", tags)); }
    /** Projects that have at least one of these tags. */
    public ProjectListOptions tagsAny(String... tags) { return put("tags-any", String.join(",", tags)); }
    /** Projects that do not have all of these tags. */
    public ProjectListOptions notTags(String... tags) { return put("not-tags", String.join(",", tags)); }
    /** Projects that have none of these tags. */
    public ProjectListOptions notTagsAny(String... tags) { return put("not-tags-any", String.join(",", tags)); }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }
}
