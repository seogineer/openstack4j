package org.openstack4j.api.heat.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.heat.ext.StackOutput;
import org.openstack4j.openstack.heat.domain.HeatStackUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Heat/Ext/Stacks")
public class StackExtensionTests extends AbstractHeatExtTest {

    private static final String S = "/stacks/s1/da778f26";
    private static final String STACK = "{\"stack\": {\"id\": \"da778f26\", \"stack_name\": \"s1\", \"stack_status\": \"CREATE_COMPLETE\", \"links\": []}}";

    public void deleteByIdentityLooksUpThenDeletes() throws Exception {
        respondWith(200, STACK);
        respondWith(204);
        Assert.assertTrue(osv3().heat().stacks().delete("s1").isSuccess());
        expect("GET", "/stacks/s1");
        expect("DELETE", S);
    }

    public void eventsByNameFollowRedirect() throws Exception {
        respondWith(java.util.Collections.singletonMap("Location", "/stacks/s1/da778f26/events"), 302, "");
        respondWith(200, "{\"events\": [{\"id\": \"e1\", \"resource_name\": \"random\", \"resource_status\": \"CREATE_COMPLETE\", \"event_time\": \"2015-09-01T20:57:55Z\", \"links\": []}]}");
        Assert.assertEquals(osv3().heat().events().list("s1").size(), 1);
        expect("GET", "/stacks/s1/events");
        expect("GET", S + "/events");
    }

    public void environmentExportFilesOutputs() throws Exception {
        respondWith(200, "{\"encrypted_param_names\": [], \"event_sinks\": [], \"parameter_defaults\": {}, \"parameters\": {\"length\": 8}, \"resource_registry\": {\"resources\": {}}}");
        respondWith(200, "{\"action\": \"SUSPEND\", \"id\": \"da778f26\", \"name\": \"s1\", \"resources\": {\"random\": {\"action\": \"SUSPEND\"}}}");
        respondWith(200, "{\"file:///home/username/hello.sh\": \"#!/bin/sh\\necho hello\\n\"}");
        respondWith(200, "{\"outputs\": [{\"output_key\": \"value\", \"description\": \"the random string\"}]}");
        respondWith(200, "{\"output\": {\"output_key\": \"value\", \"output_value\": \"aB3dE5\", \"description\": \"the random string\"}}");

        var stacks = osv3().heat().stacks();
        Map<String, Object> environment = stacks.environment("s1", "da778f26");
        Map<String, Object> export = stacks.export("s1", "da778f26");
        Map<String, String> files = stacks.files("s1", "da778f26");
        List<? extends StackOutput> outputs = stacks.outputs("s1", "da778f26");
        StackOutput output = stacks.output("s1", "da778f26", "value");

        expect("GET", S + "/environment");
        expect("GET", S + "/export");
        expect("GET", S + "/files");
        expect("GET", S + "/outputs");
        expect("GET", S + "/outputs/value");
        Assert.assertEquals(((Map<?, ?>) environment.get("parameters")).get("length"), 8);
        Assert.assertEquals(export.get("action"), "SUSPEND");
        Assert.assertTrue(files.get("file:///home/username/hello.sh").startsWith("#!/bin/sh"));
        Assert.assertEquals(outputs.get(0).getOutputKey(), "value");
        Assert.assertNull(outputs.get(0).getOutputValue());
        Assert.assertEquals(output.getOutputValue(), "aB3dE5");
    }

    public void patchUpdate() throws Exception {
        respondWith(202);
        Assert.assertTrue(osv3().heat().stacks().patchUpdate("s1", "da778f26", HeatStackUpdate.builder().parameters(Map.of("length", "12")).build()).isSuccess());
        RecordedRequest patch = expect("PATCH", S);
        Assert.assertEquals(body(patch).get("parameters").get("length").asText(), "12");
    }

    public void actionsSendExplicitNull() throws Exception {
        for (int i = 0; i < 5; i++)
            respondWith(202);
        var stacks = osv3().heat().stacks();
        stacks.suspend("s1", "da778f26");
        stacks.resume("s1", "da778f26");
        stacks.check("s1", "da778f26");
        stacks.cancelUpdate("s1", "da778f26");
        Assert.assertTrue(stacks.cancelWithoutRollback("s1", "da778f26").isSuccess());
        for (String action : new String[] {"suspend", "resume", "check", "cancel_update", "cancel_without_rollback"}) {
            RecordedRequest request = expect("POST", S + "/actions");
            Assert.assertEquals(body(request).toString(), "{\"" + action + "\":null}");
        }
    }
}
