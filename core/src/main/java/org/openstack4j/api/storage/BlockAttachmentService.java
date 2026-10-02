package org.openstack4j.api.storage;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeAttachmentRecord;
import org.openstack4j.model.storage.block.options.AttachmentListOptions;

/**
 * Volume attachments ({@code /attachments}, block storage microversion 3.27+). Every method needs microversions to be
 * turned on ({@code os.blockStorage().microVersions().negotiate()}).
 */
public interface BlockAttachmentService extends RestService {

    List<? extends VolumeAttachmentRecord> list();

    List<? extends VolumeAttachmentRecord> list(AttachmentListOptions options);

    List<? extends VolumeAttachmentRecord> listDetail();

    List<? extends VolumeAttachmentRecord> listDetail(AttachmentListOptions options);

    VolumeAttachmentRecord get(String attachmentId);

    /**
     * Reserves (without a connector) or attaches (with one) a volume.
     *
     * @param instanceId the server, or {@code null}
     * @param connector  the host connector, or {@code null} to only reserve
     * @param mode       {@code rw} or {@code ro} (3.54+), or {@code null}
     */
    VolumeAttachmentRecord create(String volumeId, String instanceId, Map<String, Object> connector, String mode);

    /** Supplies the connector of a reserved attachment. */
    VolumeAttachmentRecord update(String attachmentId, Map<String, Object> connector);

    /** Marks the attachment complete ({@code os-complete}, 3.44+). */
    ActionResponse complete(String attachmentId);

    ActionResponse delete(String attachmentId);
}
