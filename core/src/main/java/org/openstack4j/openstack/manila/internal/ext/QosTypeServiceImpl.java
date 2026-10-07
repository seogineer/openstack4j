package org.openstack4j.openstack.manila.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.QosTypeService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.QosType;
import org.openstack4j.model.manila.ext.options.QosTypeCreate;
import org.openstack4j.model.manila.ext.options.QosTypeUpdate;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaQosType;
import org.openstack4j.openstack.manila.domain.ext.ManilaQosType.ManilaQosTypeList;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;

public class QosTypeServiceImpl extends BaseManilaExtService implements QosTypeService {

    private static final MicroVersion FLOOR = ManilaMicroVersions.V(94);

    @Override
    public List<? extends QosType> list() {
        return list(null);
    }

    @Override
    public List<? extends QosType> list(Map<String, String> filters) {
        return listOf(FLOOR, ManilaQosTypeList.class, "/qos-types", filters);
    }

    @Override
    public QosType get(String id) {
        return show(FLOOR, ManilaQosType.class, "/qos-types/" + id(id));
    }

    @Override
    public QosType create(QosTypeCreate create) {
        return create(FLOOR, ManilaQosType.class, "/qos-types", "qos_type", create);
    }

    @Override
    public QosType update(String id, QosTypeUpdate update) {
        return update(FLOOR, ManilaQosType.class, "/qos-types/" + id(id), "qos_type", update);
    }

    @Override
    public Map<String, String> getSpecs(String id) {
        return strings(showStrict(FLOOR, Map.class, "/qos-types/" + id(id) + "/specs"), "specs");
    }

    @Override
    public Map<String, String> setSpecs(String id, Map<String, ?> specs) {
        String path = "/qos-types/" + id(id) + "/specs";
        return strings(at(FLOOR, post(Map.class, path), path).entity(org.openstack4j.openstack.internal.microversion.JsonBody.of("specs", java.util.Objects.requireNonNull(specs, "specs"))).execute(propagate404()), "specs");
    }

    @Override
    public ActionResponse unsetSpec(String id, String key) {
        return remove(FLOOR, "/qos-types/" + id(id) + "/specs/" + id(key));
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(FLOOR, "/qos-types/" + id(id));
    }
}
