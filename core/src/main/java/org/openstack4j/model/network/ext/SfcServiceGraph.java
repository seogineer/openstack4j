package org.openstack4j.model.network.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An SFC service graph (dependencies between port chains). Fields without a getter are in getAttributes(). */
public interface SfcServiceGraph extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    Map<String, List<String>> getPortChains();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
