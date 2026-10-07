package org.openstack4j.api.octavia.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.octavia.ext.FlavorProfile;
import org.openstack4j.model.octavia.ext.OctaviaFlavor;
import org.openstack4j.model.octavia.options.FlavorProfileOptions;
import org.openstack4j.model.octavia.options.OctaviaFlavorOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Octavia/Ext/Flavors")
public class FlavorTests extends AbstractOctaviaExtTest {

    private static final String FLAVOR = "{\"id\": \"8f94060c\", \"name\": \"Basic\", \"description\": \"A basic standalone Octavia load balancer.\", \"enabled\": true, \"flavor_profile_id\": \"5712097e\"}";
    private static final String PROFILE = "{\"id\": \"5712097e\", \"name\": \"amphora-act-stdby\", \"provider_name\": \"amphora\", \"flavor_data\": \"{\\\"loadbalancer_topology\\\": \\\"ACTIVE_STANDBY\\\"}\"}";

    public void flavors() throws Exception {
        respondWith(201, "{\"flavor\": " + FLAVOR + "}");
        respondWith(200, "{\"flavors\": [" + FLAVOR + "]}");
        respondWith(200, "{\"flavor\": " + FLAVOR + "}");
        respondWith(200, "{\"flavor\": " + FLAVOR + "}");
        respondWith(204);

        var flavors = osv3().octavia().flavors();
        OctaviaFlavor created = flavors.create(OctaviaFlavorOptions.create("Basic", "5712097e").description("A basic standalone Octavia load balancer."));
        List<? extends OctaviaFlavor> all = flavors.list();
        flavors.get("8f94060c");
        flavors.update("8f94060c", OctaviaFlavorOptions.update().enabled(false));
        Assert.assertTrue(flavors.delete("8f94060c").isSuccess());

        RecordedRequest create = expect("POST", "/v2.0/lbaas/flavors");
        Assert.assertEquals(body(create).get("flavor").get("flavor_profile_id").asText(), "5712097e");
        expect("GET", "/v2.0/lbaas/flavors");
        expect("GET", "/v2.0/lbaas/flavors/8f94060c");
        RecordedRequest update = expect("PUT", "/v2.0/lbaas/flavors/8f94060c");
        Assert.assertEquals(body(update).toString(), "{\"flavor\":{\"enabled\":false}}");
        expect("DELETE", "/v2.0/lbaas/flavors/8f94060c");
        Assert.assertTrue(created.isEnabled());
        Assert.assertEquals(all.get(0).getFlavorProfileId(), "5712097e");
    }

    public void flavorProfiles() throws Exception {
        respondWith(201, "{\"flavorprofile\": " + PROFILE + "}");
        respondWith(200, "{\"flavorprofiles\": [" + PROFILE + "]}");
        respondWith(200, "{\"flavorprofile\": " + PROFILE + "}");
        respondWith(200, "{\"flavorprofile\": " + PROFILE + "}");
        respondWith(204);

        var profiles = osv3().octavia().flavorProfiles();
        FlavorProfile created = profiles.create(FlavorProfileOptions.create("amphora-act-stdby", "amphora", "{\"loadbalancer_topology\": \"ACTIVE_STANDBY\"}"));
        List<? extends FlavorProfile> all = profiles.list();
        profiles.get("5712097e");
        profiles.update("5712097e", FlavorProfileOptions.update().name("renamed"));
        profiles.delete("5712097e");

        RecordedRequest create = expect("POST", "/v2.0/lbaas/flavorprofiles");
        Assert.assertEquals(body(create).get("flavorprofile").get("flavor_data").asText(), "{\"loadbalancer_topology\": \"ACTIVE_STANDBY\"}");
        expect("GET", "/v2.0/lbaas/flavorprofiles");
        expect("GET", "/v2.0/lbaas/flavorprofiles/5712097e");
        Assert.assertEquals(body(expect("PUT", "/v2.0/lbaas/flavorprofiles/5712097e")).get("flavorprofile").size(), 1);
        expect("DELETE", "/v2.0/lbaas/flavorprofiles/5712097e");
        Assert.assertEquals(created.getProviderName(), "amphora");
        Assert.assertTrue(all.get(0).getFlavorData().contains("ACTIVE_STANDBY"));
    }
}
