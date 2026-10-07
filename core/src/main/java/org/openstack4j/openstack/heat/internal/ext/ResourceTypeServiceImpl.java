package org.openstack4j.openstack.heat.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.heat.ext.ResourceTypeService;
import org.openstack4j.openstack.heat.domain.ext.HeatResourceTypes;

public class ResourceTypeServiceImpl extends BaseHeatExtService implements ResourceTypeService {

    @Override
    public List<String> list() {
        return list(null);
    }

    @Override
    public List<String> list(Map<String, String> filters) {
        HeatResourceTypes types = get(HeatResourceTypes.class, "/resource_types").params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        return types == null ? Collections.emptyList() : types.getTypes();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> schema(String type) {
        Map<String, Object> schema = showStrict(Map.class, "/resource_types/" + id(type));
        return schema == null ? Collections.emptyMap() : schema;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> template(String type, String templateType) {
        Map<String, Object> template = get(Map.class, "/resource_types/" + id(type) + "/template")
                .param(templateType != null, "template_type", templateType).execute(propagate404());
        return template == null ? Collections.emptyMap() : template;
    }
}
