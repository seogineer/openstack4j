package org.openstack4j.api.manila.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.manila.ext.ShareNetworkInfo;
import org.openstack4j.model.manila.ext.ShareTypeInfo;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/ShareNetworkTypeExt")
public class ShareNetworkTypeExtTests extends AbstractManilaExtTest {

    private static final String P = "/v2/b80f8d4e28b74188858b654cb1fccf7d";
    private static final String VERSION = "X-OpenStack-Manila-API-Version";
    private static final String ROOT = "{\"versions\": [{\"id\": \"v1.0\", \"status\": \"DEPRECATED\", \"version\": \"\", \"min_version\": \"\"},"
            + " {\"id\": \"v2.0\", \"status\": \"CURRENT\", \"version\": \"2.99\", \"min_version\": \"2.0\"}]}";
    private static final String NOT_FOUND = "{\"itemNotFound\": {\"message\": \"nope\", \"code\": 404}}";
    private static final String NETWORK = "{\"id\": \"sn1\", \"name\": \"net\", \"description\": \"d\", \"project_id\": \"p1\","
            + " \"status\": \"active\", \"security_service_update_support\": true, \"created_at\": \"2020-01-01T00:00:00.000000\","
            + " \"share_network_subnets\": [{\"id\": \"sub1\", \"availability_zone\": null, \"neutron_net_id\": \"nn1\","
            + " \"neutron_subnet_id\": \"ns1\", \"network_type\": \"vlan\", \"segmentation_id\": 100, \"cidr\": \"10.0.0.0/24\","
            + " \"ip_version\": 4, \"gateway\": \"10.0.0.1\", \"mtu\": 1500}]}";
    private static final String PUBLIC_TYPE = "{\"id\": \"t1\", \"name\": \"default\", \"share_type_access:is_public\": true,"
            + " \"description\": \"d1\", \"is_default\": true, \"extra_specs\": {\"driver_handles_share_servers\": \"True\"},"
            + " \"required_extra_specs\": {\"driver_handles_share_servers\": \"True\"}}";
    private static final String PRIVATE_TYPE = "{\"id\": \"t2\", \"name\": \"priv\", \"share_type_access:is_public\": false,"
            + " \"description\": \"d2\", \"is_default\": false, \"extra_specs\": {}, \"required_extra_specs\": {}}";

    @AfterMethod
    public void clearMicroVersions() {
        osv3().share().microVersions().clear();
    }

    /** Pins the session microversion; consumes the root request of the first use, or drains the unused answer. */
    private void useVersion(String version) throws Exception {
        int before = server.getRequestCount();
        respondWith(300, ROOT);
        osv3().share().microVersions().use(version);
        if (server.getRequestCount() > before) {
            Assert.assertEquals(decodedPath(takeRequest()), "/");
        } else {
            new okhttp3.OkHttpClient().newCall(new okhttp3.Request.Builder().url(server.url("/drain")).build()).execute().close();
            takeRequest();
        }
    }

    public void shareNetworksDetailAt265() throws Exception {
        useVersion("2.65");
        respondWith(200, "{\"share_networks\": [" + NETWORK + "]}");

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("project_id", "p");
        filters.put("all_tenants", "1");
        List<? extends ShareNetworkInfo> networks = osv3().share().shareNetworksExt().listDetails(filters);

        RecordedRequest request = expect("GET", "/share-networks/detail?all_tenants=1&project_id=p");
        Assert.assertEquals(request.getHeader(VERSION), "2.65");
        Assert.assertEquals(networks.size(), 1);
        ShareNetworkInfo network = networks.get(0);
        Assert.assertEquals(network.getName(), "net");
        Assert.assertEquals(network.getStatus(), "active");
        Assert.assertEquals(network.getSecurityServiceUpdateSupport(), Boolean.TRUE);
        Assert.assertNull(network.getNetworkAllocationUpdateSupport());
        Assert.assertEquals(network.getShareNetworkSubnets().size(), 1);
        Assert.assertEquals(network.getShareNetworkSubnets().get(0).getNeutronNetId(), "nn1");
        Assert.assertEquals(network.getShareNetworkSubnets().get(0).getSegmentationId(), Integer.valueOf(100));
    }

    public void shareNetworksListAtFloorAndNullFilters() throws Exception {
        respondWith(200, "{\"share_networks\": [{\"id\": \"sn1\", \"name\": \"net\", \"neutron_net_id\": \"nn1\", \"cidr\": \"10.0.0.0/24\"}]}");

        List<? extends ShareNetworkInfo> networks = osv3().share().shareNetworksExt().list(null);

        RecordedRequest request = expect("GET", P + "/share-networks");
        Assert.assertFalse(request.getPath().contains("?"), request.getPath());
        Assert.assertEquals(request.getHeader(VERSION), "2.7");
        Assert.assertNull(networks.get(0).getShareNetworkSubnets());
        Assert.assertEquals(networks.get(0).getAttributes().get("neutron_net_id"), "nn1");
    }

    public void shareNetworkGet() throws Exception {
        respondWith(200, "{\"share_network\": " + NETWORK + "}");

        ShareNetworkInfo network = osv3().share().shareNetworksExt().get("sn1");

        Assert.assertEquals(expect("GET", P + "/share-networks/sn1").getHeader(VERSION), "2.7");
        Assert.assertEquals(network.getId(), "sn1");
        Assert.assertEquals(network.getShareNetworkSubnets().get(0).getCidr(), "10.0.0.0/24");
    }

    public void shareNetworkGet404() throws Exception {
        respondWith(404, NOT_FOUND);

        Assert.assertNull(osv3().share().shareNetworksExt().get("sn1"));

        expect("GET", P + "/share-networks/sn1");
    }

    public void shareTypesIsPublicAll() throws Exception {
        respondWith(200, "{\"share_types\": [" + PUBLIC_TYPE + "," + PRIVATE_TYPE + "],"
                + " \"volume_types\": [" + PUBLIC_TYPE + "," + PRIVATE_TYPE + "]}");

        List<? extends ShareTypeInfo> types = osv3().share().shareTypesExt().list(Map.of("is_public", "all"));

        RecordedRequest request = expect("GET", P + "/types?is_public=all");
        Assert.assertEquals(request.getHeader(VERSION), "2.7");
        Assert.assertEquals(types.size(), 2);
        Assert.assertEquals(types.get(0).getIsPublic(), Boolean.TRUE);
        Assert.assertEquals(types.get(0).getIsDefault(), Boolean.TRUE);
        Assert.assertEquals(types.get(0).getDescription(), "d1");
        Assert.assertEquals(types.get(0).getExtraSpecs().get("driver_handles_share_servers"), "True");
        Assert.assertEquals(types.get(0).getRequiredExtraSpecs().get("driver_handles_share_servers"), "True");
        Assert.assertEquals(types.get(1).getIsPublic(), Boolean.FALSE);
        Assert.assertEquals(types.get(1).getIsDefault(), Boolean.FALSE);
        Assert.assertEquals(types.get(1).getName(), "priv");
    }

    public void shareTypesNullFilters() throws Exception {
        respondWith(200, "{\"share_types\": []}");

        Assert.assertTrue(osv3().share().shareTypesExt().list(null).isEmpty());

        Assert.assertFalse(expect("GET", P + "/types").getPath().contains("?"));
    }

    public void shareTypeGetTwoRoots() throws Exception {
        respondWith(200, "{\"share_type\": " + PUBLIC_TYPE + ", \"volume_type\": " + PUBLIC_TYPE + "}");

        ShareTypeInfo type = osv3().share().shareTypesExt().get("t1");

        expect("GET", P + "/types/t1");
        Assert.assertEquals(type.getId(), "t1");
        Assert.assertEquals(type.getName(), "default");
        Assert.assertEquals(type.getIsPublic(), Boolean.TRUE);
        Assert.assertEquals(type.getDescription(), "d1");
    }

    public void shareTypeGet404() throws Exception {
        respondWith(404, NOT_FOUND);

        Assert.assertNull(osv3().share().shareTypesExt().get("t1"));

        expect("GET", P + "/types/t1");
    }

    public void shareTypeLegacyKey() throws Exception {
        respondWith(200, "{\"share_type\": {\"id\": \"t3\", \"name\": \"old\", \"os-share-type-access:is_public\": false}}");

        ShareTypeInfo type = osv3().share().shareTypesExt().get("t3");

        expect("GET", P + "/types/t3");
        Assert.assertEquals(type.getIsPublic(), Boolean.FALSE);
        Assert.assertFalse(type.getAttributes().containsKey("os-share-type-access:is_public"));
    }
}
