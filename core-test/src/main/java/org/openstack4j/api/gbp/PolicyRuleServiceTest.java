package org.openstack4j.api.gbp;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.openstack4j.api.AbstractTest;
import org.openstack4j.api.Builders;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.gbp.PolicyRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test cases for policy rule on GBP
 *
 * @author vinod borole
 */
@Test(suiteName = "grouppolicy/policy_rules")
public class PolicyRuleServiceTest extends AbstractTest {

    private static final String POLICY_RULES = "/network/gbp/policy_rules.json";
    private static final String POLICY_RULE = "/network/gbp/policy_rule.json";
    private static final String POLICY_RULE_UPDATE = "/network/gbp/policy_rule_update.json";


    @Override
    protected Service service() {
        return Service.NETWORK;
    }

    @Test
    public void testListPolicyRule() throws Exception {
        respondWith(POLICY_RULES);
        List<? extends PolicyRule> policyRuleList = osv2().gbp().policyRule().list();
        assertEquals(10, policyRuleList.size());
        Objects.requireNonNull(policyRuleList.get(0));
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : Policy Rule from List : " + policyRuleList.get(0));
        assertEquals(policyRuleList.get(0).getId(), "059909d1-7f20-40cf-a78a-27c340a5aaac");
    }

    @Test
    public void testGetPolicyRule() throws Exception {
        respondWith(POLICY_RULE);
        String id = "059909d1-7f20-40cf-a78a-27c340a5aaac";
        PolicyRule policyrule = osv2().gbp().policyRule().get(id);
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : Policy Rule by ID : " + policyrule);
        assertNotNull(policyrule);
        assertEquals(id, policyrule.getId());
    }

    @Test
    public void testCreatePolicyRule() throws Exception {
        respondWith(POLICY_RULE);
        PolicyRule policyRuleCreate = Builders.policyRule().name("icmp-rule").build();
        PolicyRule policyRule = osv2().gbp().policyRule().create(policyRuleCreate);
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : Create Policy Rule : " + policyRule);
        assertEquals("36e41adb-0b9b-4a11-abd5-66e5386139d4", policyRule.getPolicyClassifierId());
        assertEquals("icmp-rule", policyRule.getName());
    }

    @Test
    public void testUpdatePolicyRule() throws Exception {
        respondWith(POLICY_RULE_UPDATE);
        String id = "059909d1-7f20-40cf-a78a-27c340a5aaac";
        PolicyRule policyRuleUpdate = Builders.policyRule().name("icmp-rule-update").description("icmp-rule-desc-update").build();
        PolicyRule policyRule = osv2().gbp().policyRule().update(id, policyRuleUpdate);
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : Upate Policy Rule : " + policyRule);
        assertEquals("icmp-rule-desc-update", policyRule.getDescription());

    }

    @Test
    public void testDeletePolicyRule() {
        respondWith(200);
        String id = "059909d1-7f20-40cf-a78a-27c340a5aaac";
        ActionResponse result = osv2().gbp().policyRule().delete(id);
        assertTrue(result.isSuccess());
    }


    private static String ruleJson(String flags) {
        return "{\"policy_rule\": {\"id\": \"r1\", \"name\": \"n\"" + flags + "}}";
    }

    @Test
    public void testIsEnabledIndependentOfShared() throws Exception {
        respondWith(200, ruleJson(", \"enabled\": true, \"shared\": false"));
        PolicyRule enabledNotShared = osv2().gbp().policyRule().get("r1");
        assertTrue(enabledNotShared.isEnabled());
        assertFalse(enabledNotShared.isShared());

        respondWith(200, ruleJson(", \"enabled\": false, \"shared\": true"));
        PolicyRule sharedNotEnabled = osv2().gbp().policyRule().get("r1");
        assertFalse(sharedNotEnabled.isEnabled());
        assertTrue(sharedNotEnabled.isShared());
    }

    @Test
    public void testIsEnabledFalseWhenAbsent() throws Exception {
        respondWith(200, ruleJson(", \"shared\": true"));
        assertFalse(osv2().gbp().policyRule().get("r1").isEnabled());
    }

    @Test
    public void testIsEnabledWhenSharedAbsent() throws Exception {
        respondWith(200, ruleJson(", \"enabled\": true"));
        PolicyRule rule = osv2().gbp().policyRule().get("r1");
        assertTrue(rule.isEnabled());
        assertFalse(rule.isShared());
    }

    @Test
    public void testUpdateSendsEnabledOfModel() throws Exception {
        JsonNode body = updateBodyOfFetchedRule(true, false);
        assertNotNull(body.get("enabled"));
        assertTrue(body.get("enabled").asBoolean());
        assertNotNull(body.get("shared"));
        assertFalse(body.get("shared").asBoolean());
    }

    @Test
    public void testUpdateSendsDisabledSharedRule() throws Exception {
        JsonNode body = updateBodyOfFetchedRule(false, true);
        assertNotNull(body.get("enabled"));
        assertFalse(body.get("enabled").asBoolean());
        assertNotNull(body.get("shared"));
        assertTrue(body.get("shared").asBoolean());
    }

    private JsonNode updateBodyOfFetchedRule(boolean enabled, boolean shared) throws Exception {
        String flags = ", \"enabled\": " + enabled + ", \"shared\": " + shared;
        respondWith(200, ruleJson(flags));
        PolicyRule rule = osv2().gbp().policyRule().get("r1");
        respondWith(200, ruleJson(flags));
        osv2().gbp().policyRule().update("r1", rule);

        // requests of other tests in this class may still be queued
        RecordedRequest put = takeRequest();
        while (!"PUT".equals(put.getMethod()) || !put.getPath().endsWith("/policy_rules/r1")) {
            put = takeRequest();
        }
        return new ObjectMapper().readTree(put.getBody().readUtf8()).get("policy_rule");
    }

}
