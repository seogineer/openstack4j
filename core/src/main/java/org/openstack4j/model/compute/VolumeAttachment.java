package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/**
 * Provides volume attachment result
 *
 * @author Octopus Zhang
 */
public interface VolumeAttachment extends ModelEntity {

    /**
     * the device name in the server,like /dev/vdd
     *
     * @return device name
     */
    String getDevice();

    /**
     * Gets the id.
     *
     * @return the id
     */
    String getId();

    /**
     * the server's id int this attachment
     *
     * @return the id
     */
    String getServerId();

    /**
     * the volume's id int this attachment
     *
     * @return the id
     */
    String getVolumeId();

    /** @return attachment_id (2.89+) */
    default String getAttachmentId() { return null; }
    /** @return bdm_uuid (2.89+) */
    default String getBdmUuid() { return null; }
    /** @return tag (2.70+) */
    default String getTag() { return null; }
    /** @return delete_on_termination (2.79+) */
    default Boolean getDeleteOnTermination() { return null; }
}
