package org.openstack4j.openstack.trove.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.trove.ext.ConfigurationGroupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.trove.ext.Configuration;
import org.openstack4j.model.trove.ext.options.ConfigurationOptions;
import org.openstack4j.openstack.trove.domain.ext.TroveConfiguration;
import org.openstack4j.openstack.trove.domain.ext.TroveConfiguration.TroveConfigurationList;

public class ConfigurationGroupServiceImpl extends BaseTroveExtService implements ConfigurationGroupService {

    private static final String PATH = "/configurations";
    private static final String ROOT = "configuration";

    @Override
    public List<? extends Configuration> list() {
        return list(null);
    }

    @Override
    public List<? extends Configuration> list(Map<String, String> filters) {
        return listOf(TroveConfigurationList.class, PATH, filters);
    }

    @Override
    public Configuration get(String id) {
        return show(TroveConfiguration.class, PATH + "/" + id(id));
    }

    @Override
    public Configuration create(ConfigurationOptions options) {
        return create(TroveConfiguration.class, PATH, ROOT, options);
    }

    @Override
    public ActionResponse update(String id, ConfigurationOptions options) {
        if (!java.util.Objects.requireNonNull(options, "options").toMap().containsKey("values"))
            throw new IllegalArgumentException("A configuration group update replaces all values; set values (or use patchValues)");
        return putWithResponse(PATH + "/" + id(id)).entity(org.openstack4j.openstack.internal.microversion.JsonBody.of(ROOT, java.util.Objects.requireNonNull(options, "options").toMap())).execute();
    }

    @Override
    public ActionResponse patchValues(String id, Map<String, Object> values) {
        return patchWithResponse(PATH + "/" + id(id)).entity(org.openstack4j.openstack.internal.microversion.JsonBody.of("configuration", Map.of("values", java.util.Objects.requireNonNull(values, "values")))).execute();
    }

    @Override
    public List<Map<String, Object>> listInstances(String id) {
        return listOf(PATH + "/" + id(id) + "/instances", "instances", null);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
