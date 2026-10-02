package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.VolumeAttachmentRecord;
import org.openstack4j.model.storage.block.VolumeMessage;
import org.openstack4j.model.storage.block.options.AttachmentListOptions;
import org.openstack4j.model.storage.block.options.MessageListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/AttachmentsMessages")
public class AttachmentAndMessageTests extends AbstractBlockStorageMicroVersionTest {

    private static final String ATTACHMENT = "{\"id\": \"a7d16728-e489-479f-96cd-2c2c6c24a100\", \"status\": \"reserved\", \"instance\": \"96a38bed-26b5-410b-8cef-1913a2e0e0b8\","
            + " \"volume_id\": \"" + VOLUME + "\", \"attached_at\": null, \"detached_at\": null, \"attach_mode\": \"rw\", \"connection_info\": {}}";

    public void attachmentLifecycle() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"attachment\": " + ATTACHMENT + "}");
        respondWith(200, "{\"attachment\": " + ATTACHMENT.replace("\"connection_info\": {}", "\"connection_info\": {\"driver_volume_type\": \"iscsi\"}") + "}");
        respondWith(204);
        respondWith(200, "{\"attachments\": [" + ATTACHMENT + "]}");
        respondWith(200, "{\"attachment\": " + ATTACHMENT + "}");
        respondWith(200);

        VolumeAttachmentRecord created = osv3().blockStorage().attachments().create(VOLUME, "96a38bed-26b5-410b-8cef-1913a2e0e0b8", null, "rw");
        VolumeAttachmentRecord updated = osv3().blockStorage().attachments().update(created.getId(), Collections.singletonMap("initiator", "iqn.x"));
        osv3().blockStorage().attachments().complete(created.getId());
        List<? extends VolumeAttachmentRecord> all = osv3().blockStorage().attachments().listDetail(AttachmentListOptions.create().volumeId(VOLUME));
        VolumeAttachmentRecord shown = osv3().blockStorage().attachments().get(created.getId());
        boolean deleted = osv3().blockStorage().attachments().delete(created.getId()).isSuccess();

        RecordedRequest create = takeRequest();
        assertVersionHeader(create, "3.71");
        JsonNode body = body(create).get("attachment");
        Assert.assertEquals(body.get("volume_uuid").asText(), VOLUME);
        Assert.assertEquals(body.get("instance_uuid").asText(), "96a38bed-26b5-410b-8cef-1913a2e0e0b8");
        Assert.assertEquals(body.get("mode").asText(), "rw");
        Assert.assertFalse(body.has("connector"));
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        Assert.assertEquals(body(update).get("attachment").get("connector").get("initiator").asText(), "iqn.x");
        RecordedRequest complete = takeRequest();
        Assert.assertTrue(complete.getPath().endsWith("/attachments/" + created.getId() + "/action"));
        Assert.assertTrue(body(complete).has("os-complete"));
        Assert.assertTrue(takeRequest().getPath().contains("/attachments/detail?volume_id=" + VOLUME));
        Assert.assertTrue(takeRequest().getPath().endsWith("/attachments/" + created.getId()));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getStatus(), "reserved");
        Assert.assertEquals(updated.getConnectionInfo().get("driver_volume_type"), "iscsi");
        Assert.assertEquals(all.size(), 1);
        Assert.assertEquals(shown.getAttachMode(), "rw");
        Assert.assertNull(shown.getAttachedAt());
        Assert.assertTrue(deleted);
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.27.*")
    public void attachmentsNeed327() throws Exception {
        try {
            osv3().blockStorage().attachments().list();
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.54.*")
    public void attachmentModeNeeds354() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.50");
        try {
            osv3().blockStorage().attachments().create(VOLUME, null, null, "ro");
        } finally {
            assertNoMoreRequests();
        }
    }

    public void messagesListGetDelete() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"messages\": [{\"id\": \"c506cd4b-9048-43bc-97ef-0d7dec369b42\", \"event_id\": \"VOLUME_000002\", \"user_message\": \"No storage could be allocated for this volume request.\","
                + " \"message_level\": \"ERROR\", \"resource_type\": \"VOLUME\", \"resource_uuid\": \"" + VOLUME + "\", \"request_id\": \"req-1\","
                + " \"created_at\": \"2026-10-02T00:00:00-00:00\", \"guaranteed_until\": \"2026-11-01T00:00:00-00:00\", \"links\": []}]}");
        respondWith(200, "{\"message\": {\"id\": \"c506cd4b-9048-43bc-97ef-0d7dec369b42\", \"event_id\": \"VOLUME_000002\", \"message_level\": \"ERROR\"}}");
        respondWith(204);

        List<? extends VolumeMessage> messages = osv3().blockStorage().messages().list(MessageListOptions.create().resourceUuid(VOLUME).limit(10));
        VolumeMessage one = osv3().blockStorage().messages().get("c506cd4b-9048-43bc-97ef-0d7dec369b42");
        boolean deleted = osv3().blockStorage().messages().delete("c506cd4b-9048-43bc-97ef-0d7dec369b42").isSuccess();

        String path = takeRequest().getPath();
        Assert.assertTrue(path.contains("/messages?") && path.contains("resource_uuid=" + VOLUME) && path.contains("limit=10"), path);
        Assert.assertTrue(takeRequest().getPath().endsWith("/messages/c506cd4b-9048-43bc-97ef-0d7dec369b42"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(messages.get(0).getEventId(), "VOLUME_000002");
        Assert.assertEquals(messages.get(0).getResourceUuid(), VOLUME);
        Assert.assertNotNull(messages.get(0).getGuaranteedUntil());
        Assert.assertEquals(one.getMessageLevel(), "ERROR");
        Assert.assertTrue(deleted);
        Assert.assertEquals(MessageListOptions.create().limit(1).getRequiredMicroVersion(), "3.5");
        Assert.assertNull(MessageListOptions.create().eventId("x").getRequiredMicroVersion());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.3.*")
    public void messagesNeed33() throws Exception {
        try {
            osv3().blockStorage().messages().list();
        } finally {
            assertNoMoreRequests();
        }
    }
}
