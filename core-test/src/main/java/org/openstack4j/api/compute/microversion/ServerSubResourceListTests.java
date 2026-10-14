package org.openstack4j.api.compute.microversion;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.compute.InterfaceAttachment;
import org.openstack4j.model.compute.ServerGroup;
import org.openstack4j.model.compute.ServerGroupListOptions;
import org.openstack4j.model.compute.VolumeAttachment;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Test(suiteName = "Compute/ServerSubResourceLists")
public class ServerSubResourceListTests extends AbstractComputeMicroVersionTest {

    private static Set<String> keys(JsonNode node) {
        Set<String> keys = new HashSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    public void setMetadataPosts() throws Exception {
        respondWith(200, "{\"metadata\":{\"a\":\"1\",\"b\":\"2\"}}");

        Map<String, String> result = osv3().compute().servers().setMetadata("s1", Map.of("a", "1"));

        RecordedRequest request = takeRequest();
        assertEquals(request.getMethod(), "POST");
        assertTrue(request.getPath().endsWith("/servers/s1/metadata"), request.getPath());
        JsonNode body = body(request);
        assertEquals(keys(body), Set.of("metadata"));
        assertEquals(body.get("metadata").get("a").asText(), "1");
        assertEquals(body.get("metadata").size(), 1);
        assertEquals(result.size(), 2);
        assertEquals(result.get("b"), "2");
    }

    public void updateMetadataStillPuts() throws Exception {
        respondWith(200, "{\"metadata\":{\"a\":\"1\"}}");

        osv3().compute().servers().updateMetadata("s1", Map.of("a", "1"));

        RecordedRequest request = takeRequest();
        assertEquals(request.getMethod(), "PUT");
        assertTrue(request.getPath().endsWith("/servers/s1/metadata"), request.getPath());
    }

    private static final String ATTACHMENT = "{\"interfaceAttachment\":{\"port_id\":\"p1\",\"net_id\":\"n1\",\"port_state\":\"ACTIVE\","
            + "\"mac_addr\":\"fa:16:3e:00:00:01\",\"fixed_ips\":[{\"subnet_id\":\"sub1\",\"ip_address\":\"10.0.0.5\"}]}}";

    public void attachToNetworkWithIp() throws Exception {
        respondWith(200, ATTACHMENT);

        InterfaceAttachment attached = osv3().compute().servers().interfaces().attachToNetwork("s1", "n1", "10.0.0.5");

        RecordedRequest request = takeRequest();
        assertEquals(request.getMethod(), "POST");
        assertTrue(request.getPath().endsWith("/servers/s1/os-interface"), request.getPath());
        JsonNode body = body(request);
        assertEquals(keys(body), Set.of("interfaceAttachment"));
        JsonNode inner = body.get("interfaceAttachment");
        assertEquals(keys(inner), Set.of("net_id", "fixed_ips"));
        assertEquals(inner.get("net_id").asText(), "n1");
        assertEquals(inner.get("fixed_ips").size(), 1);
        assertEquals(keys(inner.get("fixed_ips").get(0)), Set.of("ip_address"));
        assertEquals(inner.get("fixed_ips").get(0).get("ip_address").asText(), "10.0.0.5");
        assertEquals(attached.getPortId(), "p1");
        assertEquals(attached.getNetId(), "n1");
    }

    public void attachToNetworkWithoutIp() throws Exception {
        respondWith(200, ATTACHMENT);

        osv3().compute().servers().interfaces().attachToNetwork("s1", "n1", null);

        JsonNode inner = body(takeRequest()).get("interfaceAttachment");
        assertEquals(keys(inner), Set.of("net_id"));
        assertFalse(inner.has("fixed_ips"));
    }

    public void attachToPortBodyUnchanged() throws Exception {
        respondWith(200, ATTACHMENT);

        osv3().compute().servers().interfaces().create("s1", "p");

        JsonNode inner = body(takeRequest()).get("interfaceAttachment");
        assertEquals(keys(inner), Set.of("port_id"));
        assertEquals(inner.get("port_id").asText(), "p");
    }

    public void listVolumeAttachments() throws Exception {
        respondWith(200, "{\"volumeAttachments\":["
                + "{\"id\":\"v1\",\"serverId\":\"s1\",\"volumeId\":\"v1\",\"device\":\"/dev/vdb\"},"
                + "{\"serverId\":\"s1\",\"volumeId\":\"v2\",\"device\":\"/dev/vdc\",\"attachment_id\":\"a2\","
                + "\"bdm_uuid\":\"b2\",\"tag\":\"t2\",\"delete_on_termination\":true}]}");

        List<? extends VolumeAttachment> list = osv3().compute().servers().listVolumeAttachments("s1");

        RecordedRequest request = takeRequest();
        assertEquals(request.getMethod(), "GET");
        assertTrue(request.getPath().endsWith("/servers/s1/os-volume_attachments"), request.getPath());
        assertEquals(list.size(), 2);
        assertEquals(list.get(0).getId(), "v1");
        assertEquals(list.get(0).getDevice(), "/dev/vdb");
        assertNull(list.get(1).getId());
        assertEquals(list.get(1).getVolumeId(), "v2");
        assertEquals(list.get(1).getAttachmentId(), "a2");
        assertEquals(list.get(1).getBdmUuid(), "b2");
        assertEquals(list.get(1).getTag(), "t2");
        assertEquals(list.get(1).getDeleteOnTermination(), Boolean.TRUE);
    }

    public void getVolumeAttachmentFound() throws Exception {
        respondWith(200, "{\"volumeAttachment\":{\"serverId\":\"s1\",\"volumeId\":\"v2\",\"device\":\"/dev/vdc\"}}");

        VolumeAttachment attachment = osv3().compute().servers().getVolumeAttachment("s1", "v2");

        RecordedRequest request = takeRequest();
        assertTrue(request.getPath().endsWith("/servers/s1/os-volume_attachments/v2"), request.getPath());
        assertEquals(attachment.getDevice(), "/dev/vdc");
    }

    public void getVolumeAttachment404() throws Exception {
        respondWith(404, "{\"itemNotFound\":{\"message\":\"nope\",\"code\":404}}");

        assertNull(osv3().compute().servers().getVolumeAttachment("s1", "v2"));

        takeRequest();
    }

    public void serverGroupsAllProjects() throws Exception {
        respondWith(200, "{\"server_groups\":[{\"id\":\"g1\",\"name\":\"grp\",\"policy\":\"affinity\","
                + "\"project_id\":\"p1\",\"user_id\":\"u1\",\"members\":[]}]}");

        List<? extends ServerGroup> groups = osv3().compute().serverGroups()
                .list(ServerGroupListOptions.create().allProjects().limit(10).offset(5));

        RecordedRequest request = takeRequest();
        assertEquals(request.getMethod(), "GET");
        assertTrue(request.getPath().contains("/os-server-groups?"), request.getPath());
        String query = request.getPath().substring(request.getPath().indexOf('?') + 1);
        assertEquals(Set.of(query.split("&")), Set.of("all_projects=true", "limit=10", "offset=5"));
        assertEquals(groups.size(), 1);
        assertEquals(groups.get(0).getProjectId(), "p1");
        assertEquals(groups.get(0).getUserId(), "u1");
    }

    public void serverGroupsEmptyOptions() throws Exception {
        respondWith(200, "{\"server_groups\":[]}");

        Assert.assertTrue(osv3().compute().serverGroups().list(ServerGroupListOptions.create()).isEmpty());

        RecordedRequest request = takeRequest();
        assertFalse(request.getPath().contains("?"), request.getPath());
    }

    public void serverGroupsPlainListUnchanged() throws Exception {
        respondWith(200, "{\"server_groups\":[]}");

        osv3().compute().serverGroups().list();

        assertFalse(takeRequest().getPath().contains("?"));
    }
}
