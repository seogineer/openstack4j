package org.openstack4j.api.instanceha;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.model.instanceha.Host;
import org.openstack4j.model.instanceha.Notification;
import org.openstack4j.model.instanceha.Segment;
import org.openstack4j.model.instanceha.VMove;
import org.openstack4j.model.instanceha.options.HostOptions;
import org.openstack4j.model.instanceha.options.NotificationOptions;
import org.openstack4j.model.instanceha.options.SegmentOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "InstanceHa")
public class InstanceHaTests extends AbstractTest {

    private static final String P = "/v1/26decac97b67478f9f64ff2c2c1b778e";
    private static final String SEGMENT = "{\"uuid\": \"seg1\", \"name\": \"new_segment\", \"service_type\": \"COMPUTE\", \"recovery_method\": \"auto\", \"enabled\": true}";
    private static final String HOST = "{\"uuid\": \"h1\", \"name\": \"openstack-VirtualBox\", \"type\": \"COMPUTE\", \"control_attributes\": \"SSH\", \"reserved\": false,"
            + " \"on_maintenance\": false, \"failover_segment\": {\"uuid\": \"seg1\"}}";

    @Override
    protected Service service() {
        return Service.INSTANCE_HA;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    private static String json(RecordedRequest r) throws Exception {
        return new ObjectMapper().readTree(r.getBody().readUtf8()).toString();
    }

    public void segmentsAndHosts() throws Exception {
        respondWith(200, "{\"segment\": " + SEGMENT + "}");
        respondWith(200, "{\"segments\": [" + SEGMENT + "]}");
        respondWith(200, "{\"segment\": " + SEGMENT + "}");
        respondWith(204);
        respondWith(200, "{\"host\": " + HOST + "}");
        respondWith(200, "{\"hosts\": [" + HOST + "]}");
        respondWith(200, "{\"host\": " + HOST + "}");
        respondWith(200, "{\"host\": " + HOST.replace("\"on_maintenance\": false", "\"on_maintenance\": true") + "}");
        respondWith(204);

        var ha = osv3().instanceHa();
        Segment segment = ha.segments().create(SegmentOptions.create("new_segment", "COMPUTE", "auto").enabled(true));
        List<? extends Segment> segments = ha.segments().list(Map.of("recovery_method", "auto"));
        ha.segments().update("seg1", SegmentOptions.update().description("d"));
        Assert.assertTrue(ha.segments().delete("seg1").isSuccess());
        Host host = ha.hosts().create("seg1", HostOptions.create("openstack-VirtualBox", "COMPUTE", "SSH"));
        List<? extends Host> hosts = ha.hosts().list("seg1", Map.of("on_maintenance", "false"));
        ha.hosts().get("seg1", "h1");
        Host updated = ha.hosts().update("seg1", "h1", HostOptions.update().onMaintenance(true));
        Assert.assertTrue(ha.hosts().delete("seg1", "h1").isSuccess());

        RecordedRequest createSegment = takeRequest();
        Assert.assertEquals(path(createSegment), P + "/segments");
        Assert.assertEquals(createSegment.getHeader("OpenStack-API-Version"), "instance-ha 1.2");
        Assert.assertEquals(json(createSegment), "{\"segment\":{\"name\":\"new_segment\",\"service_type\":\"COMPUTE\",\"recovery_method\":\"auto\",\"enabled\":true}}");
        Assert.assertEquals(path(takeRequest()), P + "/segments?recovery_method=auto");
        RecordedRequest updateSegment = takeRequest();
        Assert.assertNull(updateSegment.getHeader("OpenStack-API-Version"));
        Assert.assertEquals(json(updateSegment), "{\"segment\":{\"description\":\"d\"}}");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        RecordedRequest createHost = takeRequest();
        Assert.assertEquals(path(createHost), P + "/segments/seg1/hosts");
        Assert.assertEquals(json(createHost), "{\"host\":{\"name\":\"openstack-VirtualBox\",\"type\":\"COMPUTE\",\"control_attributes\":\"SSH\"}}");
        Assert.assertEquals(path(takeRequest()), P + "/segments/seg1/hosts?on_maintenance=false");
        Assert.assertEquals(path(takeRequest()), P + "/segments/seg1/hosts/h1");
        Assert.assertEquals(json(takeRequest()), "{\"host\":{\"on_maintenance\":true}}");
        Assert.assertEquals(path(takeRequest()), P + "/segments/seg1/hosts/h1");
        Assert.assertEquals(segment.getUuid(), "seg1");
        Assert.assertEquals(segments.get(0).isEnabled(), Boolean.TRUE);
        Assert.assertEquals(host.getFailoverSegment().get("uuid"), "seg1");
        Assert.assertEquals(hosts.get(0).getControlAttributes(), "SSH");
        Assert.assertEquals(updated.isOnMaintenance(), Boolean.TRUE);
    }

    public void notificationsAndVmoves() throws Exception {
        String notification = "{\"notification_uuid\": \"n1\", \"status\": \"new\", \"type\": \"VM\", \"source_host_uuid\": \"h1\","
                + " \"payload\": {\"instance_uuid\": \"i1\", \"event\": \"LIFECYCLE\"}}";
        respondWith(200, "{\"notification\": " + notification + "}");
        respondWith(200, "{\"notifications\": [" + notification + "]}");
        respondWith(200, "{\"notification\": " + notification.replace("\"new\"", "\"finished\"") + "}");
        respondWith(200, "{\"vmoves\": [{\"uuid\": \"vm1\", \"instance_uuid\": \"i1\", \"source_host\": \"host1\", \"dest_host\": \"host2\", \"status\": \"succeeded\"}]}");
        respondWith(200, "{\"vmove\": {\"uuid\": \"vm1\", \"instance_name\": \"vm1\"}}");

        var ha = osv3().instanceHa();
        Notification created = ha.notifications().create(NotificationOptions.create("VM", "openstack-VirtualBox", "2017-04-23T07:18:51.523726",
                Map.of("instance_uuid", "i1", "vir_domain_event", "STOPPED_DESTROYED", "event", "LIFECYCLE")));
        ha.notifications().list(Map.of("status", "new"));
        Notification finished = ha.notifications().get("n1");
        List<? extends VMove> moves = ha.notifications().listVMoves("n1", null);
        VMove move = ha.notifications().getVMove("n1", "vm1");

        RecordedRequest create = takeRequest();
        Assert.assertEquals(path(create), P + "/notifications");
        Assert.assertEquals(new ObjectMapper().readTree(create.getBody().readUtf8()).get("notification").get("hostname").asText(), "openstack-VirtualBox");
        Assert.assertEquals(path(takeRequest()), P + "/notifications?status=new");
        RecordedRequest get = takeRequest();
        Assert.assertEquals(get.getHeader("OpenStack-API-Version"), "instance-ha 1.1");
        RecordedRequest vmoves = takeRequest();
        Assert.assertEquals(path(vmoves), P + "/notifications/n1/vmoves");
        Assert.assertEquals(vmoves.getHeader("OpenStack-API-Version"), "instance-ha 1.3");
        Assert.assertEquals(path(takeRequest()), P + "/notifications/n1/vmoves/vm1");
        Assert.assertEquals(created.getStatus(), "new");
        Assert.assertEquals(finished.getStatus(), "finished");
        Assert.assertEquals(moves.get(0).getDestHost(), "host2");
        Assert.assertEquals(move.getInstanceName(), "vm1");
    }
}
