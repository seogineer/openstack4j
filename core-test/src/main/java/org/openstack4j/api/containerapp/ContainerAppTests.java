package org.openstack4j.api.containerapp;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "ContainerApp")
public class ContainerAppTests extends AbstractTest {

    private static final String VERSION = "OpenStack-API-Version";

    @Override
    protected Service service() {
        return Service.CONTAINER_APP;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    private static Map<?, ?> body(RecordedRequest r) throws Exception {
        return new ObjectMapper().readValue(r.getBody().readUtf8(), Map.class);
    }

    private void expect(String method, String path, String version) throws Exception {
        RecordedRequest r = takeRequest();
        Assert.assertEquals(r.getMethod(), method, path);
        Assert.assertEquals(path(r), path);
        Assert.assertEquals(r.getHeader(VERSION), version, path);
    }

    public void catalogEntriesResolveByKnownNameFirst() {
        Assert.assertEquals(org.openstack4j.api.types.ServiceType.forCatalogEntry("container", "zun"), org.openstack4j.api.types.ServiceType.CONTAINER_APP);
        Assert.assertEquals(org.openstack4j.api.types.ServiceType.forCatalogEntry("container-infra", "magnum"), org.openstack4j.api.types.ServiceType.MAGNUM);
        Assert.assertEquals(org.openstack4j.api.types.ServiceType.forCatalogEntry("container", "my-magnum"), org.openstack4j.api.types.ServiceType.MAGNUM);
        Assert.assertEquals(org.openstack4j.api.types.ServiceType.forCatalogEntry("compute", "nova"), org.openstack4j.api.types.ServiceType.COMPUTE);
        Assert.assertEquals(org.openstack4j.api.types.ServiceType.forCatalogEntry("compute", "custom"), org.openstack4j.api.types.ServiceType.COMPUTE);
    }

    public void ttyAloneKeepsTheLegacyCreateSchema() throws Exception {
        respondWith(202, "{\"uuid\": \"c9\"}");
        osv3().containerApp().createContainer(Map.of("image", "cirros", "command", "/bin/sh", "tty", true, "interactive", true), false);
        Assert.assertNull(takeRequest().getHeader(VERSION));
    }

    public void containersAndActions() throws Exception {
        respondWith(200, "{\"versions\": [{\"id\": \"v1\", \"max_version\": \"1.40\", \"min_version\": \"1.1\"}]}");
        respondWith(200, "{\"id\": \"v1\"}");
        respondWith(200, "{\"containers\": [{\"uuid\": \"c1\", \"name\": \"web\"}], \"next\": null}");
        respondWith(202, "{\"uuid\": \"c2\", \"status\": \"Creating\"}");
        respondWith(202, "{\"uuid\": \"c3\"}");
        respondWith(200, "{\"uuid\": \"c1\", \"status\": \"Running\"}");
        respondWith(200, "{\"uuid\": \"c1\", \"memory\": \"1024\"}");
        respondWith(204);
        respondWith(204);
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"uuid\": \"c1\", \"name\": \"web2\"}");
        respondWith(202, "{\"uuid\": \"c1\", \"cpu\": 2.0}");
        respondWith(200, "");
        respondWith(200, "{\"output\": \"hello\\n\", \"exit_code\": 0, \"exec_id\": null, \"url\": null}");
        respondWith(200, "{\"exec_id\": \"e1\", \"url\": \"ws://x\"}");
        respondWith(200, "\"line1\\nline2\\n\"");
        respondWith(200, "{\"Titles\": [\"PID\"], \"Processes\": [[\"1\"]]}");
        respondWith(200, "{\"CPU %\": 0.1}");
        respondWith(200, "\"ws://127.0.0.1:6784/?token=t\"");
        respondWith(200, "{\"data\": \"dGFy\", \"stat\": {\"name\": \"etc\"}}");
        respondWith(200);
        respondWith(202, "{\"uuid\": \"i9\"}");
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"networks\": [{\"net_id\": \"n1\", \"port_id\": \"p1\", \"fixed_ips\": []}]}");
        respondWith(200, "{\"uuid\": \"c1\", \"security_groups\": [\"web\"]}");
        respondWith(200, "{\"uuid\": \"c1\", \"security_groups\": []}");
        respondWith(200, "{\"containerActions\": [{\"action\": \"create\", \"request_id\": \"req-1\"}]}");
        respondWith(200, "{\"action\": \"create\", \"events\": [{\"event\": \"compute__do_container_create\"}]}");
        respondWith(404, "{\"errors\": [{\"detail\": \"not found\"}]}");

        var z = osv3().containerApp();
        Assert.assertEquals(((List<?>) z.versions().get("versions")).size(), 1);
        Assert.assertEquals(z.version().get("id"), "v1");
        Assert.assertEquals(((List<?>) z.listContainers(Map.of("status", "Running")).get("containers")).size(), 1);
        Assert.assertEquals(z.createContainer(Map.of("image", "nginx", "command", List.of("nginx", "-g", "daemon off;")), true).get("uuid"), "c2");
        z.createContainer(Map.of("image", "cirros", "entrypoint", List.of("/bin/sh"), "privileged", true), false);
        Assert.assertEquals(z.getContainer("web").get("status"), "Running");
        Assert.assertEquals(z.updateContainer("c1", Map.of("memory", 1024)).get("memory"), "1024");
        Assert.assertTrue(z.deleteContainer("c1", false, false).isSuccess());
        Assert.assertTrue(z.deleteContainer("c1", true, true).isSuccess());
        Assert.assertTrue(z.startContainer("c1").isSuccess());
        Assert.assertTrue(z.stopContainer("c1", 10).isSuccess());
        Assert.assertTrue(z.rebootContainer("c1", null).isSuccess());
        Assert.assertTrue(z.pauseContainer("c1").isSuccess());
        Assert.assertTrue(z.unpauseContainer("c1").isSuccess());
        Assert.assertTrue(z.killContainer("c1", "SIGKILL").isSuccess());
        Assert.assertTrue(z.rebuildContainer("c1", Map.of("image", "nginx:latest")).isSuccess());
        Assert.assertEquals(z.renameContainer("c1", "web2").get("name"), "web2");
        z.resizeContainer("c1", Map.of("cpu", 2));
        Assert.assertTrue(z.resizeContainerTty("c1", 24, 80).isSuccess());
        Assert.assertEquals(z.executeContainer("c1", "echo hello", true, false).get("exit_code"), 0);
        Assert.assertEquals(z.resizeExec("c1", "e1", 24, 80).get("url"), "ws://x");
        Assert.assertEquals(z.containerLogs("c1", Map.of("tail", "2")), "line1\nline2\n");
        Assert.assertEquals(z.containerTop("c1", "aux").get("Titles"), List.of("PID"));
        Assert.assertEquals(z.containerStats("c1").get("CPU %"), 0.1);
        Assert.assertEquals(z.attachContainer("c1"), "ws://127.0.0.1:6784/?token=t");
        Assert.assertEquals(z.getArchive("c1", "/etc").get("data"), "dGFy");
        Assert.assertTrue(z.putArchive("c1", "/tmp", "dGFy").isSuccess());
        Assert.assertEquals(z.commitContainer("c1", "myrepo", "v1").get("uuid"), "i9");
        Assert.assertTrue(z.attachNetwork("c1", Map.of("network", "private")).isSuccess());
        Assert.assertTrue(z.detachNetwork("c1", Map.of("port", "p1")).isSuccess());
        Assert.assertEquals(z.listContainerNetworks("c1").get(0).get("net_id"), "n1");
        z.addSecurityGroup("c1", "web");
        z.removeSecurityGroup("c1", "web");
        Assert.assertEquals(z.listContainerActions("c1").get(0).get("request_id"), "req-1");
        Assert.assertEquals(((List<?>) z.getContainerAction("c1", "req-1").get("events")).size(), 1);
        Assert.assertNull(z.getContainer("missing"));

        expect("GET", "/", null);
        expect("GET", "/v1", null);
        expect("GET", "/v1/containers?status=Running", null);
        RecordedRequest r = takeRequest();
        Assert.assertEquals(path(r), "/v1/containers?run=true");
        Assert.assertEquals(r.getHeader(VERSION), "container 1.20");
        Assert.assertEquals(body(r).get("command"), List.of("nginx", "-g", "daemon off;"));
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/containers?run=false");
        Assert.assertEquals(r.getHeader(VERSION), "container 1.40");
        expect("GET", "/v1/containers/web", null);
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PATCH");
        Assert.assertEquals(body(r).get("memory"), 1024);
        expect("DELETE", "/v1/containers/c1", null);
        r = takeRequest();
        Assert.assertTrue(path(r).contains("force=true") && path(r).contains("stop=true"), path(r));
        Assert.assertEquals(r.getHeader(VERSION), "container 1.12");
        expect("POST", "/v1/containers/c1/start", null);
        expect("POST", "/v1/containers/c1/stop?timeout=10", null);
        expect("POST", "/v1/containers/c1/reboot", null);
        expect("POST", "/v1/containers/c1/pause", null);
        expect("POST", "/v1/containers/c1/unpause", null);
        expect("POST", "/v1/containers/c1/kill?signal=SIGKILL", null);
        expect("POST", "/v1/containers/c1/rebuild?image=nginx:latest", null);
        expect("POST", "/v1/containers/c1/rename?name=web2", "container 1.13");
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/containers/c1/resize_container");
        Assert.assertEquals(r.getHeader(VERSION), "container 1.19");
        Assert.assertEquals(body(r).get("cpu"), 2);
        r = takeRequest();
        Assert.assertTrue(path(r).startsWith("/v1/containers/c1/resize?") && path(r).contains("h=24") && path(r).contains("w=80"), path(r));
        r = takeRequest();
        Assert.assertTrue(path(r).startsWith("/v1/containers/c1/execute?") && path(r).contains("command=echo hello") && path(r).contains("run=true")
                && path(r).contains("interactive=false"), path(r));
        r = takeRequest();
        Assert.assertTrue(path(r).startsWith("/v1/containers/c1/execute_resize?") && path(r).contains("exec_id=e1"), path(r));
        expect("GET", "/v1/containers/c1/logs?tail=2", null);
        expect("GET", "/v1/containers/c1/top?ps_args=aux", null);
        expect("GET", "/v1/containers/c1/stats", null);
        expect("GET", "/v1/containers/c1/attach", null);
        expect("GET", "/v1/containers/c1/get_archive?path=/etc", "container 1.25");
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/containers/c1/put_archive?path=/tmp");
        Assert.assertEquals(r.getHeader(VERSION), "container 1.25");
        Assert.assertEquals(body(r).get("data"), "dGFy");
        r = takeRequest();
        Assert.assertTrue(path(r).contains("repository=myrepo") && path(r).contains("tag=v1"), path(r));
        expect("POST", "/v1/containers/c1/network_attach?network=private", "container 1.8");
        expect("POST", "/v1/containers/c1/network_detach?port=p1", "container 1.6");
        expect("GET", "/v1/containers/c1/network_list", "container 1.18");
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/containers/c1/add_security_group");
        Assert.assertEquals(r.getHeader(VERSION), "container 1.14");
        Assert.assertEquals(body(r).get("name"), "web");
        expect("POST", "/v1/containers/c1/remove_security_group", "container 1.14");
        expect("GET", "/v1/containers/c1/container_actions", null);
        expect("GET", "/v1/containers/c1/container_actions/req-1", null);
        takeRequest();
    }

    public void imagesHostsServicesCapsules() throws Exception {
        respondWith(200, "{\"images\": [{\"uuid\": \"i1\", \"repo\": \"nginx\"}]}");
        respondWith(200, "{\"uuid\": \"i1\", \"repo\": \"nginx\"}");
        respondWith(202, "{\"uuid\": \"i2\", \"repo\": \"redis\"}");
        respondWith(200);
        respondWith(200, "[{\"name\": \"nginx\", \"is_official\": true}]");
        respondWith(200, "{\"hosts\": [{\"uuid\": \"h1\", \"hostname\": \"compute-1\"}]}");
        respondWith(200, "{\"uuid\": \"h1\", \"cpus\": 8}");
        respondWith(200, "{\"services\": [{\"id\": 1, \"binary\": \"zun-compute\", \"state\": \"up\"}]}");
        respondWith(200);
        respondWith(200, "{\"service\": {\"host\": \"compute-1\", \"binary\": \"zun-compute\", \"disabled\": false}}");
        respondWith(200, "{\"service\": {\"host\": \"compute-1\", \"binary\": \"zun-compute\", \"disabled\": true, \"disabled_reason\": \"maint\"}}");
        respondWith(200, "{\"service\": {\"host\": \"compute-1\", \"binary\": \"zun-compute\", \"forced_down\": true}}");
        respondWith(200, "{\"capsules\": [{\"uuid\": \"cap1\"}]}");
        respondWith(200, "{\"uuid\": \"cap1\", \"status\": \"Running\"}");
        respondWith(202, "{\"uuid\": \"cap2\"}");
        respondWith(204);

        var z = osv3().containerApp();
        Assert.assertEquals(((List<?>) z.listImages(null).get("images")).size(), 1);
        Assert.assertEquals(z.getImage("i1").get("repo"), "nginx");
        Assert.assertEquals(z.pullImage(Map.of("repo", "redis", "host", "compute-1")).get("uuid"), "i2");
        Assert.assertTrue(z.deleteImage("i1").isSuccess());
        Assert.assertEquals(z.searchImages("nginx", Map.of("exact_match", "true")).get(0).get("name"), "nginx");
        Assert.assertEquals(((List<?>) z.listHosts(null).get("hosts")).size(), 1);
        Assert.assertEquals(z.getHost("compute-1").get("cpus"), 8);
        Assert.assertEquals(z.listServices().get(0).get("binary"), "zun-compute");
        Assert.assertTrue(z.deleteService("compute-1", "zun-compute").isSuccess());
        Assert.assertEquals(z.enableService("compute-1", "zun-compute").get("disabled"), false);
        Assert.assertEquals(z.disableService("compute-1", "zun-compute", "maint").get("disabled_reason"), "maint");
        Assert.assertEquals(z.forceDownService("compute-1", "zun-compute", true).get("forced_down"), true);
        Assert.assertEquals(((List<?>) z.listCapsules(null).get("capsules")).size(), 1);
        Assert.assertEquals(z.getCapsule("cap1").get("status"), "Running");
        Assert.assertEquals(z.createCapsule(Map.of("kind", "capsule", "metadata", Map.of("name", "demo"), "spec", Map.of())).get("uuid"), "cap2");
        Assert.assertTrue(z.deleteCapsule("cap1").isSuccess());

        expect("GET", "/v1/images", null);
        expect("GET", "/v1/images/i1", null);
        RecordedRequest r = takeRequest();
        Assert.assertEquals(body(r).get("repo"), "redis");
        expect("DELETE", "/v1/images/i1", null);
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/images/nginx/search?exact_match=true");
        expect("GET", "/v1/hosts", "container 1.4");
        expect("GET", "/v1/hosts/compute-1", "container 1.4");
        expect("GET", "/v1/services", null);
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "DELETE");
        Assert.assertTrue(path(r).startsWith("/v1/services?") && path(r).contains("host=compute-1") && path(r).contains("binary=zun-compute"), path(r));
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PUT");
        Assert.assertTrue(path(r).startsWith("/v1/services/enable?") && path(r).contains("host=compute-1") && path(r).contains("binary=zun-compute"), path(r));
        Assert.assertEquals(r.getBodySize(), 0L, "parameters only in the query: pecan merges a repeated body key into a list");
        r = takeRequest();
        Assert.assertTrue(path(r).startsWith("/v1/services/disable?") && path(r).contains("disabled_reason=maint"), path(r));
        r = takeRequest();
        Assert.assertTrue(path(r).startsWith("/v1/services/force_down?") && path(r).contains("forced_down=true"), path(r));
        Assert.assertEquals(r.getBodySize(), 0L);
        expect("GET", "/v1/capsules", "container 1.32");
        expect("GET", "/v1/capsules/cap1", "container 1.32");
        r = takeRequest();
        Assert.assertEquals(r.getHeader(VERSION), "container 1.32");
        Assert.assertEquals(((Map<?, ?>) body(r).get("template")).get("kind"), "capsule");
        expect("DELETE", "/v1/capsules/cap1", "container 1.32");
    }

    public void quotasNetworksRegistriesAndOptInVersion() throws Exception {
        respondWith(200, "{\"containers\": 40, \"memory\": 51200, \"cpu\": 20, \"disk\": 100}");
        respondWith(200, "{\"containers\": {\"limit\": 40, \"in_use\": 1}}");
        respondWith(200, "{\"containers\": 40}");
        respondWith(200, "{\"containers\": 10}");
        respondWith(200);
        respondWith(200, "{\"containers\": 40}");
        respondWith(200, "{\"containers\": 50}");
        respondWith(200, "{\"availability_zones\": [{\"availability_zone\": \"nova\"}]}");
        respondWith(200, "{\"uuid\": \"n1\", \"name\": \"net\"}");
        respondWith(204);
        respondWith(200, "{\"registries\": [{\"uuid\": \"r1\", \"domain\": \"docker.io\"}]}");
        respondWith(200, "{\"registry\": {\"uuid\": \"r1\", \"domain\": \"docker.io\", \"password\": \"***\"}}");
        respondWith(201, "{\"registry\": {\"uuid\": \"r2\", \"domain\": \"quay.io\"}}");
        respondWith(200, "{\"registry\": {\"uuid\": \"r2\", \"name\": \"quay\"}}");
        respondWith(204);
        respondWith(404, "{\"errors\": [{\"detail\": \"not found\"}]}");
        respondWith(200, "{\"containers\": []}");
        respondWith(200, "{\"hosts\": []}");
        respondWith(200, "{\"containers\": []}");

        var z = osv3().containerApp();
        Assert.assertEquals(z.getQuotas("p1", false).get("containers"), 40);
        Assert.assertEquals(((Map<?, ?>) z.getQuotas("p1", true).get("containers")).get("in_use"), 1);
        z.getDefaultQuotas("p1");
        Assert.assertEquals(z.updateQuotas("p1", Map.of("containers", 10)).get("containers"), 10);
        Assert.assertTrue(z.deleteQuotas("p1").isSuccess());
        z.getQuotaClass("default");
        Assert.assertEquals(z.updateQuotaClass("default", Map.of("containers", 50)).get("containers"), 50);
        Assert.assertEquals(z.listAvailabilityZones().get(0).get("availability_zone"), "nova");
        Assert.assertEquals(z.createNetwork(Map.of("name", "net", "neutron_net_id", "nn1")).get("uuid"), "n1");
        Assert.assertTrue(z.deleteNetwork("n1").isSuccess());
        Assert.assertEquals(((List<?>) z.listRegistries(null).get("registries")).size(), 1);
        Assert.assertEquals(z.getRegistry("r1").get("password"), "***");
        Assert.assertEquals(z.createRegistry(Map.of("domain", "quay.io", "username", "u", "password", "p")).get("uuid"), "r2");
        Assert.assertEquals(z.updateRegistry("r2", Map.of("name", "quay")).get("name"), "quay");
        Assert.assertTrue(z.deleteRegistry("r2").isSuccess());
        Assert.assertNull(z.getRegistry("missing"));
        z.useApiVersion("1.40");
        try {
            z.listContainers(null);
            z.listHosts(null);
        } finally {
            z.useApiVersion(null);
        }
        z.listContainers(null);
        Assert.assertThrows(IllegalArgumentException.class, () -> z.useApiVersion("2.0"));

        expect("GET", "/v1/quotas/p1", "container 1.26");
        expect("GET", "/v1/quotas/p1?usages=true", "container 1.26");
        expect("GET", "/v1/quotas/p1/defaults", "container 1.26");
        RecordedRequest r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PUT");
        Assert.assertEquals(body(r).get("containers"), 10);
        expect("DELETE", "/v1/quotas/p1", "container 1.26");
        expect("GET", "/v1/quota_classes/default", "container 1.26");
        expect("PUT", "/v1/quota_classes/default", "container 1.26");
        expect("GET", "/v1/availability_zones", null);
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/networks");
        Assert.assertEquals(body(r).get("neutron_net_id"), "nn1");
        expect("DELETE", "/v1/networks/n1", "container 1.27");
        expect("GET", "/v1/registries", "container 1.30");
        expect("GET", "/v1/registries/r1", "container 1.30");
        r = takeRequest();
        Assert.assertEquals(((Map<?, ?>) body(r).get("registry")).get("domain"), "quay.io");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PATCH");
        Assert.assertEquals(((Map<?, ?>) body(r).get("registry")).get("name"), "quay");
        expect("DELETE", "/v1/registries/r2", "container 1.30");
        takeRequest();
        expect("GET", "/v1/containers", "container 1.40");
        expect("GET", "/v1/hosts", "container 1.40");
        expect("GET", "/v1/containers", null);
    }
}
