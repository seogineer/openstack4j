package org.openstack4j.model.network.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A QoS rule type and the drivers that support it. */
public interface QosRuleType extends ModelEntity {
    String getType();
    List<Map<String, Object>> getDrivers();
}
