package org.openstack4j.api.trove.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.trove.ext.Configuration;
import org.openstack4j.model.trove.ext.options.ConfigurationOptions;

/** Configuration groups ({@code /v1.0/{project_id}/configurations}). */
public interface ConfigurationGroupService extends RestService {

    /** @return the configuration groups */
    List<? extends Configuration> list();

    /** @param filters query parameters such as {@code name}, {@code project_id} */
    List<? extends Configuration> list(Map<String, String> filters);

    /** @return the configuration group, or {@code null} when it does not exist */
    Configuration get(String id);

    Configuration create(ConfigurationOptions options);

    /** Replaces the group's values (and name/description when set); Trove answers 202 without a body. */
    ActionResponse update(String id, ConfigurationOptions options);

    /** Merges the given parameter values into the group ({@code PATCH}); the other values stay. */
    ActionResponse patchValues(String id, Map<String, Object> values);

    /** @return the instances the group is attached to ({@code id}, {@code name}); a missing group raises */
    List<Map<String, Object>> listInstances(String id);

    ActionResponse delete(String id);
}
