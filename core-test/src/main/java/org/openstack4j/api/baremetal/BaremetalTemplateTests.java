package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.baremetal.Allocation;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.DeployTemplate;
import org.openstack4j.model.baremetal.InspectionRule;
import org.openstack4j.model.baremetal.Runbook;
import org.openstack4j.model.baremetal.options.AllocationCreate;
import org.openstack4j.model.baremetal.options.DeployTemplateCreate;
import org.openstack4j.model.baremetal.options.InspectionRuleCreate;
import org.openstack4j.model.baremetal.options.RunbookCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/Templates")
public class BaremetalTemplateTests extends AbstractBaremetalTest {

    private static final String ALLOCATION = "{\"uuid\": \"5344a3e2-978a-444e-990a-cbf47c62ef88\", \"name\": \"allocation-1\", \"node_uuid\": null, \"state\": \"allocating\","
            + " \"last_error\": null, \"resource_class\": \"bm-large\", \"candidate_nodes\": [], \"traits\": [\"CUSTOM_GOLD\"], \"extra\": {}, \"owner\": null, \"links\": []}";
    private static final String TEMPLATE = "{\"uuid\": \"bbb45f41-d4bc-4307-8d1d-32f95ce1e920\", \"name\": \"CUSTOM_HYPERTHREADING_ON\","
            + " \"steps\": [{\"interface\": \"bios\", \"step\": \"apply_configuration\", \"args\": {}, \"priority\": 150}], \"extra\": {}, \"links\": []}";
    private static final String RUNBOOK = "{\"uuid\": \"6a8d1e58-6b1d-4d2c-9e7e-4b9d0b9c0a11\", \"name\": \"CUSTOM_AWESOME\", \"public\": false, \"owner\": \"p1\","
            + " \"steps\": [{\"interface\": \"bmc\", \"step\": \"reset_idrac\", \"args\": {}, \"order\": 0}], \"extra\": {}}";
    private static final String RULE = "{\"uuid\": \"783bf33a-a8e3-4d6f-a8a0-0e5c1c7a9d11\", \"description\": \"set name\", \"phase\": \"main\", \"priority\": 0, \"sensitive\": false,"
            + " \"conditions\": [{\"op\": \"eq\", \"args\": [\"{inventory[cpu][architecture]}\", \"x86_64\"]}], \"actions\": [{\"op\": \"set-attribute\", \"args\": [\"/driver\", \"redfish\"]}]}";

    public void allocations() throws Exception {
        respondWith(201, ALLOCATION);
        respondWith(200, "{\"allocations\": [" + ALLOCATION + "]}");
        respondWith(200, ALLOCATION);
        respondWith(200, ALLOCATION);
        respondWith(200, ALLOCATION);
        respondWith(204);
        respondWith(204);

        var allocations = osv3().baremetal().allocations();
        Allocation created = allocations.create(AllocationCreate.create("bm-large").name("allocation-1").traits(List.of("CUSTOM_GOLD")));
        List<? extends Allocation> all = allocations.list(Map.of("state", "active"));
        allocations.get("allocation-1");
        allocations.update("allocation-1", List.of(BaremetalPatch.replace("/name", "a2")));
        Assert.assertEquals(allocations.getForNode("n1").getResourceClass(), "bm-large");
        Assert.assertTrue(allocations.delete("allocation-1").isSuccess());
        Assert.assertTrue(allocations.deleteForNode("n1").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v1/allocations")).toString(), "{\"resource_class\":\"bm-large\",\"name\":\"allocation-1\",\"traits\":[\"CUSTOM_GOLD\"]}");
        expect("GET", "/v1/allocations?state=active");
        expect("GET", "/v1/allocations/allocation-1");
        expect("PATCH", "/v1/allocations/allocation-1");
        expect("GET", "/v1/nodes/n1/allocation");
        expect("DELETE", "/v1/allocations/allocation-1");
        expect("DELETE", "/v1/nodes/n1/allocation");
        Assert.assertEquals(created.getState(), "allocating");
        Assert.assertEquals(all.get(0).getTraits(), List.of("CUSTOM_GOLD"));
    }

    public void deployTemplatesListDetailByDefault() throws Exception {
        respondWith(201, TEMPLATE);
        respondWith(200, "{\"deploy_templates\": [" + TEMPLATE + "]}");
        respondWith(200, "{\"deploy_templates\": []}");
        respondWith(200, TEMPLATE);
        respondWith(204);

        var templates = osv3().baremetal().deployTemplates();
        List<Map<String, Object>> steps = List.of(Map.of("interface", "bios", "step", "apply_configuration", "args", Map.of(), "priority", 150));
        DeployTemplate created = templates.create(DeployTemplateCreate.create("CUSTOM_HYPERTHREADING_ON", steps));
        List<? extends DeployTemplate> all = templates.list();
        templates.list(Map.of("detail", "false"));
        templates.update("CUSTOM_HYPERTHREADING_ON", List.of(BaremetalPatch.add("/extra/a", "b")));
        Assert.assertTrue(templates.delete("CUSTOM_HYPERTHREADING_ON").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v1/deploy_templates")).get("steps").get(0).get("priority").asInt(), 150);
        expect("GET", "/v1/deploy_templates?detail=true");
        expect("GET", "/v1/deploy_templates?detail=false");
        expect("PATCH", "/v1/deploy_templates/CUSTOM_HYPERTHREADING_ON");
        expect("DELETE", "/v1/deploy_templates/CUSTOM_HYPERTHREADING_ON");
        Assert.assertEquals(created.getSteps().get(0).get("step"), "apply_configuration");
        Assert.assertEquals(all.get(0).getName(), "CUSTOM_HYPERTHREADING_ON");
    }

    public void runbooksAndInspectionRules() throws Exception {
        respondWith(201, RUNBOOK);
        respondWith(200, RUNBOOK);
        respondWith(201, RULE);
        respondWith(200, "{\"inspection_rules\": [" + RULE + "]}");
        respondWith(204);

        Runbook runbook = osv3().baremetal().runbooks().create(RunbookCreate.create("CUSTOM_AWESOME",
                List.of(Map.of("interface", "bmc", "step", "reset_idrac", "args", Map.of(), "order", 0))).publicRunbook(false).owner("p1"));
        Assert.assertEquals(osv3().baremetal().runbooks().get("CUSTOM_AWESOME").getOwner(), "p1");
        InspectionRule rule = osv3().baremetal().inspectionRules().create(InspectionRuleCreate.create(
                List.of(Map.of("op", "set-attribute", "args", List.of("/driver", "redfish")))).description("set name").phase("main"));
        List<? extends InspectionRule> rules = osv3().baremetal().inspectionRules().list(Map.of("phase", "main"));
        Assert.assertTrue(osv3().baremetal().inspectionRules().delete(rule.getUuid()).isSuccess());

        var create = body(expect("POST", "/v1/runbooks"));
        Assert.assertFalse(create.get("public").asBoolean(true));
        Assert.assertEquals(create.get("owner").asText(), "p1");
        expect("GET", "/v1/runbooks/CUSTOM_AWESOME");
        Assert.assertEquals(body(expect("POST", "/v1/inspection_rules")).get("phase").asText(), "main");
        String query = decodedPath(takeRequest());
        Assert.assertTrue(query.contains("phase=main") && query.contains("detail=true"), query);
        expect("DELETE", "/v1/inspection_rules/783bf33a-a8e3-4d6f-a8a0-0e5c1c7a9d11");
        Assert.assertEquals(runbook.isPublic(), Boolean.FALSE);
        Assert.assertEquals(rule.getActions().get(0).get("op"), "set-attribute");
        Assert.assertEquals(rules.get(0).getPriority(), Integer.valueOf(0));
    }

    public void missingAllocationOfNodeIsNull() throws Exception {
        respondWith(404, "{\"error_message\": \"not found\"}");
        Assert.assertNull(osv3().baremetal().allocations().getForNode("n1"));
        takeRequest();
    }
}
