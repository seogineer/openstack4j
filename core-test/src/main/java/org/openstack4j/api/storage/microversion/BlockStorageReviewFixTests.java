package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.model.storage.block.options.VolumeListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Behaviours found in the final review of sub-project D. */
@Test(suiteName = "BlockStorage/ReviewFixes")
public class BlockStorageReviewFixTests extends AbstractBlockStorageMicroVersionTest {

    public void quotaClassUpdateSendsOnlyTheFieldsSet() throws Exception {
        respondWith(200, "{\"quota_class_set\": {\"id\": \"default\", \"volumes\": 5, \"snapshots\": 10, \"gigabytes\": 1000}}");
        respondWith(200, "{\"quota_set\": {\"id\": \"p1\", \"volumes\": 5, \"snapshots\": 10, \"gigabytes\": 1000}}");

        osv3().blockStorage().quotaSets().updateQuotaClass("default", Builders.blockQuotaSet().volumes(5).build());
        osv3().blockStorage().quotaSets().updateForTenant("p1", Builders.blockQuotaSet().volumes(5).build());

        for (String root : new String[] {"quota_class_set", "quota_set"}) {
            JsonNode body = body(takeRequest()).get(root);
            Assert.assertEquals(body.get("volumes").asInt(), 5);
            Assert.assertFalse(body.has("snapshots"), root + " must not reset snapshots to 0");
            Assert.assertFalse(body.has("gigabytes"), root + " must not reset gigabytes to 0");
        }
    }

    public void unsetQuotaFieldsStillReadAsZero() throws Exception {
        respondWith(200, "{\"quota_set\": {\"id\": \"p1\", \"volumes\": 5}}");
        var quota = osv3().blockStorage().quotaSets().get("p1");
        takeRequest();
        Assert.assertEquals(quota.getVolumes(), 5);
        Assert.assertEquals(quota.getSnapshots(), 0);
        Assert.assertEquals(quota.getGigabytes(), 0);
    }

    public void metadataFilterEscapesQuotesAndBackslashes() {
        Map<String, String> params = VolumeListOptions.create().metadata(Collections.singletonMap("owner", "O'Brien\\x")).toQueryParams();
        Assert.assertEquals(params.get("metadata"), "{'owner': 'O\\'Brien\\\\x'}");
    }

    public void metadataSettersReturnEmptyOnNotFound() throws Exception {
        respondWith(404, "{\"itemNotFound\": {\"message\": \"gone\", \"code\": 404}}");
        respondWith(404, "{\"itemNotFound\": {\"message\": \"gone\", \"code\": 404}}");
        respondWith(404, "{\"itemNotFound\": {\"message\": \"gone\", \"code\": 404}}");

        Map<String, String> set = osv3().blockStorage().volumes().setMetadata(VOLUME, Collections.singletonMap("k", "v"));
        Map<String, String> image = osv3().blockStorage().volumes().setImageMetadata(VOLUME, Collections.singletonMap("k", "v"));
        Map<String, String> specs = osv3().blockStorage().volumeTypes().setExtraSpecs("t1", Collections.singletonMap("k", "v"));

        takeRequest();
        takeRequest();
        takeRequest();
        Assert.assertTrue(set.isEmpty());
        Assert.assertTrue(image.isEmpty());
        Assert.assertTrue(specs.isEmpty());
    }
}
