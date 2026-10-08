package org.openstack4j.api.optimization;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Optimization")
public class OptimizationTests extends AbstractTest {

    private static final String VERSION = "OpenStack-API-Version";

    @Override
    protected Service service() {
        return Service.OPTIMIZATION;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    private static String json(RecordedRequest r) throws Exception {
        return new ObjectMapper().readTree(r.getBody().readUtf8()).toString();
    }

    private static List<?> items(Map<String, Object> page, String key) {
        return (List<?>) page.get(key);
    }

    public void auditTemplatesAndAudits() throws Exception {
        respondWith(200, "{\"versions\": [{\"id\": \"v1\", \"max_version\": \"1.5\"}]}");
        respondWith(200, "{\"id\": \"v1\", \"media_types\": []}");
        respondWith(200, "{\"audit_templates\": [{\"uuid\": \"at1\", \"name\": \"t1\"}], \"next\": \"http://x/v1/audit_templates?marker=at1\"}");
        respondWith(200, "{\"audit_templates\": [{\"uuid\": \"at1\", \"goal_name\": \"dummy\"}]}");
        respondWith(200, "{\"uuid\": \"at1\", \"name\": \"t1\"}");
        respondWith(201, "{\"uuid\": \"at2\", \"name\": \"t2\"}");
        respondWith(200, "{\"uuid\": \"at2\", \"name\": \"t3\"}");
        respondWith(204);
        respondWith(200, "{\"audits\": []}");
        respondWith(200, "{\"audits\": [{\"uuid\": \"a1\"}]}");
        respondWith(200, "{\"uuid\": \"a1\", \"state\": \"SUCCEEDED\"}");
        respondWith(201, "{\"uuid\": \"a2\", \"state\": \"PENDING\"}");
        respondWith(201, "{\"uuid\": \"a3\"}");
        respondWith(201, "{\"uuid\": \"a4\"}");
        respondWith(200, "{\"uuid\": \"a2\", \"state\": \"CANCELLED\"}");
        respondWith(200, "{\"uuid\": \"a3\"}");
        respondWith(201, "{\"uuid\": \"at4\"}");
        respondWith(204);
        respondWith(404, "{\"error_message\": \"{\\\"faultstring\\\": \\\"Audit x could not be found.\\\"}\"}");

        var w = osv3().optimization();
        Assert.assertEquals(items(w.versions(), "versions").size(), 1);
        Assert.assertEquals(w.version().get("id"), "v1");
        Map<String, Object> templates = w.listAuditTemplates(Map.of("goal", "dummy"));
        Assert.assertEquals(templates.get("next"), "http://x/v1/audit_templates?marker=at1");
        Assert.assertEquals(items(w.listAuditTemplatesDetail(null), "audit_templates").size(), 1);
        Assert.assertEquals(w.getAuditTemplate("t1").get("uuid"), "at1");
        Assert.assertEquals(w.createAuditTemplate(Map.of("name", "t2", "goal", "dummy")).get("uuid"), "at2");
        Assert.assertEquals(w.updateAuditTemplate("at2", List.of(Map.of("op", "replace", "path", "/name", "value", "t3"))).get("name"), "t3");
        Assert.assertTrue(w.deleteAuditTemplate("at2").isSuccess());
        Assert.assertTrue(items(w.listAudits(null), "audits").isEmpty());
        Assert.assertEquals(items(w.listAuditsDetail(Map.of("audit_template", "at1")), "audits").size(), 1);
        Assert.assertEquals(w.getAudit("a1").get("state"), "SUCCEEDED");
        Assert.assertEquals(w.createAudit(Map.of("audit_template_uuid", "at1", "audit_type", "ONESHOT")).get("uuid"), "a2");
        w.createAudit(Map.of("goal", "dummy", "audit_type", "CONTINUOUS", "interval", "3600", "start_time", "2026-10-09T00:00:00"));
        w.createAudit(Map.of("goal", "dummy", "force", true));
        Assert.assertEquals(w.updateAudit("a2", List.of(Map.of("op", "replace", "path", "/state", "value", "CANCELLED"))).get("state"), "CANCELLED");
        w.updateAudit("a3", List.of(Map.of("op", "replace", "path", "/end_time", "value", "2026-10-10T00:00:00")));
        w.createAuditTemplate(Map.of("name", "t4", "goal", "dummy", "strategy", "dummy", "default_parameters", Map.of("para1", 2)));
        Assert.assertTrue(w.deleteAudit("a2").isSuccess());
        Assert.assertNull(w.getAudit("x"));

        Assert.assertEquals(path(takeRequest()), "/");
        Assert.assertEquals(path(takeRequest()), "/v1");
        Assert.assertEquals(path(takeRequest()), "/v1/audit_templates?goal=dummy");
        Assert.assertEquals(path(takeRequest()), "/v1/audit_templates/detail");
        Assert.assertEquals(path(takeRequest()), "/v1/audit_templates/t1");
        RecordedRequest r = takeRequest();
        Assert.assertEquals(r.getMethod(), "POST");
        Assert.assertNull(r.getHeader(VERSION));
        Map<?, ?> template = new ObjectMapper().readValue(r.getBody().readUtf8(), Map.class);
        Assert.assertEquals(template.get("goal"), "dummy");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PATCH");
        Assert.assertEquals(path(r), "/v1/audit_templates/at2");
        Assert.assertEquals(new ObjectMapper().readTree(r.getBody().readUtf8()).get(0).get("path").asText(), "/name");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(path(takeRequest()), "/v1/audits");
        Assert.assertEquals(path(takeRequest()), "/v1/audits/detail?audit_template=at1");
        Assert.assertEquals(path(takeRequest()), "/v1/audits/a1");
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/audits");
        Assert.assertNull(r.getHeader(VERSION));
        Assert.assertEquals(takeRequest().getHeader(VERSION), "infra-optim 1.1");
        Assert.assertEquals(takeRequest().getHeader(VERSION), "infra-optim 1.2");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PATCH");
        Assert.assertEquals(path(r), "/v1/audits/a2");
        Assert.assertNull(r.getHeader(VERSION));
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/audits/a3");
        Assert.assertEquals(r.getHeader(VERSION), "infra-optim 1.1");
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/audit_templates");
        Assert.assertEquals(r.getHeader(VERSION), "infra-optim 1.7");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "DELETE");
        Assert.assertEquals(path(r), "/v1/audits/a2");
        takeRequest();
    }

    public void actionPlansActionsAndCatalogs() throws Exception {
        respondWith(200, "{\"action_plans\": [{\"uuid\": \"p1\", \"state\": \"RECOMMENDED\"}]}");
        respondWith(200, "{\"action_plans\": [{\"uuid\": \"p1\", \"efficacy_indicators\": []}]}");
        respondWith(200, "{\"uuid\": \"p1\", \"state\": \"RECOMMENDED\"}");
        respondWith(200, "{\"uuid\": \"p1\", \"state\": \"PENDING\"}");
        respondWith(200, "{\"uuid\": \"p1\", \"state\": \"PENDING\"}");
        respondWith(204);
        respondWith(200, "{\"actions\": [{\"uuid\": \"x1\", \"action_type\": \"migrate\"}]}");
        respondWith(200, "{\"actions\": [{\"uuid\": \"x1\", \"input_parameters\": {}}]}");
        respondWith(200, "{\"uuid\": \"x1\", \"state\": \"PENDING\"}");
        respondWith(200, "{\"uuid\": \"x1\", \"state\": \"SKIPPED\"}");
        respondWith(200, "{\"goals\": [{\"uuid\": \"g1\", \"name\": \"dummy\"}]}");
        respondWith(200, "{\"goals\": [{\"uuid\": \"g1\", \"efficacy_specification\": []}]}");
        respondWith(200, "{\"uuid\": \"g1\", \"name\": \"dummy\"}");
        respondWith(200, "{\"strategies\": [{\"uuid\": \"s1\", \"name\": \"dummy\"}]}");
        respondWith(200, "{\"strategies\": [{\"uuid\": \"s1\", \"parameters_spec\": {}}]}");
        respondWith(200, "{\"uuid\": \"s1\", \"name\": \"dummy\"}");
        respondWith(200, "[{\"type\": \"Datasource\", \"state\": \"gnocchi: available\", \"mandatory\": true, \"comment\": \"\"}]");
        respondWith(200, "{\"scoring_engines\": [{\"uuid\": \"e1\", \"name\": \"dummy_scorer\"}]}");
        respondWith(200, "{\"scoring_engines\": [{\"uuid\": \"e1\", \"metainfo\": \"\"}]}");
        respondWith(200, "{\"uuid\": \"e1\", \"name\": \"dummy_scorer\"}");
        respondWith(200, "{\"services\": [{\"id\": 1, \"name\": \"watcher-applier\", \"status\": \"ACTIVE\"}]}");
        respondWith(200, "{\"services\": [{\"id\": 1, \"host\": \"controller\"}]}");
        respondWith(200, "{\"id\": 1, \"name\": \"watcher-applier\"}");
        respondWith(200, "{\"context\": [{\"server_uuid\": \"vm1\", \"node_hostname\": \"compute-1\"}]}");
        respondWith(202);

        var w = osv3().optimization();
        Assert.assertEquals(items(w.listActionPlans(Map.of("audit_uuid", "a1")), "action_plans").size(), 1);
        w.listActionPlansDetail(null);
        Assert.assertEquals(w.getActionPlan("p1").get("state"), "RECOMMENDED");
        Assert.assertEquals(w.updateActionPlan("p1", List.of(Map.of("op", "replace", "path", "/state", "value", "PENDING"))).get("state"), "PENDING");
        Assert.assertEquals(w.startActionPlan("p1").get("state"), "PENDING");
        Assert.assertTrue(w.deleteActionPlan("p1").isSuccess());
        Assert.assertEquals(items(w.listActions(Map.of("action_plan_uuid", "p1")), "actions").size(), 1);
        w.listActionsDetail(null);
        Assert.assertEquals(w.getAction("x1").get("state"), "PENDING");
        Assert.assertEquals(w.updateAction("x1", List.of(Map.of("op", "replace", "path", "/state", "value", "SKIPPED"))).get("state"), "SKIPPED");
        w.listGoals(null);
        w.listGoalsDetail(null);
        Assert.assertEquals(w.getGoal("dummy").get("uuid"), "g1");
        w.listStrategies(Map.of("goal", "dummy"));
        w.listStrategiesDetail(null);
        Assert.assertEquals(w.getStrategy("dummy").get("uuid"), "s1");
        Assert.assertEquals(w.getStrategyState("dummy").get(0).get("type"), "Datasource");
        w.listScoringEngines(null);
        w.listScoringEnginesDetail(null);
        Assert.assertEquals(w.getScoringEngine("dummy_scorer").get("uuid"), "e1");
        w.listServices(null);
        w.listServicesDetail(null);
        Assert.assertEquals(w.getService("watcher-applier").get("id"), 1);
        Assert.assertEquals(((Map<?, ?>) ((List<?>) w.getDataModel("compute", "a1").get("context")).get(0)).get("server_uuid"), "vm1");
        Assert.assertTrue(w.triggerWebhook("a1").isSuccess());

        Assert.assertEquals(path(takeRequest()), "/v1/action_plans?audit_uuid=a1");
        Assert.assertEquals(path(takeRequest()), "/v1/action_plans/detail");
        Assert.assertEquals(path(takeRequest()), "/v1/action_plans/p1");
        Assert.assertEquals(takeRequest().getMethod(), "PATCH");
        RecordedRequest r = takeRequest();
        Assert.assertEquals(r.getMethod(), "POST");
        Assert.assertEquals(path(r), "/v1/action_plans/p1/start");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(path(takeRequest()), "/v1/actions?action_plan_uuid=p1");
        Assert.assertEquals(path(takeRequest()), "/v1/actions/detail");
        Assert.assertEquals(path(takeRequest()), "/v1/actions/x1");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PATCH");
        Assert.assertEquals(path(r), "/v1/actions/x1");
        Assert.assertEquals(r.getHeader(VERSION), "infra-optim 1.5");
        Assert.assertEquals(new ObjectMapper().readTree(r.getBody().readUtf8()).get(0).get("value").asText(), "SKIPPED");
        Assert.assertEquals(path(takeRequest()), "/v1/goals");
        Assert.assertEquals(path(takeRequest()), "/v1/goals/detail");
        Assert.assertEquals(path(takeRequest()), "/v1/goals/dummy");
        Assert.assertEquals(path(takeRequest()), "/v1/strategies?goal=dummy");
        Assert.assertEquals(path(takeRequest()), "/v1/strategies/detail");
        Assert.assertEquals(path(takeRequest()), "/v1/strategies/dummy");
        Assert.assertEquals(path(takeRequest()), "/v1/strategies/dummy/state");
        Assert.assertEquals(path(takeRequest()), "/v1/scoring_engines");
        Assert.assertEquals(path(takeRequest()), "/v1/scoring_engines/detail");
        Assert.assertEquals(path(takeRequest()), "/v1/scoring_engines/dummy_scorer");
        Assert.assertEquals(path(takeRequest()), "/v1/services");
        Assert.assertEquals(path(takeRequest()), "/v1/services/detail");
        Assert.assertEquals(path(takeRequest()), "/v1/services/watcher-applier");
        r = takeRequest();
        Assert.assertTrue(path(r).startsWith("/v1/data_model?"), path(r));
        Assert.assertTrue(path(r).contains("data_model_type=compute") && path(r).contains("audit_uuid=a1"), path(r));
        Assert.assertEquals(r.getHeader(VERSION), "infra-optim 1.3");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "POST");
        Assert.assertEquals(path(r), "/v1/webhooks/a1");
        Assert.assertEquals(r.getHeader(VERSION), "infra-optim 1.4");
    }

    public void listRaisesOn404AndDeleteFailureIsReported() throws Exception {
        respondWith(404, "{\"error_message\": \"not found\"}");
        respondWith(400, "{\"error_message\": \"{\\\"faultstring\\\": \\\"Audit template is in use\\\"}\"}");

        var w = osv3().optimization();
        Assert.assertThrows(RuntimeException.class, () -> w.listAudits(null));
        Assert.assertFalse(w.deleteAuditTemplate("at1").isSuccess());
        takeRequest();
        takeRequest();
    }
}
