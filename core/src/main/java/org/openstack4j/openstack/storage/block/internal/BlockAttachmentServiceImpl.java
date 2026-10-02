package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.storage.BlockAttachmentService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeAttachmentRecord;
import org.openstack4j.model.storage.block.options.AttachmentListOptions;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.storage.block.domain.CinderAttachment;
import org.openstack4j.openstack.storage.block.domain.CinderAttachment.Attachments;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;

public class BlockAttachmentServiceImpl extends BaseBlockStorageServices implements BlockAttachmentService {

    @Override public List<? extends VolumeAttachmentRecord> list() { return list(AttachmentListOptions.create()); }
    @Override public List<? extends VolumeAttachmentRecord> listDetail() { return listDetail(AttachmentListOptions.create()); }

    @Override
    public List<? extends VolumeAttachmentRecord> list(AttachmentListOptions options) {
        requireMicroVersion("Attachments", V(27));
        return get(Attachments.class, uri("/attachments")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public List<? extends VolumeAttachmentRecord> listDetail(AttachmentListOptions options) {
        requireMicroVersion("Attachments", V(27));
        return get(Attachments.class, uri("/attachments/detail")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public VolumeAttachmentRecord get(String attachmentId) {
        Objects.requireNonNull(attachmentId);
        requireMicroVersion("Attachments", V(27));
        return get(CinderAttachment.class, uri("/attachments/%s", attachmentId)).execute();
    }

    @Override
    public VolumeAttachmentRecord create(String volumeId, String instanceId, Map<String, Object> connector, String mode) {
        Objects.requireNonNull(volumeId);
        requireMicroVersion("Attachments", V(27));
        if (mode != null)
            requireMicroVersion("Attachment mode", V(54));
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("volume_uuid", volumeId);
        if (instanceId != null) attachment.put("instance_uuid", instanceId);
        if (connector != null) attachment.put("connector", connector);
        if (mode != null) attachment.put("mode", mode);
        return post(CinderAttachment.class, uri("/attachments")).entity(JsonBody.of("attachment", attachment)).execute();
    }

    @Override
    public VolumeAttachmentRecord update(String attachmentId, Map<String, Object> connector) {
        Objects.requireNonNull(attachmentId);
        Objects.requireNonNull(connector);
        requireMicroVersion("Attachments", V(27));
        return put(CinderAttachment.class, uri("/attachments/%s", attachmentId))
                .entity(JsonBody.of("attachment", Collections.singletonMap("connector", connector))).execute();
    }

    @Override
    public ActionResponse complete(String attachmentId) {
        Objects.requireNonNull(attachmentId);
        requireMicroVersion("Attachment completion", V(44));
        return post(ActionResponse.class, uri("/attachments/%s/action", attachmentId))
                .entity(JsonBody.of("os-complete", Collections.emptyMap())).execute();
    }

    @Override
    public ActionResponse delete(String attachmentId) {
        Objects.requireNonNull(attachmentId);
        requireMicroVersion("Attachments", V(27));
        return deleteWithResponse(uri("/attachments/%s", attachmentId)).execute();
    }
}
