package org.openstack4j.model.identity.v3;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A federation mapping: rules that turn remote attributes into local users and groups. */
public interface Mapping extends ModelEntity {
    String getId();
    List<Map<String, Object>> getRules();
    String getSchemaVersion();
}
