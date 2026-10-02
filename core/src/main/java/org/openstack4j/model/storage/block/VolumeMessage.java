package org.openstack4j.model.storage.block;

import java.util.Date;
import java.util.List;

import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.common.Link;

/** A user-facing fault message ({@code /messages}, 3.3+). */
public interface VolumeMessage extends ModelEntity {
    String getId();
    String getEventId();
    String getUserMessage();
    String getMessageLevel();
    String getResourceType();
    String getResourceUuid();
    String getRequestId();
    Date getCreatedAt();
    Date getGuaranteedUntil();
    List<? extends Link> getLinks();
}
