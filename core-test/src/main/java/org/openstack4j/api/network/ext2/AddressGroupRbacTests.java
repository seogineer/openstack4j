package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.AddressGroup;
import org.openstack4j.model.network.ext.RbacPolicy;
import org.openstack4j.model.network.options.AddressGroupOptions;
import org.openstack4j.model.network.options.RbacPolicyOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/AddressGroupsRbac")
public class AddressGroupRbacTests extends AbstractNetworkingExtTest {

    private static final String AG = "9f60873a-9bb0-45c9-ac25-b1d5d668d955";

    private static String group(String addresses) {
        return "{\"id\": \"" + AG + "\", \"name\": \"address-group-2\", \"project_id\": \"" + PROJECT + "\", \"addresses\": [" + addresses + "], \"description\": \"docs example group\"}";
    }

    public void addressGroups() throws Exception {
        respondWith(201, "{\"address_group\": " + group("") + "}");
        respondWith(200, "{\"address_groups\": [" + group("") + "]}");
        respondWith(200, "{\"address_group\": " + group("\"10.0.2.100/32\"") + "}");
        respondWith(200, "{\"address_group\": " + group("") + "}");
        respondWith(200, "{\"address_group\": " + group("") + "}");
        respondWith(200, "{\"address_group\": " + group("") + "}");
        respondWith(204);

        var groups = osv3().networking().addressGroups();
        AddressGroup created = groups.create(AddressGroupOptions.create("address-group-2").description("docs example group"));
        groups.list();
        AddressGroup added = groups.addAddresses(AG, List.of("10.0.2.100/32"));
        groups.removeAddresses(AG, List.of("10.0.2.100/32"));
        groups.get(AG);
        groups.update(AG, AddressGroupOptions.update().name("renamed"));
        groups.delete(AG);

        RecordedRequest create = expect("POST", "/v2.0/address-groups");
        Assert.assertEquals(body(create).get("address_group").get("name").asText(), "address-group-2");
        Assert.assertFalse(body(create).get("address_group").has("addresses"));
        expect("GET", "/v2.0/address-groups");
        RecordedRequest add = expect("PUT", "/v2.0/address-groups/" + AG + "/add_addresses");
        Assert.assertEquals(body(add).get("addresses").get(0).asText(), "10.0.2.100/32");
        expect("PUT", "/v2.0/address-groups/" + AG + "/remove_addresses");
        expect("GET", "/v2.0/address-groups/" + AG);
        expect("PUT", "/v2.0/address-groups/" + AG);
        expect("DELETE", "/v2.0/address-groups/" + AG);
        Assert.assertEquals(created.getDescription(), "docs example group");
        Assert.assertEquals(added.getAddresses(), List.of("10.0.2.100/32"));
    }

    public void rbacPolicies() throws Exception {
        String policy = "{\"target_tenant\": \"*\", \"tenant_id\": \"" + PROJECT + "\", \"object_type\": \"network\", \"object_id\": \"1f32f072-4d17-4811-b619-3623d018bd40\","
                + " \"action\": \"access_as_external\", \"project_id\": \"" + PROJECT + "\", \"id\": \"6d4c666e-1aad-465e-b670-4d112b760137\"}";
        respondWith(201, "{\"rbac_policy\": " + policy + "}");
        respondWith(200, "{\"rbac_policies\": [" + policy + "]}");
        respondWith(200, "{\"rbac_policy\": " + policy + "}");
        respondWith(200, "{\"rbac_policy\": " + policy + "}");
        respondWith(204);

        var rbac = osv3().networking().rbacPolicies();
        RbacPolicy created = rbac.create(RbacPolicyOptions.create("network", "1f32f072-4d17-4811-b619-3623d018bd40", "access_as_external", "*"));
        List<? extends RbacPolicy> all = rbac.list();
        rbac.get(created.getId());
        rbac.update(created.getId(), RbacPolicyOptions.update("p2"));
        rbac.delete(created.getId());

        RecordedRequest create = expect("POST", "/v2.0/rbac-policies");
        var body = body(create).get("rbac_policy");
        Assert.assertEquals(body.get("object_type").asText(), "network");
        Assert.assertEquals(body.get("action").asText(), "access_as_external");
        Assert.assertEquals(body.get("target_tenant").asText(), "*");
        expect("GET", "/v2.0/rbac-policies");
        expect("GET", "/v2.0/rbac-policies/" + created.getId());
        RecordedRequest update = expect("PUT", "/v2.0/rbac-policies/" + created.getId());
        Assert.assertEquals(body(update).get("rbac_policy").get("target_tenant").asText(), "p2");
        expect("DELETE", "/v2.0/rbac-policies/" + created.getId());
        Assert.assertEquals(created.getTargetTenant(), "*");
        Assert.assertEquals(all.get(0).getObjectId(), "1f32f072-4d17-4811-b619-3623d018bd40");
    }
}
