package org.openstack4j.api.identity.v3;

import java.util.List;
import java.util.Set;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.identity.v3.Credential;
import org.openstack4j.model.identity.v3.Endpoint;

import org.openstack4j.model.identity.v3.options.CredentialListOptions;
import org.openstack4j.model.identity.v3.options.EndpointListOptions;
import org.openstack4j.model.identity.v3.options.ServiceListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Filters of the service, endpoint and credential lists. */
@Test(groups = "idV3", suiteName = "Identity/V3/ListOptions")
public class KeystoneListOptionsTests extends AbstractTest {

    @Override
    protected AbstractTest.Service service() {
        return AbstractTest.Service.IDENTITY;
    }

    private static Set<String> query(RecordedRequest request) {
        String query = request.getRequestUrl().encodedQuery();
        return query == null ? Set.of() : Set.of(query.split("&"));
    }

    public void servicesFilters() throws Exception {
        respondWith(200, "{\"services\": [{\"id\": \"s1\", \"type\": \"compute\", \"name\": \"nova\", \"enabled\": true}]}");

        List<? extends org.openstack4j.model.identity.v3.Service> services = osv3().identity().serviceEndpoints().list(ServiceListOptions.create().type("compute").name("nova"));

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "GET");
        Assert.assertTrue(request.getRequestUrl().encodedPath().endsWith("/services"), request.getPath());
        Assert.assertEquals(query(request), Set.of("type=compute", "name=nova"));
        Assert.assertEquals(services.get(0).getName(), "nova");
    }

    public void endpointsFilters() throws Exception {
        respondWith(200, "{\"endpoints\": [{\"id\": \"e1\", \"interface\": \"internal\", \"service_id\": \"s\", \"region_id\": \"r\", \"url\": \"http://x:8774\"}]}");

        List<? extends Endpoint> endpoints = osv3().identity().serviceEndpoints()
                .listEndpoints(EndpointListOptions.create().iface(Facing.INTERNAL).serviceId("s").regionId("r"));

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getRequestUrl().encodedPath().endsWith("/endpoints"), request.getPath());
        Assert.assertEquals(query(request), Set.of("interface=internal", "service_id=s", "region_id=r"));
        Assert.assertEquals(endpoints.size(), 1);
        Assert.assertEquals(endpoints.get(0).getId(), "e1");
    }

    public void credentialsFilters() throws Exception {
        respondWith(200, "{\"credentials\": [{\"id\": \"c1\", \"user_id\": \"u\", \"type\": \"ec2\", \"blob\": \"{}\"}]}");

        List<? extends Credential> credentials = osv3().identity().credentials().list(CredentialListOptions.create().userId("u").type("ec2"));

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getRequestUrl().encodedPath().endsWith("/credentials"), request.getPath());
        Assert.assertEquals(query(request), Set.of("user_id=u", "type=ec2"));
        Assert.assertEquals(credentials.size(), 1);
    }

    public void emptyOptionsAndPlainListsSendNoQuery() throws Exception {
        respondWith(200, "{\"services\": []}");
        respondWith(200, "{\"endpoints\": []}");
        respondWith(200, "{\"credentials\": []}");
        respondWith(200, "{\"services\": []}");

        osv3().identity().serviceEndpoints().list(ServiceListOptions.create());
        osv3().identity().serviceEndpoints().listEndpoints(EndpointListOptions.create());
        osv3().identity().credentials().list(CredentialListOptions.create());
        osv3().identity().serviceEndpoints().list();

        for (int i = 0; i < 4; i++) {
            Assert.assertEquals(query(takeRequest()), Set.of());
        }
    }
}
