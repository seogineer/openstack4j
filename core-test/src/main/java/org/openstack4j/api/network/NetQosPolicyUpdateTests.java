package org.openstack4j.api.network;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.api.Builders;
import org.openstack4j.api.network.ext2.AbstractNetworkingExtTest;
import org.openstack4j.model.network.ext.NetQosPolicy;
import org.openstack4j.model.network.ext.NetQosPolicyUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

/** PUT /qos/policies/{id}: only the fields set through the builder are sent. */
@Test(suiteName = "Network/QosPolicyUpdate")
public class NetQosPolicyUpdateTests extends AbstractNetworkingExtTest {

    private static final String POLICY = "{\"policy\":{\"id\":\"q1\",\"name\":\"renamed\",\"description\":\"d\",\"shared\":true,"
            + "\"is_default\":false,\"project_id\":\"p1\",\"tenant_id\":\"p1\",\"rules\":[],\"tags\":[]}}";

    private static Set<String> keys(JsonNode node) {
        Set<String> keys = new HashSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    public void onlyNameIsSent() throws Exception {
        respondWith(200, POLICY);

        NetQosPolicy updated = osv3().networking().netQosPolicy()
                .update(Builders.netQosPolicyUpdate().id("q1").name("renamed").build());

        JsonNode body = body(expect("PUT", "/qos/policies/q1"));
        Assert.assertEquals(keys(body), Set.of("policy"));
        Assert.assertEquals(keys(body.get("policy")), Set.of("name"));
        Assert.assertEquals(body.get("policy").get("name").asText(), "renamed");
        Assert.assertEquals(updated.getId(), "q1");
        Assert.assertEquals(updated.getName(), "renamed");
        Assert.assertTrue(updated.isShared());
    }

    public void allFieldsAreSentWithApiNames() throws Exception {
        respondWith(200, POLICY);

        osv3().networking().netQosPolicy().update(Builders.netQosPolicyUpdate()
                .id("q1").name("n").description("d").shared(true).isDefault(true).build());

        JsonNode policy = body(expect("PUT", "/qos/policies/q1")).get("policy");
        Assert.assertEquals(keys(policy), Set.of("name", "description", "shared", "is_default"));
        Assert.assertTrue(policy.get("shared").asBoolean());
        Assert.assertTrue(policy.get("is_default").asBoolean());
        Assert.assertEquals(policy.get("description").asText(), "d");
    }

    public void falseValuesAreSentWhenSet() throws Exception {
        respondWith(200, POLICY);

        osv3().networking().netQosPolicy().update(Builders.netQosPolicyUpdate().id("q1").shared(false).isDefault(false).build());

        JsonNode policy = body(expect("PUT", "/qos/policies/q1")).get("policy");
        Assert.assertEquals(keys(policy), Set.of("shared", "is_default"));
        Assert.assertFalse(policy.get("shared").asBoolean());
        Assert.assertFalse(policy.get("is_default").asBoolean());
    }

    public void idIsNeverInTheBody() throws Exception {
        respondWith(200, POLICY);

        osv3().networking().netQosPolicy().update(Builders.netQosPolicyUpdate().id("q1").description("only").build());

        String raw = expect("PUT", "/qos/policies/q1").getBody().readUtf8();
        Assert.assertFalse(raw.contains("\"id\""), raw);
        Assert.assertFalse(raw.contains("isDefault"), raw);
        Assert.assertFalse(raw.contains("default\""), raw.replace("is_default", ""));
    }

    public void toBuilderKeepsTheSetFields() throws Exception {
        NetQosPolicyUpdate update = Builders.netQosPolicyUpdate().id("q1").name("a").build();
        NetQosPolicyUpdate changed = update.toBuilder().description("b").build();
        respondWith(200, POLICY);

        osv3().networking().netQosPolicy().update(changed);

        JsonNode policy = body(expect("PUT", "/qos/policies/q1")).get("policy");
        Assert.assertEquals(keys(policy), Set.of("name", "description"));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void idIsRequired() {
        osv3().networking().netQosPolicy().update(Builders.netQosPolicyUpdate().name("x").build());
    }
}
