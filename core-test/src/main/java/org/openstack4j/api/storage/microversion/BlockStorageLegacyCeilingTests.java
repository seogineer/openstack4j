package org.openstack4j.api.storage.microversion;

import java.util.Collections;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/LegacyCeilings")
public class BlockStorageLegacyCeilingTests extends AbstractBlockStorageMicroVersionTest {

    private static final String CREATED_VOLUME = "{\"volume\": {\"id\": \"" + VOLUME + "\", \"status\": \"creating\", \"size\": 1}}";
    private static final String CREATED_SNAPSHOT = "{\"snapshot\": {\"id\": \"" + SNAPSHOT + "\", \"status\": \"creating\", \"volume_id\": \"" + VOLUME + "\"}}";

    public void legacyCreateWithBootableIsCapped() throws Exception {
        negotiate("3.71");
        respondWith(202, CREATED_VOLUME);
        respondWith(202, CREATED_VOLUME);

        osv3().blockStorage().volumes().create(Builders.volume().name("a").size(1).bootable(true).build());
        osv3().blockStorage().volumes().create(Builders.volume().name("b").size(1).build());

        RecordedRequest withBootable = takeRequest();
        assertVersionHeader(withBootable, "3.52");
        Assert.assertTrue(body(withBootable).get("volume").get("bootable").asBoolean());
        assertVersionHeader(takeRequest(), "3.71");
    }

    public void legacySnapshotForceFalseIsCapped() throws Exception {
        negotiate("3.71");
        respondWith(202, CREATED_SNAPSHOT);
        respondWith(202, CREATED_SNAPSHOT);
        respondWith(202, CREATED_SNAPSHOT);

        osv3().blockStorage().snapshots().create(Builders.volumeSnapshot().name("s").volume(VOLUME).force(false).build());
        osv3().blockStorage().snapshots().create(Builders.volumeSnapshot().name("s").volume(VOLUME).force(true).build());
        osv3().blockStorage().snapshots().create(Builders.volumeSnapshot().name("s").volume(VOLUME).build());

        assertVersionHeader(takeRequest(), "3.65");
        assertVersionHeader(takeRequest(), "3.71");
        assertVersionHeader(takeRequest(), "3.71");
    }

    public void legacyListsStayAtNegotiatedVersion() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"volumes\": []}");
        respondWith(200, "{\"snapshots\": []}");
        respondWith(200, "{\"backups\": []}");
        respondWith(200, "{\"transfers\": []}");
        respondWith(200, "{\"services\": []}");

        osv3().blockStorage().volumes().list(Collections.singletonMap("status", "available"));
        osv3().blockStorage().snapshots().list();
        osv3().blockStorage().backups().list();
        osv3().blockStorage().volumes().transfer().list();
        osv3().blockStorage().services().list();

        for (int i = 0; i < 5; i++)
            assertVersionHeader(takeRequest(), "3.71");
    }
}
