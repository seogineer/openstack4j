package org.openstack4j.model.network.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A range of segmentation ids project networks may use. */
public interface NetworkSegmentRange extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    Boolean isDefault();
    Boolean isShared();
    String getProjectId();
    String getNetworkType();
    String getPhysicalNetwork();
    Integer getMinimum();
    Integer getMaximum();
    List<Integer> getAvailable();
    Map<String, String> getUsed();
}
