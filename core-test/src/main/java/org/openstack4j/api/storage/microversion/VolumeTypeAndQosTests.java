package org.openstack4j.api.storage.microversion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.DefaultVolumeType;
import org.openstack4j.model.storage.block.QosSpec;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.VolumeTypeAccess;
import org.openstack4j.model.storage.block.options.VolumeTypeListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/TypesQos")
public class VolumeTypeAndQosTests extends AbstractBlockStorageMicroVersionTest {

    private static final String TYPE = "6685584b-1eac-4da6-b5c3-555430cf68ff";
    private static final String TYPE_JSON = "{\"volume_type\": {\"id\": \"" + TYPE + "\", \"name\": \"vol-type-001\", \"description\": \"d\", \"is_public\": true, \"os-volume-type-access:is_public\": true, \"extra_specs\": {\"capabilities\": \"gpu\"}, \"qos_specs_id\": null}}";

    public void typeCrudAndDefault() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"volume_types\": [" + TYPE_JSON.substring(16, TYPE_JSON.length() - 1) + "]}");
        respondWith(200, TYPE_JSON);
        respondWith(200, TYPE_JSON);
        respondWith(200, TYPE_JSON);
        respondWith(200, TYPE_JSON);
        respondWith(202);

        List<? extends VolumeType> types = osv3().blockStorage().volumeTypes().list(VolumeTypeListOptions.create().isPublic(true).extraSpecs(Collections.singletonMap("capabilities", "gpu")));
        VolumeType one = osv3().blockStorage().volumeTypes().get(TYPE);
        VolumeType def = osv3().blockStorage().volumeTypes().getDefault();
        VolumeType created = osv3().blockStorage().volumeTypes().create(Builders.volumeType().name("vol-type-001").description("d").isPublic(false).extraSpecs(Collections.singletonMap("capabilities", "gpu")).build());
        osv3().blockStorage().volumeTypes().update(TYPE, "renamed", null, true);
        boolean deleted = osv3().blockStorage().volumeTypes().delete(TYPE).isSuccess();

        String list = takeRequest().getPath();
        Assert.assertTrue(list.contains("/types?") && list.contains("is_public=true") && list.contains("extra_specs="), list);
        Assert.assertTrue(takeRequest().getPath().endsWith("/types/" + TYPE));
        Assert.assertTrue(takeRequest().getPath().endsWith("/types/default"));
        JsonNode create = body(takeRequest()).get("volume_type");
        Assert.assertFalse(create.get("os-volume-type-access:is_public").asBoolean());
        Assert.assertEquals(create.get("description").asText(), "d");
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        JsonNode upd = body(update).get("volume_type");
        Assert.assertEquals(upd.get("name").asText(), "renamed");
        Assert.assertTrue(upd.get("is_public").asBoolean());
        Assert.assertFalse(upd.has("description"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(types.get(0).getName(), "vol-type-001");
        Assert.assertEquals(one.getExtraSpecs().get("capabilities"), "gpu");
        Assert.assertEquals(def.getId(), TYPE);
        Assert.assertEquals(created.isPublic(), Boolean.TRUE);
        Assert.assertTrue(deleted);
        Assert.assertEquals(VolumeTypeListOptions.create().extraSpecs(Map.of("a", "b")).getRequiredMicroVersion(), "3.52");
    }

    public void extraSpecsAndAccess() throws Exception {
        respondWith(200, "{\"extra_specs\": {\"capabilities\": \"gpu\"}}");
        respondWith(200, "{\"extra_specs\": {\"capabilities\": \"gpu\", \"k\": \"v\"}}");
        respondWith(200, "{\"k\": \"v\"}");
        respondWith(200, "{\"k\": \"v2\"}");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"volume_type_access\": [{\"project_id\": \"p1\", \"volume_type_id\": \"" + TYPE + "\"}]}");

        Map<String, String> specs = osv3().blockStorage().volumeTypes().extraSpecs(TYPE);
        Map<String, String> set = osv3().blockStorage().volumeTypes().setExtraSpecs(TYPE, Collections.singletonMap("k", "v"));
        String one = osv3().blockStorage().volumeTypes().extraSpec(TYPE, "k");
        String updated = osv3().blockStorage().volumeTypes().updateExtraSpec(TYPE, "k", "v2");
        osv3().blockStorage().volumeTypes().deleteExtraSpec(TYPE, "k");
        osv3().blockStorage().volumeTypes().addProjectAccess(TYPE, "p1");
        osv3().blockStorage().volumeTypes().removeProjectAccess(TYPE, "p1");
        List<? extends VolumeTypeAccess> access = osv3().blockStorage().volumeTypes().listProjectAccess(TYPE);

        Assert.assertTrue(takeRequest().getPath().endsWith("/types/" + TYPE + "/extra_specs"));
        Assert.assertEquals(body(takeRequest()).get("extra_specs").get("k").asText(), "v");
        Assert.assertTrue(takeRequest().getPath().endsWith("/extra_specs/k"));
        Assert.assertEquals(body(takeRequest()).get("k").asText(), "v2");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(body(takeRequest()).get("addProjectAccess").get("project").asText(), "p1");
        Assert.assertEquals(body(takeRequest()).get("removeProjectAccess").get("project").asText(), "p1");
        Assert.assertTrue(takeRequest().getPath().endsWith("/types/" + TYPE + "/os-volume-type-access"));
        Assert.assertEquals(specs.get("capabilities"), "gpu");
        Assert.assertEquals(set.size(), 2);
        Assert.assertEquals(one, "v");
        Assert.assertEquals(updated, "v2");
        Assert.assertEquals(access.get(0).getProjectId(), "p1");
    }

    public void encryptionCrud() throws Exception {
        respondWith(200, "{\"volume_type_id\": \"" + TYPE + "\", \"control_location\": \"front-end\", \"encryption_id\": \"e1\", \"key_size\": 256, \"provider\": \"luks\", \"cipher\": \"aes-xts-plain64\"}");
        respondWith(200, "{\"cipher\": \"aes-xts-plain64\"}");
        respondWith(200, "{\"encryption\": {\"key_size\": 64, \"provider\": \"luks\", \"control_location\": \"back-end\", \"cipher\": \"aes-xts-plain64\"}}");
        respondWith(202);

        osv3().blockStorage().volumeTypes().encryption(TYPE);
        String cipher = osv3().blockStorage().volumeTypes().encryptionSpec(TYPE, "cipher");
        osv3().blockStorage().volumeTypes().updateEncryption(TYPE, "e1", Builders.volumeTypeEncryption().provider("luks").keySize(64).build());
        boolean deleted = osv3().blockStorage().volumeTypes().deleteEncryption(TYPE, "e1").isSuccess();

        Assert.assertTrue(takeRequest().getPath().endsWith("/types/" + TYPE + "/encryption"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/encryption/cipher"));
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        Assert.assertTrue(update.getPath().endsWith("/encryption/e1"));
        Assert.assertEquals(body(update).get("encryption").get("key_size").asInt(), 64);
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(cipher, "aes-xts-plain64");
        Assert.assertTrue(deleted);
    }

    public void defaultTypes() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"default_types\": [{\"project_id\": \"p1\", \"volume_type_id\": \"" + TYPE + "\"}]}");
        respondWith(200, "{\"default_type\": {\"project_id\": \"p1\", \"volume_type_id\": \"" + TYPE + "\"}}");
        respondWith(200, "{\"default_type\": {\"project_id\": \"p1\", \"volume_type_id\": \"" + TYPE + "\"}}");
        respondWith(204);

        List<? extends DefaultVolumeType> all = osv3().blockStorage().defaultTypes().list();
        DefaultVolumeType one = osv3().blockStorage().defaultTypes().get("p1");
        DefaultVolumeType set = osv3().blockStorage().defaultTypes().set("p1", "lvm_backend");
        boolean unset = osv3().blockStorage().defaultTypes().unset("p1").isSuccess();

        Assert.assertTrue(takeRequest().getPath().endsWith("/default-types"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/default-types/p1"));
        RecordedRequest put = takeRequest();
        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertEquals(body(put).get("default_type").get("volume_type").asText(), "lvm_backend");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(all.get(0).getVolumeTypeId(), TYPE);
        Assert.assertEquals(one.getProjectId(), "p1");
        Assert.assertEquals(set.getVolumeTypeId(), TYPE);
        Assert.assertTrue(unset);
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.62.*")
    public void defaultTypesNeed362() throws Exception {
        try {
            osv3().blockStorage().defaultTypes().list();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void qosSpecs() throws Exception {
        String qos = "{\"qos_specs\": {\"specs\": {\"delay\": \"1\"}, \"consumer\": \"back-end\", \"name\": \"reliability-spec\", \"id\": \"0388d6c6-d5d4-42a3-b289-95205c50dd15\"}}";
        respondWith(200, "{\"qos_specs\": [{\"specs\": {}, \"consumer\": \"back-end\", \"name\": \"reliability-spec\", \"id\": \"0388d6c6-d5d4-42a3-b289-95205c50dd15\"}]}");
        respondWith(200, qos);
        respondWith(200, qos);
        respondWith(200, qos);
        respondWith(202);
        respondWith(200, "{\"qos_associations\": [{\"association_type\": \"volume_type\", \"name\": \"reliability-type\", \"id\": \"" + TYPE + "\"}]}");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);

        List<? extends QosSpec> all = osv3().blockStorage().qosSpecs().list();
        QosSpec created = osv3().blockStorage().qosSpecs().create("reliability-spec", Map.of("consumer", "back-end", "delay", "1"));
        QosSpec shown = osv3().blockStorage().qosSpecs().get(created.getId());
        osv3().blockStorage().qosSpecs().update(created.getId(), Collections.singletonMap("delay", "2"));
        osv3().blockStorage().qosSpecs().deleteKeys(created.getId(), Arrays.asList("delay"));
        osv3().blockStorage().qosSpecs().associations(created.getId());
        osv3().blockStorage().qosSpecs().associate(created.getId(), TYPE);
        osv3().blockStorage().qosSpecs().disassociate(created.getId(), TYPE);
        osv3().blockStorage().qosSpecs().disassociateAll(created.getId());
        osv3().blockStorage().qosSpecs().delete(created.getId(), true);

        Assert.assertTrue(takeRequest().getPath().endsWith("/qos-specs"));
        JsonNode create = body(takeRequest()).get("qos_specs");
        Assert.assertEquals(create.get("name").asText(), "reliability-spec");
        Assert.assertEquals(create.get("consumer").asText(), "back-end");
        Assert.assertTrue(takeRequest().getPath().endsWith("/qos-specs/" + created.getId()));
        Assert.assertEquals(body(takeRequest()).get("qos_specs").get("delay").asText(), "2");
        Assert.assertEquals(body(takeRequest()).get("keys").get(0).asText(), "delay");
        Assert.assertTrue(takeRequest().getPath().endsWith("/associations"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/associate?vol_type_id=" + TYPE));
        Assert.assertTrue(takeRequest().getPath().endsWith("/disassociate?vol_type_id=" + TYPE));
        Assert.assertTrue(takeRequest().getPath().endsWith("/disassociate_all"));
        RecordedRequest delete = takeRequest();
        Assert.assertEquals(delete.getMethod(), "DELETE");
        Assert.assertTrue(delete.getPath().endsWith("?force=true"));
        Assert.assertEquals(all.get(0).getName(), "reliability-spec");
        Assert.assertEquals(shown.getSpecs().get("delay"), "1");
        Assert.assertEquals(shown.getConsumer(), "back-end");
    }
}
