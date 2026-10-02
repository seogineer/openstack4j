package org.openstack4j.model.storage.block.options;

/** Filters for {@code GET /attachments[/detail]} (3.27+). */
public class AttachmentListOptions extends BlockStorageListOptions<AttachmentListOptions> {

    public static AttachmentListOptions create() {
        return new AttachmentListOptions();
    }

    public AttachmentListOptions instanceId(String instanceId) { return put("instance_id", instanceId, 0); }
    public AttachmentListOptions volumeId(String volumeId) { return put("volume_id", volumeId, 0); }
    public AttachmentListOptions status(String status) { return put("status", status, 0); }
}
