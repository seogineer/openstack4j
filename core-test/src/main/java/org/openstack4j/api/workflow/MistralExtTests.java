package org.openstack4j.api.workflow;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Workflow/Ext")
public class MistralExtTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.WORKFLOW;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    private static String json(RecordedRequest r) throws Exception {
        return new ObjectMapper().readTree(r.getBody().readUtf8()).toString();
    }

    public void validationAndCodeSources() throws Exception {
        respondWith(200, "{\"valid\": true, \"error\": null}");
        respondWith(200, "{\"valid\": false, \"error\": \"Invalid DSL\"}");
        respondWith(java.util.Map.of("Content-Type", "text/plain"), 201, "{\"id\": \"cs1\", \"name\": \"my_actions\", \"scope\": \"private\", \"version\": 1}");
        respondWith(java.util.Map.of("Content-Type", "text/plain"), 200, "{\"id\": \"cs1\", \"name\": \"my_actions\", \"version\": 2}");
        respondWith(200, "{\"code_sources\": [{\"id\": \"cs1\", \"name\": \"my_actions\"}]}");
        respondWith(204);

        var ext = osv3().workflow().extensions();
        Map<String, Object> valid = ext.validateWorkflow("version: '2.0'\nwf:\n  tasks: {}\n");
        Map<String, Object> invalid = ext.validateWorkbook("bad");
        Map<String, Object> created = ext.createCodeSource("my_actions", "class A: pass\n", null);
        Map<String, Object> updated = ext.updateCodeSource("my_actions", "class B: pass\n", "private");
        List<Map<String, Object>> sources = ext.listCodeSources(Map.of("scope", "private"));
        Assert.assertTrue(ext.deleteCodeSource("cs1").isSuccess());

        RecordedRequest validate = takeRequest();
        Assert.assertTrue(path(validate).endsWith("/workflows/validate"));
        Assert.assertTrue(validate.getHeader("Content-Type").startsWith("text/plain"), validate.getHeader("Content-Type"));
        Assert.assertEquals(validate.getBody().readUtf8(), "version: '2.0'\nwf:\n  tasks: {}\n");
        Assert.assertTrue(path(takeRequest()).endsWith("/workbooks/validate"));
        RecordedRequest create = takeRequest();
        Assert.assertTrue(path(create).endsWith("/code_sources?name=my_actions"), path(create));
        Assert.assertEquals(create.getBody().readUtf8(), "class A: pass\n");
        String update = path(takeRequest());
        Assert.assertTrue(update.contains("/code_sources?") && update.contains("identifier=my_actions") && update.contains("scope=private"), update);
        Assert.assertTrue(path(takeRequest()).endsWith("/code_sources?scope=private"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(valid.get("valid"), Boolean.TRUE);
        Assert.assertEquals(invalid.get("error"), "Invalid DSL");
        Assert.assertEquals(created.get("id"), "cs1");
        Assert.assertEquals(updated.get("version"), 2);
        Assert.assertEquals(sources.get(0).get("name"), "my_actions");
    }

    public void dynamicActionsTriggersExecutionsMembers() throws Exception {
        respondWith(201, "{\"id\": \"da1\", \"name\": \"my_action\", \"class_name\": \"A\"}");
        respondWith(200, "{\"id\": \"da1\", \"name\": \"my_action\", \"class_name\": \"B\"}");
        respondWith(201, "{\"id\": \"et1\", \"name\": \"on_create\", \"exchange\": \"nova\", \"topic\": \"notifications\", \"event\": \"compute.instance.create.end\"}");
        respondWith(200, "{\"id\": \"et1\", \"name\": \"on_create\", \"workflow_input\": \"{}\"}");
        respondWith(200, "{\"executions\": [{\"id\": \"sub1\", \"state\": \"SUCCESS\"}]}");
        respondWith(200, "{\"root_workflow_execution\": {\"id\": \"ex1\"}, \"statistics\": {\"total_tasks_count\": 3}}");
        respondWith(200, "{\"executions\": [{\"id\": \"sub2\"}]}");
        respondWith(201, "{\"resource_id\": \"wf1\", \"member_id\": \"p2\", \"status\": \"pending\"}");
        respondWith(200, "{\"resource_id\": \"wf1\", \"member_id\": \"p2\", \"status\": \"accepted\"}");
        respondWith(200, "{\"members\": [{\"member_id\": \"p2\", \"status\": \"accepted\"}]}");
        respondWith(204);

        var ext = osv3().workflow().extensions();
        ext.createDynamicAction(Map.of("name", "my_action", "class_name", "A", "code_source_id", "cs1"));
        ext.updateDynamicAction(Map.of("name", "my_action", "class_name", "B"));
        ext.createEventTrigger(Map.of("name", "on_create", "workflow_id", "wf1", "exchange", "nova", "topic", "notifications", "event", "compute.instance.create.end"));
        ext.updateEventTrigger("et1", Map.of("workflow_input", Map.of()));
        List<Map<String, Object>> subs = ext.listSubExecutions("ex1", Map.of("errors_only", "true"));
        Map<String, Object> report = ext.executionReport("ex1", null);
        ext.listTaskExecutions("t1");
        Map<String, Object> member = ext.addMember("wf1", "p2");
        ext.updateMember("wf1", "p2", "accepted");
        List<Map<String, Object>> members = ext.listMembers("wf1");
        Assert.assertTrue(ext.removeMember("wf1", "p2").isSuccess());

        RecordedRequest dynamic = takeRequest();
        Assert.assertTrue(path(dynamic).endsWith("/dynamic_actions"));
        Assert.assertTrue(json(dynamic).contains("\"code_source_id\":\"cs1\""));
        Assert.assertEquals(takeRequest().getMethod(), "PUT");
        Assert.assertTrue(path(takeRequest()).endsWith("/event_triggers"));
        RecordedRequest trigger = takeRequest();
        Assert.assertTrue(path(trigger).endsWith("/event_triggers/et1"));
        Assert.assertEquals(json(trigger), "{\"workflow_input\":{}}");
        Assert.assertTrue(path(takeRequest()).endsWith("/executions/ex1/executions?errors_only=true"));
        Assert.assertTrue(path(takeRequest()).endsWith("/executions/ex1/report"));
        Assert.assertTrue(path(takeRequest()).endsWith("/tasks/t1/executions"));
        RecordedRequest add = takeRequest();
        Assert.assertTrue(path(add).endsWith("/workflows/wf1/members"));
        Assert.assertEquals(json(add), "{\"member_id\":\"p2\"}");
        Assert.assertEquals(json(takeRequest()), "{\"status\":\"accepted\"}");
        Assert.assertTrue(path(takeRequest()).endsWith("/workflows/wf1/members"));
        Assert.assertTrue(path(takeRequest()).endsWith("/workflows/wf1/members/p2"));
        Assert.assertEquals(subs.get(0).get("state"), "SUCCESS");
        Assert.assertEquals(((Map<?, ?>) report.get("statistics")).get("total_tasks_count"), 3);
        Assert.assertEquals(member.get("status"), "pending");
        Assert.assertEquals(members.get(0).get("status"), "accepted");
    }
}
