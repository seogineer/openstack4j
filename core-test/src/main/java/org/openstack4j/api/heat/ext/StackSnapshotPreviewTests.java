package org.openstack4j.api.heat.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.heat.ext.StackSnapshot;
import org.openstack4j.openstack.heat.domain.HeatStackCreate;
import org.openstack4j.openstack.heat.domain.HeatStackUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Heat/Ext/SnapshotsPreview")
public class StackSnapshotPreviewTests extends AbstractHeatExtTest {

    private static final String S = "/stacks/s1/da778f26";

    public void snapshots() throws Exception {
        respondWith(200, "{\"id\": \"13c3a4b5\", \"name\": \"snap1\", \"action\": \"CREATE\", \"status\": \"IN_PROGRESS\", \"status_reason\": null, \"data\": null, \"creation_time\": \"2015-09-01T20:57:55Z\"}");
        respondWith(200, "{\"snapshots\": [{\"id\": \"13c3a4b5\", \"name\": \"snap1\", \"status\": \"COMPLETE\", \"status_reason\": null, \"creation_time\": \"2015-08-04T20:57:55Z\", \"data\": null}]}");
        respondWith(200, "{\"snapshot\": {\"id\": \"13c3a4b5\", \"action\": \"CREATE\", \"name\": \"snap1\", \"status\": \"COMPLETE\", \"status_reason\": \"Stack SNAPSHOT completed successfully\","
                + " \"creation_time\": \"2015-08-04T20:57:55Z\", \"data\": {\"status\": \"COMPLETE\", \"name\": \"s1\"}}}");
        respondWith(202, "{\"code\": \"202 Accepted\", \"message\": \"The request is accepted for processing.\", \"title\": \"Accepted\"}");
        respondWith(204);
        respondWith(200, "{\"id\": \"x\", \"status\": \"IN_PROGRESS\"}");

        var stacks = osv3().heat().stacks();
        StackSnapshot created = stacks.snapshot("s1", "da778f26", "snap1");
        List<? extends StackSnapshot> all = stacks.snapshots("s1", "da778f26");
        StackSnapshot one = stacks.getSnapshot("s1", "da778f26", "13c3a4b5");
        Assert.assertTrue(stacks.restoreSnapshot("s1", "da778f26", "13c3a4b5").isSuccess());
        Assert.assertTrue(stacks.deleteSnapshot("s1", "da778f26", "13c3a4b5").isSuccess());
        stacks.snapshot("s1", "da778f26", null);

        RecordedRequest create = expect("POST", S + "/snapshots");
        Assert.assertEquals(body(create).toString(), "{\"name\":\"snap1\"}");
        expect("GET", S + "/snapshots");
        expect("GET", S + "/snapshots/13c3a4b5");
        expect("POST", S + "/snapshots/13c3a4b5/restore");
        expect("DELETE", S + "/snapshots/13c3a4b5");
        Assert.assertEquals(body(expect("POST", S + "/snapshots")).toString(), "{}");
        Assert.assertEquals(created.getStatus(), "IN_PROGRESS");
        Assert.assertEquals(all.get(0).getName(), "snap1");
        Assert.assertEquals(one.getData().get("name"), "s1");
    }

    public void previews() throws Exception {
        respondWith(200, "{\"stack\": {\"id\": \"None\", \"stack_name\": \"s1\", \"resources\": [{\"resource_name\": \"random\", \"resource_type\": \"OS::Heat::RandomString\"}]}}");
        respondWith(200, "{\"resource_changes\": {\"unchanged\": [], \"updated\": [{\"resource_name\": \"random\"}], \"replaced\": [], \"added\": [], \"deleted\": []}}");
        respondWith(200, "{\"resource_changes\": {\"unchanged\": [{\"resource_name\": \"random\"}], \"updated\": [], \"replaced\": [], \"added\": [], \"deleted\": []}}");

        var stacks = osv3().heat().stacks();
        Map<String, Object> preview = stacks.preview(HeatStackCreate.build().name("s1").template("heat_template_version: 2016-10-14\nresources: {}").build());
        Map<String, List<Map<String, Object>>> changes = stacks.previewUpdate("s1", "da778f26", HeatStackUpdate.builder().parameters(Map.of("length", "12")).build());
        Map<String, List<Map<String, Object>>> patchChanges = stacks.previewPatchUpdate("s1", "da778f26", HeatStackUpdate.builder().parameters(Map.of("length", "12")).build());

        RecordedRequest create = expect("POST", "/stacks/preview");
        Assert.assertEquals(body(create).get("stack_name").asText(), "s1");
        expect("PUT", S + "/preview");
        expect("PATCH", S + "/preview");
        Assert.assertEquals(((Map<?, ?>) ((List<?>) preview.get("resources")).get(0)).get("resource_name"), "random");
        Assert.assertEquals(changes.get("updated").get(0).get("resource_name"), "random");
        Assert.assertEquals(patchChanges.get("unchanged").size(), 1);
    }
}
