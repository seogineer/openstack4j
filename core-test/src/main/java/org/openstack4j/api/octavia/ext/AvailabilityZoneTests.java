package org.openstack4j.api.octavia.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.octavia.ext.AvailabilityZoneProfile;
import org.openstack4j.model.octavia.ext.OctaviaAvailabilityZone;
import org.openstack4j.model.octavia.options.AvailabilityZoneProfileOptions;
import org.openstack4j.model.octavia.options.OctaviaAvailabilityZoneOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Octavia/Ext/AvailabilityZones")
public class AvailabilityZoneTests extends AbstractOctaviaExtTest {

    private static final String AZ = "{\"name\": \"my_az\", \"description\": \"My availability zone.\", \"enabled\": true, \"availability_zone_profile_id\": \"5712097e\"}";
    private static final String PROFILE = "{\"id\": \"5712097e\", \"name\": \"some_az\", \"provider_name\": \"amphora\", \"availability_zone_data\": \"{\\\"compute_zone\\\": \\\"az1\\\"}\"}";

    public void availabilityZones() throws Exception {
        respondWith(201, "{\"availability_zone\": " + AZ + "}");
        respondWith(200, "{\"availability_zones\": [" + AZ + "]}");
        respondWith(200, "{\"availability_zone\": " + AZ + "}");
        respondWith(200, "{\"availability_zone\": " + AZ + "}");
        respondWith(204);

        var zones = osv3().octavia().availabilityZones();
        OctaviaAvailabilityZone created = zones.create(OctaviaAvailabilityZoneOptions.create("my_az", "5712097e").description("My availability zone."));
        List<? extends OctaviaAvailabilityZone> all = zones.list();
        zones.get("my_az");
        zones.update("my_az", OctaviaAvailabilityZoneOptions.update().enabled(false));
        Assert.assertTrue(zones.delete("my_az").isSuccess());

        RecordedRequest create = expect("POST", "/v2.0/lbaas/availabilityzones");
        Assert.assertEquals(body(create).get("availability_zone").get("availability_zone_profile_id").asText(), "5712097e");
        expect("GET", "/v2.0/lbaas/availabilityzones");
        expect("GET", "/v2.0/lbaas/availabilityzones/my_az");
        Assert.assertEquals(body(expect("PUT", "/v2.0/lbaas/availabilityzones/my_az")).toString(), "{\"availability_zone\":{\"enabled\":false}}");
        expect("DELETE", "/v2.0/lbaas/availabilityzones/my_az");
        Assert.assertEquals(created.getName(), "my_az");
        Assert.assertTrue(all.get(0).isEnabled());
    }

    public void availabilityZoneProfiles() throws Exception {
        respondWith(201, "{\"availability_zone_profile\": " + PROFILE + "}");
        respondWith(200, "{\"availability_zone_profiles\": [" + PROFILE + "]}");
        respondWith(200, "{\"availability_zone_profile\": " + PROFILE + "}");
        respondWith(200, "{\"availability_zone_profile\": " + PROFILE + "}");
        respondWith(204);

        var profiles = osv3().octavia().availabilityZoneProfiles();
        AvailabilityZoneProfile created = profiles.create(AvailabilityZoneProfileOptions.create("some_az", "amphora", "{\"compute_zone\": \"az1\"}"));
        profiles.list();
        profiles.get("5712097e");
        profiles.update("5712097e", AvailabilityZoneProfileOptions.update().name("renamed"));
        profiles.delete("5712097e");

        RecordedRequest create = expect("POST", "/v2.0/lbaas/availabilityzoneprofiles");
        Assert.assertEquals(body(create).get("availability_zone_profile").get("provider_name").asText(), "amphora");
        expect("GET", "/v2.0/lbaas/availabilityzoneprofiles");
        expect("GET", "/v2.0/lbaas/availabilityzoneprofiles/5712097e");
        expect("PUT", "/v2.0/lbaas/availabilityzoneprofiles/5712097e");
        expect("DELETE", "/v2.0/lbaas/availabilityzoneprofiles/5712097e");
        Assert.assertTrue(created.getAvailabilityZoneData().contains("az1"));
    }
}
