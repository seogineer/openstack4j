package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Conductor;
import org.openstack4j.model.baremetal.VolumeConnector;
import org.openstack4j.model.baremetal.VolumeTarget;
import org.openstack4j.model.baremetal.options.VolumeConnectorCreate;
import org.openstack4j.model.baremetal.options.VolumeTargetCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/Volume")
public class BaremetalVolumeTests extends AbstractBaremetalTest {

    private static final String CONNECTOR = "{\"uuid\": \"9bf93e01-d728-47a3-ad4b-5e66a835037c\", \"node_uuid\": \"6d85703a-565d-469a-96ce-30b6de53079d\","
            + " \"type\": \"iqn\", \"connector_id\": \"iqn.2017-07.org.openstack:01:d9a51732c3f\", \"extra\": {}, \"links\": []}";
    private static final String TARGET = "{\"uuid\": \"bd4d008c-7d31-463d-abf9-6c23d9d55f7f\", \"node_uuid\": \"6d85703a-565d-469a-96ce-30b6de53079d\","
            + " \"volume_type\": \"iscsi\", \"boot_index\": 0, \"volume_id\": \"04452bed-5367-4202-8bf5-de4335ac56d2\", \"properties\": {}, \"extra\": {}}";

    public void volumeConnectorsAndTargets() throws Exception {
        respondWith(201, CONNECTOR);
        respondWith(200, "{\"connectors\": [" + CONNECTOR + "]}");
        respondWith(200, "{\"connectors\": []}");
        respondWith(200, CONNECTOR);
        respondWith(204);
        respondWith(201, TARGET);
        respondWith(200, "{\"targets\": [" + TARGET + "]}");
        respondWith(200, TARGET);
        respondWith(204);

        var connectors = osv3().baremetal().volumeConnectors();
        VolumeConnector connector = connectors.create(VolumeConnectorCreate.create("6d85703a-565d-469a-96ce-30b6de53079d", "iqn", "iqn.2017-07.org.openstack:01:d9a51732c3f"));
        List<? extends VolumeConnector> all = connectors.list();
        connectors.listByNode("n1");
        connectors.update(connector.getUuid(), List.of(BaremetalPatch.replace("/extra/a", "b")));
        Assert.assertTrue(connectors.delete(connector.getUuid()).isSuccess());
        var targets = osv3().baremetal().volumeTargets();
        VolumeTarget target = targets.create(VolumeTargetCreate.create("6d85703a-565d-469a-96ce-30b6de53079d", "iscsi", 0, "04452bed-5367-4202-8bf5-de4335ac56d2"));
        List<? extends VolumeTarget> ofNode = targets.listByNode("n1");
        targets.get(target.getUuid());
        Assert.assertTrue(targets.delete(target.getUuid()).isSuccess());

        Assert.assertEquals(body(expect("POST", "/v1/volume/connectors")).get("connector_id").asText(), "iqn.2017-07.org.openstack:01:d9a51732c3f");
        expect("GET", "/v1/volume/connectors?detail=true");
        expect("GET", "/v1/nodes/n1/volume/connectors?detail=true");
        expect("PATCH", "/v1/volume/connectors/9bf93e01-d728-47a3-ad4b-5e66a835037c");
        expect("DELETE", "/v1/volume/connectors/9bf93e01-d728-47a3-ad4b-5e66a835037c");
        Assert.assertEquals(body(expect("POST", "/v1/volume/targets")).get("boot_index").asInt(), 0);
        expect("GET", "/v1/nodes/n1/volume/targets?detail=true");
        expect("GET", "/v1/volume/targets/bd4d008c-7d31-463d-abf9-6c23d9d55f7f");
        expect("DELETE", "/v1/volume/targets/bd4d008c-7d31-463d-abf9-6c23d9d55f7f");
        Assert.assertEquals(all.get(0).getType(), "iqn");
        Assert.assertEquals(ofNode.get(0).getBootIndex(), Integer.valueOf(0));
        Assert.assertEquals(target.getVolumeType(), "iscsi");
    }

    public void conductorsAndShards() throws Exception {
        respondWith(200, "{\"conductors\": [{\"hostname\": \"compute1.localdomain\", \"conductor_group\": \"\", \"alive\": false, \"drivers\": [\"ipmi\"], \"links\": []}]}");
        respondWith(200, "{\"hostname\": \"compute1.localdomain\", \"conductor_group\": \"\", \"alive\": true, \"drivers\": [\"ipmi\", \"redfish\"]}");
        respondWith(200, "{\"shards\": [{\"name\": \"example_shard1\", \"count\": 47}, {\"name\": null, \"count\": 3}]}");

        List<? extends Conductor> conductors = osv3().baremetal().conductors().list();
        Conductor conductor = osv3().baremetal().conductors().get("compute1.localdomain");
        Map<String, Integer> shards = osv3().baremetal().conductors().shards();

        expect("GET", "/v1/conductors?detail=true");
        expect("GET", "/v1/conductors/compute1.localdomain");
        expect("GET", "/v1/shards");
        Assert.assertEquals(conductors.get(0).isAlive(), Boolean.FALSE);
        Assert.assertEquals(conductor.getDrivers(), List.of("ipmi", "redfish"));
        Assert.assertEquals(shards.get("example_shard1"), Integer.valueOf(47));
        Assert.assertEquals(shards.get(null), Integer.valueOf(3));
    }
}
