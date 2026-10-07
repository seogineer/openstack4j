package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.QosType;
import org.openstack4j.model.manila.ext.options.QosTypeCreate;
import org.openstack4j.model.manila.ext.options.QosTypeUpdate;

/** QoS types ({@code /v2/qos-types}, microversion 2.94). */
public interface QosTypeService extends RestService {

    /** @return the QoS types */
    List<? extends QosType> list();

    /** @param filters query parameters such as {@code name}, {@code limit}, {@code offset} */
    List<? extends QosType> list(Map<String, String> filters);

    /** @return the QoS type, or {@code null} when it does not exist */
    QosType get(String id);

    QosType create(QosTypeCreate create);

    QosType update(String id, QosTypeUpdate update);

    /** @return the QoS type's specs; a missing type raises */
    Map<String, String> getSpecs(String id);

    /** Adds or changes specs and returns them. */
    Map<String, String> setSpecs(String id, Map<String, ?> specs);

    ActionResponse unsetSpec(String id, String key);

    ActionResponse delete(String id);
}
