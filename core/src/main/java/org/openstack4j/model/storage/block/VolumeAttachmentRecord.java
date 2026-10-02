package org.openstack4j.model.storage.block;

import java.util.Date;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A volume attachment managed through the {@code /attachments} API (3.27+). */
public interface VolumeAttachmentRecord extends ModelEntity {
    String getId();
    String getStatus();
    /** @return the server (instance) uuid, or {@code null} */
    String getInstance();
    String getVolumeId();
    Date getAttachedAt();
    Date getDetachedAt();
    /** @return rw or ro (3.54+) */
    String getAttachMode();
    /** @return driver connection details, present after the connector was given */
    Map<String, Object> getConnectionInfo();
}
