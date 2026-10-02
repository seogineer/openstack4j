package org.openstack4j.model.storage.block;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A QoS specification ({@code /qos-specs}). */
public interface QosSpec extends ModelEntity {
    String getId();
    String getName();
    /** @return front-end, back-end or both */
    String getConsumer();
    Map<String, String> getSpecs();
}
