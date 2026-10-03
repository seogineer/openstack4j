package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.NetworkSegmentRange;
import org.openstack4j.model.network.ext.Segment;
import org.openstack4j.model.network.options.NetworkSegmentRangeOptions;
import org.openstack4j.model.network.options.SegmentOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/Segments")
public class SegmentTests extends AbstractNetworkingExtTest {

    public void segments() throws Exception {
        String segment = "{\"name\": null, \"network_id\": \"n1\", \"segmentation_id\": 2, \"network_type\": \"vlan\", \"physical_network\": \"public\", \"revision_number\": 1, \"id\": \"seg1\", \"description\": null}";
        respondWith(201, "{\"segment\": " + segment + "}");
        respondWith(200, "{\"segments\": [" + segment + "]}");
        respondWith(200, "{\"segment\": " + segment + "}");
        respondWith(200, "{\"segment\": " + segment + "}");
        respondWith(204);

        var segments = osv3().networking().segments();
        Segment created = segments.create(SegmentOptions.create("n1", "vlan").physicalNetwork("public").segmentationId(2));
        List<? extends Segment> all = segments.list();
        segments.get("seg1");
        segments.update("seg1", SegmentOptions.update().name("renamed"));
        segments.delete("seg1");

        RecordedRequest create = expect("POST", "/v2.0/segments");
        var body = body(create).get("segment");
        Assert.assertEquals(body.get("network_id").asText(), "n1");
        Assert.assertEquals(body.get("segmentation_id").asInt(), 2);
        expect("GET", "/v2.0/segments");
        expect("GET", "/v2.0/segments/seg1");
        expect("PUT", "/v2.0/segments/seg1");
        expect("DELETE", "/v2.0/segments/seg1");
        Assert.assertEquals(created.getSegmentationId(), Integer.valueOf(2));
        Assert.assertNull(all.get(0).getName());
    }

    public void networkSegmentRanges() throws Exception {
        String range = "{\"id\": \"rg1\", \"name\": \"range_vlan_physnet1\", \"description\": \"d\", \"default\": false, \"shared\": false, \"project_id\": \"" + PROJECT + "\","
                + " \"network_type\": \"vlan\", \"physical_network\": \"physnet1\", \"minimum\": 10, \"maximum\": 20, \"available\": [10, 11, 19, 20], \"used\": {\"17\": \"p1\"}}";
        respondWith(201, "{\"network_segment_range\": " + range + "}");
        respondWith(200, "{\"network_segment_ranges\": [" + range + "]}");
        respondWith(200, "{\"network_segment_range\": " + range + "}");
        respondWith(200, "{\"network_segment_range\": " + range + "}");
        respondWith(204);

        var ranges = osv3().networking().networkSegmentRanges();
        NetworkSegmentRange created = ranges.create(NetworkSegmentRangeOptions.create("vlan", 10, 20).physicalNetwork("physnet1").name("range_vlan_physnet1"));
        ranges.list();
        ranges.get("rg1");
        ranges.update("rg1", NetworkSegmentRangeOptions.update().maximum(30));
        ranges.delete("rg1");

        RecordedRequest create = expect("POST", "/v2.0/network_segment_ranges");
        Assert.assertEquals(body(create).get("network_segment_range").get("minimum").asInt(), 10);
        expect("GET", "/v2.0/network_segment_ranges");
        expect("GET", "/v2.0/network_segment_ranges/rg1");
        Assert.assertEquals(body(expect("PUT", "/v2.0/network_segment_ranges/rg1")).get("network_segment_range").get("maximum").asInt(), 30);
        expect("DELETE", "/v2.0/network_segment_ranges/rg1");
        Assert.assertFalse(created.isDefault());
        Assert.assertEquals(created.getAvailable(), List.of(10, 11, 19, 20));
        Assert.assertEquals(created.getUsed().get("17"), "p1");
    }
}
