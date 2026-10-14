package org.openstack4j.api.magnum;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.model.magnum.Clustertemplate;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Test(suiteName = "magnum/ClustertemplateShow")
public class MagnumClustertemplateShowTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.MAGNUM;
    }

    public void showClustertemplate() throws Exception {
        respondWith(200, "{\"uuid\": \"ct1\", \"name\": \"k8s\", \"coe\": \"kubernetes\", \"image_id\": \"fedora\","
                + " \"floating_ip_enabled\": true, \"tls_disabled\": false}");

        Clustertemplate template = osv3().magnum().showClustertemplate("ct1");

        RecordedRequest request = takeRequest();
        assertEquals(request.getMethod(), "GET");
        assertTrue(request.getPath().endsWith("/v1/clustertemplates/ct1"), request.getPath());
        assertEquals(template.getUuid(), "ct1");
        assertEquals(template.getName(), "k8s");
        assertEquals(template.getCoe(), "kubernetes");
        assertEquals(template.getImageId(), "fedora");
        assertEquals(template.getFloatingIpEnabled(), Boolean.TRUE);
    }

    public void showClustertemplate404() throws Exception {
        respondWith(404, "{\"errors\": [{\"detail\": \"nope\"}]}");

        assertNull(osv3().magnum().showClustertemplate("missing"));

        takeRequest();
    }
}
