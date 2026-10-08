package org.openstack4j.api.tacker;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.common.Payloads;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "TackerSol")
public class TackerSolTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.TACKER;
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
        Assert.assertEquals(r.getHeader("Version"), version, path);
    }

    private static List<?> items(Map<String, Object> page) {
        return (List<?>) page.get("items");
    }

    public void vnfLcmV2() throws Exception {
        respondWith(200, "{\"uriPrefix\": \"/vnflcm/v2\", \"apiVersions\": [{\"version\": \"2.0.0\", \"isDeprecated\": false}]}");
        respondWith(Map.of("Content-Type", "application/json", "Link", "<http://127.0.0.1:9890/vnflcm/v2/vnf_instances?nextpage_opaque_marker=i2>;rel=\"next\""), 200,
                "[{\"id\": \"i1\", \"instantiationState\": \"NOT_INSTANTIATED\"}]");
        respondWith(Map.of("Content-Type", "application/json", "Location", "http://127.0.0.1:9890/vnflcm/v2/vnf_instances/i1"), 201,
                "{\"id\": \"i1\", \"vnfdId\": \"d1\"}");
        respondWith(200, "{\"id\": \"i1\", \"instantiationState\": \"INSTANTIATED\"}");
        respondWith(Map.of("Location", "http://127.0.0.1:9890/vnflcm/v2/vnf_lcm_op_occs/o1"), 202);
        respondWith(Map.of("Location", "http://127.0.0.1:9890/vnflcm/v2/vnf_lcm_op_occs/o2"), 202);
        respondWith(Map.of("Location", "http://127.0.0.1:9890/vnflcm/v2/vnf_lcm_op_occs/o3"), 202);
        respondWith(Map.of("Location", "http://127.0.0.1:9890/vnflcm/v2/vnf_lcm_op_occs/o4"), 202);
        respondWith(Map.of("Location", "http://127.0.0.1:9890/vnflcm/v2/vnf_lcm_op_occs/o5"), 202);
        respondWith(Map.of("Location", "http://127.0.0.1:9890/vnflcm/v2/vnf_lcm_op_occs/o6"), 202);
        respondWith(Map.of("Location", "http://127.0.0.1:9890/vnflcm/v2/vnf_lcm_op_occs/o7"), 202);
        respondWith(204);
        respondWith(200, "[{\"id\": \"o1\", \"operationState\": \"FAILED_TEMP\"}]");
        respondWith(200, "{\"id\": \"o1\", \"operation\": \"INSTANTIATE\"}");
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"id\": \"o1\", \"operationState\": \"FAILED\"}");
        respondWith(200, "[]");
        respondWith(201, "{\"id\": \"s1\", \"callbackUri\": \"http://nfvo/notify\"}");
        respondWith(200, "{\"id\": \"s1\"}");
        respondWith(204);
        respondWith(Map.of("Content-Type", "application/problem+json"), 404, "{\"status\": 404, \"detail\": \"VnfInstance x not found.\"}");
        respondWith(Map.of("Content-Type", "application/problem+json"), 409, "{\"status\": 409, \"detail\": \"VnfInstance i1 is not instantiated.\"}");

        var lcm = osv3().tacker().vnfLcm();
        Assert.assertEquals(lcm.apiVersions().get("uriPrefix"), "/vnflcm/v2");
        Map<String, Object> page = lcm.listVnfInstances(Map.of("filter", "(eq,vnfdId,d1)"));
        Assert.assertEquals(items(page).size(), 1);
        Assert.assertEquals(page.get("next"), "i2");
        Assert.assertEquals(lcm.createVnfInstance(Map.of("vnfdId", "d1", "vnfInstanceName", "vnf1")).get("id"), "i1");
        Assert.assertEquals(lcm.getVnfInstance("i1").get("instantiationState"), "INSTANTIATED");
        Assert.assertEquals(lcm.instantiate("i1", Map.of("flavourId", "simple")), "o1");
        Assert.assertEquals(lcm.scale("i1", Map.of("type", "SCALE_OUT", "aspectId", "VDU1_scale")), "o2");
        Assert.assertEquals(lcm.heal("i1", null), "o3");
        Assert.assertEquals(lcm.changeExtConn("i1", Map.of("extVirtualLinks", List.of())), "o4");
        Assert.assertEquals(lcm.changeVnfpkg("i1", Map.of("vnfdId", "d2", "additionalParams", Map.of())), "o5");
        Assert.assertEquals(lcm.updateVnfInstance("i1", Map.of("vnfInstanceName", "renamed")), "o6");
        Assert.assertEquals(lcm.terminate("i1", Map.of("terminationType", "FORCEFUL")), "o7");
        Assert.assertTrue(lcm.deleteVnfInstance("i1").isSuccess());
        Assert.assertEquals(items(lcm.listLcmOpOccs(null)).size(), 1);
        Assert.assertEquals(lcm.getLcmOpOcc("o1").get("operation"), "INSTANTIATE");
        Assert.assertTrue(lcm.retry("o1").isSuccess());
        Assert.assertTrue(lcm.rollback("o1").isSuccess());
        Assert.assertEquals(lcm.fail("o1").get("operationState"), "FAILED");
        Assert.assertTrue(items(lcm.listSubscriptions(null)).isEmpty());
        Assert.assertEquals(lcm.createSubscription(Map.of("callbackUri", "http://nfvo/notify")).get("id"), "s1");
        Assert.assertEquals(lcm.getSubscription("s1").get("id"), "s1");
        Assert.assertTrue(lcm.deleteSubscription("s1").isSuccess());
        Assert.assertNull(lcm.getVnfInstance("x"));
        ResponseException e = Assert.expectThrows(ResponseException.class, () -> lcm.terminate("i1", Map.of("terminationType", "FORCEFUL")));
        Assert.assertEquals(e.getMessage(), "VnfInstance i1 is not instantiated.");
        Assert.assertEquals(e.getStatus(), 409);

        expect("GET", "/vnflcm/v2/api_versions", "2.0.0");
        expect("GET", "/vnflcm/v2/vnf_instances?filter=(eq,vnfdId,d1)", "2.0.0");
        RecordedRequest r = takeRequest();
        Assert.assertEquals(path(r), "/vnflcm/v2/vnf_instances");
        Assert.assertEquals(body(r).get("vnfInstanceName"), "vnf1");
        expect("GET", "/vnflcm/v2/vnf_instances/i1", "2.0.0");
        r = takeRequest();
        Assert.assertEquals(path(r), "/vnflcm/v2/vnf_instances/i1/instantiate");
        Assert.assertEquals(body(r).get("flavourId"), "simple");
        expect("POST", "/vnflcm/v2/vnf_instances/i1/scale", "2.0.0");
        r = takeRequest();
        Assert.assertEquals(path(r), "/vnflcm/v2/vnf_instances/i1/heal");
        Assert.assertEquals(body(r), Map.of());
        expect("POST", "/vnflcm/v2/vnf_instances/i1/change_ext_conn", "2.0.0");
        expect("POST", "/vnflcm/v2/vnf_instances/i1/change_vnfpkg", "2.0.0");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PATCH");
        Assert.assertTrue(r.getHeader("Content-Type").startsWith("application/merge-patch+json"), r.getHeader("Content-Type"));
        expect("POST", "/vnflcm/v2/vnf_instances/i1/terminate", "2.0.0");
        expect("DELETE", "/vnflcm/v2/vnf_instances/i1", "2.0.0");
        expect("GET", "/vnflcm/v2/vnf_lcm_op_occs", "2.0.0");
        expect("GET", "/vnflcm/v2/vnf_lcm_op_occs/o1", "2.0.0");
        expect("POST", "/vnflcm/v2/vnf_lcm_op_occs/o1/retry", "2.0.0");
        expect("POST", "/vnflcm/v2/vnf_lcm_op_occs/o1/rollback", "2.0.0");
        expect("POST", "/vnflcm/v2/vnf_lcm_op_occs/o1/fail", "2.0.0");
        expect("GET", "/vnflcm/v2/subscriptions", "2.0.0");
        expect("POST", "/vnflcm/v2/subscriptions", "2.0.0");
        expect("GET", "/vnflcm/v2/subscriptions/s1", "2.0.0");
        expect("DELETE", "/vnflcm/v2/subscriptions/s1", "2.0.0");
        takeRequest();
        takeRequest();
    }

    public void vnfLcmV1Cancel() throws Exception {
        respondWith(Map.of("Location", "http://127.0.0.1:9890/vnflcm/v1/vnf_lcm_op_occs/o9"), 202);
        respondWith(202);

        var lcm = osv3().tacker().vnfLcmV1();
        Assert.assertEquals(lcm.instantiate("i1", Map.of("flavourId", "simple")), "o9");
        Assert.assertTrue(lcm.cancel("o9", "GRACEFUL").isSuccess());

        expect("POST", "/vnflcm/v1/vnf_instances/i1/instantiate", "1.3.0");
        RecordedRequest r = takeRequest();
        Assert.assertEquals(path(r), "/vnflcm/v1/vnf_lcm_op_occs/o9/cancel");
        Assert.assertEquals(body(r).get("cancelMode"), "GRACEFUL");
    }

    public void vnfPackages() throws Exception {
        respondWith(200, "[{\"id\": \"p1\", \"onboardingState\": \"ONBOARDED\"}]");
        respondWith(201, "{\"id\": \"p2\", \"onboardingState\": \"CREATED\"}");
        respondWith(200, "{\"id\": \"p2\"}");
        respondWith(200, "{\"operationalState\": \"DISABLED\"}");
        respondWith(202);
        respondWith(202);
        respondWith(Map.of("Content-Type", "application/zip"), 200, "PK-zip");
        respondWith(Map.of("Content-Type", "text/plain"), 200, "tosca_definitions_version: x");
        respondWith(Map.of("Content-Type", "application/octet-stream"), 200, "#!/bin/sh");
        respondWith(204);
        respondWith(Map.of("Content-Type", "application/problem+json"), 409, "{\"status\": 409, \"detail\": \"VNF Package p2 is in use.\"}");

        var pkgs = osv3().tacker().vnfPackages();
        Assert.assertEquals(items(pkgs.list(Map.of("all_records", "yes"))).size(), 1);
        Assert.assertEquals(pkgs.create(Map.of("key", "value")).get("id"), "p2");
        Assert.assertEquals(pkgs.get("p2").get("id"), "p2");
        Assert.assertEquals(pkgs.update("p2", Map.of("operationalState", "DISABLED")).get("operationalState"), "DISABLED");
        Assert.assertTrue(pkgs.uploadContent("p2", Payloads.create(new java.io.ByteArrayInputStream("PK".getBytes()))).isSuccess());
        Assert.assertTrue(pkgs.uploadContentFromUri("p2", Map.of("addressInformation", "http://host/pkg.zip")).isSuccess());
        File zip = File.createTempFile("pkg", ".zip");
        File vnfd = File.createTempFile("vnfd", ".yaml");
        File script = File.createTempFile("install", ".sh");
        try {
            Assert.assertTrue(pkgs.downloadContent("p2", zip).isSuccess());
            Assert.assertEquals(Files.readString(zip.toPath()), "PK-zip");
            Assert.assertTrue(pkgs.downloadVnfd("p2", "text/plain", vnfd).isSuccess());
            Assert.assertTrue(pkgs.downloadArtifact("p2", "Scripts/install.sh", script).isSuccess());
            Assert.assertEquals(Files.readString(script.toPath()), "#!/bin/sh");
        } finally {
            zip.delete();
            vnfd.delete();
            script.delete();
        }
        Assert.assertTrue(pkgs.delete("p1").isSuccess());
        var failed = pkgs.delete("p2");
        Assert.assertFalse(failed.isSuccess());
        Assert.assertEquals(failed.getFault(), "VNF Package p2 is in use.");
        Assert.assertThrows(IllegalArgumentException.class, () -> pkgs.downloadArtifact("p2", "../etc/passwd", script));

        expect("GET", "/vnfpkgm/v1/vnf_packages?all_records=yes", null);
        RecordedRequest r = takeRequest();
        Assert.assertEquals(((Map<?, ?>) body(r).get("userDefinedData")).get("key"), "value");
        expect("GET", "/vnfpkgm/v1/vnf_packages/p2", null);
        expect("PATCH", "/vnfpkgm/v1/vnf_packages/p2", null);
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PUT");
        Assert.assertEquals(path(r), "/vnfpkgm/v1/vnf_packages/p2/package_content");
        Assert.assertTrue(r.getHeader("Content-Type").startsWith("application/zip"), r.getHeader("Content-Type"));
        Assert.assertEquals(r.getBody().readUtf8(), "PK");
        r = takeRequest();
        Assert.assertEquals(path(r), "/vnfpkgm/v1/vnf_packages/p2/package_content/upload_from_uri");
        Assert.assertEquals(body(r).get("addressInformation"), "http://host/pkg.zip");
        r = takeRequest();
        Assert.assertEquals(path(r), "/vnfpkgm/v1/vnf_packages/p2/package_content");
        Assert.assertEquals(r.getHeader("Accept"), "application/zip");
        r = takeRequest();
        Assert.assertEquals(path(r), "/vnfpkgm/v1/vnf_packages/p2/vnfd");
        Assert.assertEquals(r.getHeader("Accept"), "text/plain");
        expect("GET", "/vnfpkgm/v1/vnf_packages/p2/artifacts/Scripts/install.sh", null);
        expect("DELETE", "/vnfpkgm/v1/vnf_packages/p1", null);
        takeRequest();
    }

    public void solRequestsAcceptPlainJson() throws Exception {
        respondWith(200, "[]");
        osv3().tacker().vnfLcm().listVnfInstances(null);
        Assert.assertEquals(takeRequest().getHeader("Accept"), "application/json");
    }

    public void vnfdServedAsJsonStringIsWrittenAsText() throws Exception {
        respondWith(200, "\"tosca_definitions_version: tosca_simple_yaml_1_2\\n\"");
        File vnfd = File.createTempFile("vnfd", ".yaml");
        try {
            Assert.assertTrue(osv3().tacker().vnfPackages().downloadVnfd("p1", null, vnfd).isSuccess());
            Assert.assertEquals(Files.readString(vnfd.toPath()), "tosca_definitions_version: tosca_simple_yaml_1_2\n");
        } finally {
            vnfd.delete();
        }
        takeRequest();
    }

    public void uploadFromFileHasALength() throws Exception {
        respondWith(202);
        File csar = File.createTempFile("pkg", ".zip");
        try {
            Files.writeString(csar.toPath(), "PK-file");
            Assert.assertTrue(osv3().tacker().vnfPackages().uploadContent("p1", Payloads.create(csar)).isSuccess());
        } finally {
            csar.delete();
        }
        RecordedRequest r = takeRequest();
        Assert.assertEquals(r.getHeader("Content-Length"), "7");
        Assert.assertNull(r.getHeader("Transfer-Encoding"));
        Assert.assertEquals(r.getBody().readUtf8(), "PK-file");
    }

    public void vimUpdate() throws Exception {
        respondWith(200, "{\"vim\": {\"id\": \"v1\", \"name\": \"renamed\", \"is_default\": true}}");

        Map<String, Object> vim = osv3().tacker().vim().update("v1", Map.of("name", "renamed", "is_default", true));
        Assert.assertEquals(vim.get("name"), "renamed");

        RecordedRequest r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PUT");
        Assert.assertEquals(path(r), "/v1.0/vims/v1");
        Assert.assertEquals(((Map<?, ?>) body(r).get("vim")).get("name"), "renamed");
    }

    public void vnfFaultsAndPerformance() throws Exception {
        respondWith(200, "[{\"id\": \"a1\", \"perceivedSeverity\": \"CRITICAL\"}]");
        respondWith(200, "{\"id\": \"a1\", \"ackState\": \"UNACKNOWLEDGED\"}");
        respondWith(200, "{\"ackState\": \"ACKNOWLEDGED\"}");
        respondWith(200, "[]");
        respondWith(201, "{\"id\": \"fs1\"}");
        respondWith(200, "{\"id\": \"fs1\"}");
        respondWith(204);
        respondWith(200, "[{\"id\": \"j1\"}]");
        respondWith(201, "{\"id\": \"j1\"}");
        respondWith(200, "{\"id\": \"j1\", \"reports\": []}");
        respondWith(200, "{\"callbackUri\": \"http://nfvo/pm2\"}");
        respondWith(200, "{\"entries\": [{\"objectType\": \"Vnf\"}]}");
        respondWith(204);
        respondWith(200, "[{\"id\": \"t1\"}]");
        respondWith(201, "{\"id\": \"t1\"}");
        respondWith(200, "{\"id\": \"t1\"}");
        respondWith(200, "{\"callbackUri\": \"http://nfvo/th2\"}");
        respondWith(204);

        var fm = osv3().tacker().vnfFaults();
        Assert.assertEquals(items(fm.listAlarms(Map.of("filter", "(eq,perceivedSeverity,CRITICAL)"))).size(), 1);
        Assert.assertEquals(fm.getAlarm("a1").get("ackState"), "UNACKNOWLEDGED");
        Assert.assertEquals(fm.acknowledgeAlarm("a1", "ACKNOWLEDGED").get("ackState"), "ACKNOWLEDGED");
        fm.listSubscriptions(null);
        Assert.assertEquals(fm.createSubscription(Map.of("callbackUri", "http://nfvo/fm")).get("id"), "fs1");
        fm.getSubscription("fs1");
        Assert.assertTrue(fm.deleteSubscription("fs1").isSuccess());
        var pm = osv3().tacker().vnfPerformance();
        Assert.assertEquals(items(pm.listPmJobs(null)).size(), 1);
        Assert.assertEquals(pm.createPmJob(Map.of("objectType", "Vnf", "objectInstanceIds", List.of("i1"), "criteria", Map.of(), "callbackUri", "http://nfvo/pm")).get("id"), "j1");
        pm.getPmJob("j1");
        Assert.assertEquals(pm.updatePmJob("j1", Map.of("callbackUri", "http://nfvo/pm2")).get("callbackUri"), "http://nfvo/pm2");
        Assert.assertEquals(((List<?>) pm.getReport("j1", "r1").get("entries")).size(), 1);
        Assert.assertTrue(pm.deletePmJob("j1").isSuccess());
        pm.listThresholds(null);
        pm.createThreshold(Map.of("objectType", "Vnf", "objectInstanceId", "i1", "criteria", Map.of(), "callbackUri", "http://nfvo/th"));
        pm.getThreshold("t1");
        pm.updateThreshold("t1", Map.of("callbackUri", "http://nfvo/th2"));
        Assert.assertTrue(pm.deleteThreshold("t1").isSuccess());

        expect("GET", "/vnffm/v1/alarms?filter=(eq,perceivedSeverity,CRITICAL)", "1.3.0");
        expect("GET", "/vnffm/v1/alarms/a1", "1.3.0");
        RecordedRequest r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PATCH");
        Assert.assertTrue(r.getHeader("Content-Type").startsWith("application/merge-patch+json"), r.getHeader("Content-Type"));
        Assert.assertEquals(body(r).get("ackState"), "ACKNOWLEDGED");
        expect("GET", "/vnffm/v1/subscriptions", "1.3.0");
        expect("POST", "/vnffm/v1/subscriptions", "1.3.0");
        expect("GET", "/vnffm/v1/subscriptions/fs1", "1.3.0");
        expect("DELETE", "/vnffm/v1/subscriptions/fs1", "1.3.0");
        expect("GET", "/vnfpm/v2/pm_jobs", "2.1.0");
        expect("POST", "/vnfpm/v2/pm_jobs", "2.1.0");
        expect("GET", "/vnfpm/v2/pm_jobs/j1", "2.1.0");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PATCH");
        Assert.assertTrue(r.getHeader("Content-Type").startsWith("application/merge-patch+json"), r.getHeader("Content-Type"));
        expect("GET", "/vnfpm/v2/pm_jobs/j1/reports/r1", "2.1.0");
        expect("DELETE", "/vnfpm/v2/pm_jobs/j1", "2.1.0");
        expect("GET", "/vnfpm/v2/thresholds", "2.1.0");
        expect("POST", "/vnfpm/v2/thresholds", "2.1.0");
        expect("GET", "/vnfpm/v2/thresholds/t1", "2.1.0");
        expect("PATCH", "/vnfpm/v2/thresholds/t1", "2.1.0");
        expect("DELETE", "/vnfpm/v2/thresholds/t1", "2.1.0");
    }
}
