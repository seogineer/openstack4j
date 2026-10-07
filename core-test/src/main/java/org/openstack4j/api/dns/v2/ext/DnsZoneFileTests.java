package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.dns.v2.ext.ZoneExport;
import org.openstack4j.model.dns.v2.ext.ZoneImport;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "DNS/ZoneFiles")
public class DnsZoneFileTests extends AbstractDnsExtTest {

    private static final String EXPORT = "{\"id\": \"8ec17fe1-d1f9-41b4-aa98-4eeb4c27b720\", \"zone_id\": \"6625198b-d67d-47dc-8d29-f90bd60f3ac4\", \"status\": \"PENDING\","
            + " \"message\": null, \"location\": null, \"project_id\": \"1de6e2fd\", \"version\": 1, \"created_at\": \"2015-08-27T20:57:03.000000\", \"links\": {}}";
    private static final String IMPORT = "{\"id\": \"074e805e-fe87-4cbb-b10b-21a06e215d41\", \"zone_id\": null, \"status\": \"PENDING\", \"message\": null,"
            + " \"project_id\": \"1de6e2fd\", \"version\": 1, \"links\": {}}";
    private static final String ZONE_FILE = "$ORIGIN example.com.\n$TTL 3600\nexample.com. IN SOA ns.example.com. nsadmin.example.com. 1 3600 600 86400 3600\n";

    public void exports() throws Exception {
        respondWith(202, EXPORT);
        respondWith(200, "{\"exports\": [" + EXPORT + "], \"links\": {}}");
        respondWith(200, EXPORT.replace("PENDING", "COMPLETE"));
        respondWith(java.util.Map.of("Content-Type", "text/dns"), 200, ZONE_FILE);
        respondWith(204);

        ZoneExport export = osv3().dns().zoneFiles().export("6625198b-d67d-47dc-8d29-f90bd60f3ac4");
        List<? extends ZoneExport> all = osv3().dns().zoneFiles().listExports(Map.of("status", "COMPLETE"));
        ZoneExport done = osv3().dns().zoneFiles().getExport(export.getId());
        String content = osv3().dns().zoneFiles().getExportContent(export.getId());
        Assert.assertTrue(osv3().dns().zoneFiles().deleteExport(export.getId()).isSuccess());

        Assert.assertEquals(body(expect("POST", "/v2/zones/6625198b-d67d-47dc-8d29-f90bd60f3ac4/tasks/export")).toString(), "{}");
        expect("GET", "/v2/zones/tasks/exports?status=COMPLETE");
        expect("GET", "/v2/zones/tasks/exports/8ec17fe1-d1f9-41b4-aa98-4eeb4c27b720");
        RecordedRequest contentRequest = expect("GET", "/v2/zones/tasks/exports/8ec17fe1-d1f9-41b4-aa98-4eeb4c27b720/export");
        Assert.assertEquals(contentRequest.getHeader("Accept"), "text/dns");
        expect("DELETE", "/v2/zones/tasks/exports/8ec17fe1-d1f9-41b4-aa98-4eeb4c27b720");
        Assert.assertEquals(export.getStatus(), "PENDING");
        Assert.assertEquals(all.get(0).getZoneId(), "6625198b-d67d-47dc-8d29-f90bd60f3ac4");
        Assert.assertEquals(done.getStatus(), "COMPLETE");
        Assert.assertEquals(content, ZONE_FILE);
    }

    public void imports() throws Exception {
        respondWith(202, IMPORT);
        respondWith(200, "{\"imports\": [" + IMPORT + "], \"links\": {}}");
        respondWith(200, IMPORT);
        respondWith(204);

        ZoneImport created = osv3().dns().zoneFiles().importZone(ZONE_FILE);
        osv3().dns().zoneFiles().listImports();
        osv3().dns().zoneFiles().getImport(created.getId());
        Assert.assertTrue(osv3().dns().zoneFiles().deleteImport(created.getId()).isSuccess());

        RecordedRequest post = expect("POST", "/v2/zones/tasks/imports");
        Assert.assertTrue(post.getHeader("Content-Type").startsWith("text/dns"), post.getHeader("Content-Type"));
        Assert.assertEquals(post.getBody().readUtf8(), ZONE_FILE);
        expect("GET", "/v2/zones/tasks/imports");
        expect("GET", "/v2/zones/tasks/imports/074e805e-fe87-4cbb-b10b-21a06e215d41");
        expect("DELETE", "/v2/zones/tasks/imports/074e805e-fe87-4cbb-b10b-21a06e215d41");
        Assert.assertEquals(created.getStatus(), "PENDING");
    }

    public void missingExportContentRaises() throws Exception {
        respondWith(404, "{\"code\": 404, \"type\": \"zone_export_not_found\", \"message\": \"Could not find ZoneExport\"}");
        try {
            osv3().dns().zoneFiles().getExportContent("missing");
            Assert.fail("expected a ResponseException");
        } catch (org.openstack4j.api.exceptions.ResponseException e) {
            Assert.assertEquals(e.getStatus(), 404);
        } finally {
            takeRequest();
        }
    }
}
