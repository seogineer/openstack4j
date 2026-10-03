package org.openstack4j.api.network.ext2;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.ServiceFlavor;
import org.openstack4j.model.network.ext.ServiceProfile;
import org.openstack4j.model.network.options.ServiceFlavorOptions;
import org.openstack4j.model.network.options.ServiceProfileOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/ServiceFlavors")
public class ServiceFlavorTests extends AbstractNetworkingExtTest {

    public void serviceFlavors() throws Exception {
        String flavor = "{\"description\": \"\", \"enabled\": true, \"service_profiles\": [\"sp1\"], \"service_type\": \"L3_ROUTER_NAT\", \"id\": \"f1\", \"name\": \"router-flavor\"}";
        respondWith(201, "{\"flavor\": " + flavor + "}");
        respondWith(200, "{\"flavors\": [" + flavor + "]}");
        respondWith(200, "{\"flavor\": " + flavor + "}");
        respondWith(200, "{\"flavor\": " + flavor + "}");
        respondWith(201, "{\"service_profile\": {\"id\": \"sp1\"}}");
        respondWith(204);
        respondWith(204);

        var flavors = osv3().networking().serviceFlavors();
        ServiceFlavor created = flavors.create(ServiceFlavorOptions.create("router-flavor", "L3_ROUTER_NAT"));
        flavors.list();
        flavors.get("f1");
        flavors.update("f1", ServiceFlavorOptions.update().enabled(false));
        Assert.assertTrue(flavors.associateProfile("f1", "sp1").isSuccess());
        Assert.assertTrue(flavors.disassociateProfile("f1", "sp1").isSuccess());
        flavors.delete("f1");

        RecordedRequest create = expect("POST", "/v2.0/flavors");
        Assert.assertEquals(body(create).get("flavor").get("service_type").asText(), "L3_ROUTER_NAT");
        expect("GET", "/v2.0/flavors");
        expect("GET", "/v2.0/flavors/f1");
        Assert.assertFalse(body(expect("PUT", "/v2.0/flavors/f1")).get("flavor").get("enabled").asBoolean());
        RecordedRequest associate = expect("POST", "/v2.0/flavors/f1/service_profiles");
        Assert.assertEquals(body(associate).get("service_profile").get("id").asText(), "sp1");
        expect("DELETE", "/v2.0/flavors/f1/service_profiles/sp1");
        expect("DELETE", "/v2.0/flavors/f1");
        Assert.assertEquals(created.getServiceProfiles(), java.util.List.of("sp1"));
        Assert.assertTrue(created.isEnabled());
    }

    public void serviceProfiles() throws Exception {
        String profile = "{\"enabled\": true, \"metainfo\": \"{'foo': 'bar'}\", \"driver\": \"neutron.services.l3_router.service_providers.single_node.SingleNodeDriver\", \"id\": \"sp1\", \"description\": \"d\"}";
        respondWith(201, "{\"service_profile\": " + profile + "}");
        respondWith(200, "{\"service_profiles\": [" + profile + "]}");
        respondWith(200, "{\"service_profile\": " + profile + "}");
        respondWith(200, "{\"service_profile\": " + profile + "}");
        respondWith(204);

        var profiles = osv3().networking().serviceProfiles();
        ServiceProfile created = profiles.create(ServiceProfileOptions.create().driver("neutron.services.l3_router.service_providers.single_node.SingleNodeDriver").description("d"));
        profiles.list();
        profiles.get("sp1");
        profiles.update("sp1", ServiceProfileOptions.update().enabled(false));
        profiles.delete("sp1");

        RecordedRequest create = expect("POST", "/v2.0/service_profiles");
        Assert.assertEquals(body(create).get("service_profile").get("description").asText(), "d");
        expect("GET", "/v2.0/service_profiles");
        expect("GET", "/v2.0/service_profiles/sp1");
        expect("PUT", "/v2.0/service_profiles/sp1");
        expect("DELETE", "/v2.0/service_profiles/sp1");
        Assert.assertTrue(created.getDriver().endsWith("SingleNodeDriver"));
    }
}
