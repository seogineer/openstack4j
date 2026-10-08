package org.openstack4j.api.reservation;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.model.reservation.Lease;
import org.openstack4j.model.reservation.ReservableFloatingIp;
import org.openstack4j.model.reservation.ReservableHost;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Reservation")
public class ReservationTests extends AbstractTest {

    private static final String LEASE = "{\"id\": \"l1\", \"name\": \"lease_foo\", \"start_date\": \"2017-12-26T12:00:00.000000\", \"status\": \"PENDING\", \"degraded\": false,"
            + " \"reservations\": [{\"id\": \"r1\", \"resource_type\": \"physical:host\", \"min\": 4, \"max\": 6}], \"events\": []}";
    private static final String HOST = "{\"id\": \"1\", \"hypervisor_hostname\": \"compute-1\", \"hypervisor_version\": 2010001, \"vcpus\": 4, \"reservable\": true, \"gpu\": \"a100\"}";

    @Override
    protected Service service() {
        return Service.RESERVATION;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    private static String json(RecordedRequest r) throws Exception {
        return new ObjectMapper().readTree(r.getBody().readUtf8()).toString();
    }

    public void leases() throws Exception {
        respondWith(201, "{\"lease\": " + LEASE + "}");
        respondWith(200, "{\"leases\": [" + LEASE + "]}");
        respondWith(200, "{\"lease\": " + LEASE + "}");
        respondWith(200, "{\"lease\": " + LEASE + "}");
        respondWith(204);

        var leases = osv3().reservation().leases();
        Lease created = leases.create(Map.of("name", "lease_foo", "start_date", "now", "end_date", "2017-12-27 12:00",
                "reservations", List.of(Map.of("resource_type", "physical:host", "min", 4, "max", 6, "hypervisor_properties", "", "resource_properties", "")),
                "events", List.of()));
        List<? extends Lease> all = leases.list();
        leases.get("l1");
        leases.update("l1", Map.of("prolong_for", "1d"));
        Assert.assertTrue(leases.delete("l1").isSuccess());

        RecordedRequest create = takeRequest();
        Assert.assertTrue(path(create).endsWith("/v1/leases"), path(create));
        Assert.assertEquals(new ObjectMapper().readTree(create.getBody().readUtf8()).get("name").asText(), "lease_foo");
        Assert.assertFalse(json(create).startsWith("{\"lease\""));
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/leases"));
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/leases/l1"));
        Assert.assertEquals(json(takeRequest()), "{\"prolong_for\":\"1d\"}");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getStatus(), "PENDING");
        Assert.assertEquals(all.get(0).getReservations().get(0).get("resource_type"), "physical:host");
    }

    public void hostsAndFloatingIps() throws Exception {
        respondWith(201, "{\"host\": " + HOST + "}");
        respondWith(200, "{\"hosts\": [" + HOST + "]}");
        respondWith(200, "{\"host\": " + HOST + "}");
        respondWith(204);
        respondWith(200, "{\"allocations\": [{\"resource_id\": \"1\", \"reservations\": []}]}");
        respondWith(200, "{\"allocation\": {\"resource_id\": \"1\", \"reservations\": [{\"lease_id\": \"l1\"}]}}");
        respondWith(200, "{\"resource_properties\": [{\"property\": \"gpu\", \"private\": false, \"values\": [\"a100\"]}]}");
        respondWith(200, "{\"resource_property\": {\"property_name\": \"gpu\", \"private\": true}}");
        respondWith(201, "{\"floatingip\": {\"id\": \"f1\", \"floating_network_id\": \"net1\", \"floating_ip_address\": \"172.24.4.101\", \"reservable\": true}}");
        respondWith(200, "{\"floatingips\": []}");
        respondWith(204);

        var reservation = osv3().reservation();
        ReservableHost host = reservation.hosts().create(Map.of("name", "compute-1", "gpu", "a100"));
        reservation.hosts().list();
        reservation.hosts().update("1", Map.of("gpu", "h100"));
        Assert.assertTrue(reservation.hosts().delete("1").isSuccess());
        reservation.hosts().listAllocations(Map.of("lease_id", "l1"));
        Map<String, Object> allocation = reservation.hosts().getAllocation("1");
        List<Map<String, Object>> properties = reservation.hosts().listProperties(true);
        Map<String, Object> property = reservation.hosts().updateProperty("gpu", true);
        ReservableFloatingIp fip = reservation.floatingIps().create("net1", "172.24.4.101");
        reservation.floatingIps().list();
        Assert.assertTrue(reservation.floatingIps().delete("f1").isSuccess());

        var hostCreate = new ObjectMapper().readTree(takeRequest().getBody().readUtf8());
        Assert.assertEquals(hostCreate.get("name").asText(), "compute-1");
        Assert.assertEquals(hostCreate.get("gpu").asText(), "a100");
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/os-hosts"));
        RecordedRequest update = takeRequest();
        Assert.assertTrue(path(update).endsWith("/v1/os-hosts/1"));
        Assert.assertEquals(json(update), "{\"gpu\":\"h100\"}");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/os-hosts/allocations?lease_id=l1"));
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/os-hosts/1/allocation"));
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/os-hosts/properties?detail=True"));
        RecordedRequest prop = takeRequest();
        Assert.assertEquals(prop.getMethod(), "PATCH");
        Assert.assertEquals(json(prop), "{\"private\":true}");
        var fipCreate = new ObjectMapper().readTree(takeRequest().getBody().readUtf8());
        Assert.assertEquals(fipCreate.get("floating_ip_address").asText(), "172.24.4.101");
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/floatingips"));
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/floatingips/f1"));
        Assert.assertEquals(host.getAttributes().get("gpu"), "a100");
        Assert.assertEquals(host.getHypervisorVersion(), Long.valueOf(2010001));
        Assert.assertEquals(((List<?>) allocation.get("reservations")).size(), 1);
        Assert.assertEquals(properties.get(0).get("property"), "gpu");
        Assert.assertEquals(property.get("private"), Boolean.TRUE);
        Assert.assertEquals(fip.getFloatingIpAddress(), "172.24.4.101");
    }
}
