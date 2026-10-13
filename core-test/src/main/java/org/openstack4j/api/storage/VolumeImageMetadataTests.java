package org.openstack4j.api.storage;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.openstack.storage.block.domain.CinderVolume;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Cinder volume_image_metadata is readable through Volume.getImageMetadata(). */
@Test(suiteName = "Block Storage Tests")
public class VolumeImageMetadataTests extends AbstractTest {

    private static final String VOLUME = "{\"volume\":{\"id\":\"v1\",\"name\":\"boot\",\"volume_image_metadata\":"
            + "{\"image_id\":\"img-1\",\"image_name\":\"cirros\",\"min_disk\":\"1\",\"size\":13267968}}}";

    @Override
    protected Service service() {
        return Service.BLOCK_STORAGE;
    }

    public void imageMetadataIsRead() throws Exception {
        respondWith(200, VOLUME);
        Volume volume = osv3().blockStorage().volumes().get("v1");
        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "GET");

        Map<String, String> metadata = volume.getImageMetadata();
        Assert.assertEquals(metadata.get("image_name"), "cirros");
        Assert.assertEquals(metadata.get("min_disk"), "1");
        Assert.assertEquals(metadata.get("size"), "13267968");
        Assert.assertEquals(volume.getImageRef(), "img-1");
    }

    public void withoutImageMetadataIsNull() throws Exception {
        respondWith(200, "{\"volume\":{\"id\":\"v1\"}}");
        Volume volume = osv3().blockStorage().volumes().get("v1");
        takeRequest();
        Assert.assertNull(volume.getImageMetadata());
        Assert.assertNull(volume.getImageRef());
    }

    public void serializationIsUnchanged() throws Exception {
        CinderVolume volume = ObjectMapperSingleton.getContext(CinderVolume.class).readValue(VOLUME, CinderVolume.class);
        Assert.assertEquals(volume.getImageMetadata().get("image_id"), "img-1");

        String json = ObjectMapperSingleton.getContext(CinderVolume.class).writeValueAsString(volume);
        Assert.assertEquals(json.indexOf("\"volume_image_metadata\""), json.lastIndexOf("\"volume_image_metadata\""), json);
        JsonNode metadata = new ObjectMapper().readTree(json).get("volume").get("volume_image_metadata");
        Assert.assertEquals(metadata.get("image_name").asText(), "cirros");
        Assert.assertEquals(metadata.get("min_disk").asText(), "1");
        Assert.assertTrue(metadata.get("size").isNumber(), json);
        Assert.assertFalse(json.contains("\"imageMetadata\""), json);
    }
}
