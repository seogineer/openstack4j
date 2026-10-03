package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.DefaultSecurityGroupRule;
import org.openstack4j.model.network.ext.SecurityGroupDefaultStatefulness;
import org.openstack4j.model.network.options.DefaultSecurityGroupRuleOptions;
import org.openstack4j.model.network.options.SecurityGroupDefaultStatefulnessOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/SecurityGroupDefaults")
public class SecurityGroupDefaultsTests extends AbstractNetworkingExtTest {

    private static final String RULE = "{\"direction\": \"ingress\", \"ethertype\": \"IPv4\", \"id\": \"2bc0accf-312e-429a-956e-e4407625eb62\", \"port_range_max\": 80, \"port_range_min\": 80,"
            + " \"protocol\": \"tcp\", \"remote_group_id\": null, \"remote_ip_prefix\": null, \"remote_address_group_id\": null, \"used_in_default_sg\": false, \"used_in_non_default_sg\": true, \"description\": \"\"}";

    public void defaultSecurityGroupRules() throws Exception {
        respondWith(201, "{\"default_security_group_rule\": " + RULE + "}");
        respondWith(200, "{\"default_security_group_rules\": [" + RULE + "]}");
        respondWith(200, "{\"default_security_group_rule\": " + RULE + "}");
        respondWith(204);

        var rules = osv3().networking().defaultSecurityGroupRules();
        DefaultSecurityGroupRule created = rules.create(DefaultSecurityGroupRuleOptions.create("ingress").ethertype("IPv4").protocol("tcp")
                .portRangeMin(80).portRangeMax(80).usedInDefaultSg(false).usedInNonDefaultSg(true));
        List<? extends DefaultSecurityGroupRule> all = rules.list();
        rules.get(created.getId());
        rules.delete(created.getId());

        RecordedRequest create = expect("POST", "/v2.0/default-security-group-rules");
        var body = body(create).get("default_security_group_rule");
        Assert.assertEquals(body.get("direction").asText(), "ingress");
        Assert.assertEquals(body.get("port_range_min").asInt(), 80);
        Assert.assertFalse(body.get("used_in_default_sg").asBoolean());
        Assert.assertFalse(body.has("remote_ip_prefix"));
        expect("GET", "/v2.0/default-security-group-rules");
        expect("GET", "/v2.0/default-security-group-rules/" + created.getId());
        expect("DELETE", "/v2.0/default-security-group-rules/" + created.getId());
        Assert.assertEquals(created.getPortRangeMax(), Integer.valueOf(80));
        Assert.assertTrue(all.get(0).getUsedInNonDefaultSg());
        Assert.assertNull(all.get(0).getRemoteIpPrefix());
    }

    public void defaultStatefulness() throws Exception {
        String item = "{\"id\": \"st1\", \"project_id\": \"" + PROJECT + "\", \"stateful\": false}";
        respondWith(201, "{\"security_groups_default_statefulness\": " + item + "}");
        respondWith(200, "{\"security_groups_default_statefulness\": [" + item + "]}");
        respondWith(200, "{\"security_groups_default_statefulness\": " + item + "}");
        respondWith(200, "{\"security_groups_default_statefulness\": {\"id\": \"st1\", \"project_id\": \"" + PROJECT + "\", \"stateful\": true}}");
        respondWith(204);

        var statefulness = osv3().networking().securityGroupDefaultStatefulness();
        SecurityGroupDefaultStatefulness created = statefulness.create(SecurityGroupDefaultStatefulnessOptions.create(false).projectId(PROJECT));
        List<? extends SecurityGroupDefaultStatefulness> all = statefulness.list();
        statefulness.get("st1");
        SecurityGroupDefaultStatefulness updated = statefulness.update("st1", SecurityGroupDefaultStatefulnessOptions.update(true));
        statefulness.delete("st1");

        RecordedRequest create = expect("POST", "/v2.0/security-groups-default-statefulness");
        Assert.assertFalse(body(create).get("security_groups_default_statefulness").get("stateful").asBoolean());
        expect("GET", "/v2.0/security-groups-default-statefulness");
        expect("GET", "/v2.0/security-groups-default-statefulness/st1");
        expect("PUT", "/v2.0/security-groups-default-statefulness/st1");
        expect("DELETE", "/v2.0/security-groups-default-statefulness/st1");
        Assert.assertFalse(created.isStateful());
        Assert.assertEquals(all.size(), 1);
        Assert.assertTrue(updated.isStateful());
    }
}
