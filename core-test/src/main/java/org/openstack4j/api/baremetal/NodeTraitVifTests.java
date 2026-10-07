package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.NodeService;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/NodeTraitsVifs")
public class NodeTraitVifTests extends AbstractBaremetalTest {

    private NodeService nodes() {
        return osv3().baremetal().nodes();
    }

    public void traits() throws Exception {
        respondWith(200, "{\"traits\": [\"CUSTOM_TRAIT1\", \"HW_CPU_X86_VMX\"]}");
        respondWith(204);
        respondWith(204);
        respondWith(204);
        respondWith(204);

        List<String> traits = nodes().listTraits("n1");
        Assert.assertTrue(nodes().setTraits("n1", List.of("CUSTOM_A", "CUSTOM_B")).isSuccess());
        Assert.assertTrue(nodes().addTrait("n1", "CUSTOM_C").isSuccess());
        Assert.assertTrue(nodes().removeTrait("n1", "CUSTOM_A").isSuccess());
        Assert.assertTrue(nodes().removeAllTraits("n1").isSuccess());

        expect("GET", "/v1/nodes/n1/traits");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/traits")).toString(), "{\"traits\":[\"CUSTOM_A\",\"CUSTOM_B\"]}");
        expect("PUT", "/v1/nodes/n1/traits/CUSTOM_C");
        expect("DELETE", "/v1/nodes/n1/traits/CUSTOM_A");
        expect("DELETE", "/v1/nodes/n1/traits");
        Assert.assertEquals(traits, List.of("CUSTOM_TRAIT1", "HW_CPU_X86_VMX"));
    }

    public void vifs() throws Exception {
        respondWith(200, "{\"vifs\": [{\"id\": \"1974dcfa-836f-41b2-b541-686c100900e5\"}]}");
        respondWith(204);
        respondWith(204);
        respondWith(204);

        List<String> vifs = nodes().listVifs("n1");
        Assert.assertTrue(nodes().attachVif("n1", "v1").isSuccess());
        Assert.assertTrue(nodes().attachVif("n1", "v2", Map.of("port_uuid", "p1")).isSuccess());
        Assert.assertTrue(nodes().detachVif("n1", "v1").isSuccess());

        expect("GET", "/v1/nodes/n1/vifs");
        Assert.assertEquals(body(expect("POST", "/v1/nodes/n1/vifs")).toString(), "{\"id\":\"v1\"}");
        Assert.assertEquals(body(expect("POST", "/v1/nodes/n1/vifs")).toString(), "{\"id\":\"v2\",\"port_uuid\":\"p1\"}");
        expect("DELETE", "/v1/nodes/n1/vifs/v1");
        Assert.assertEquals(vifs, List.of("1974dcfa-836f-41b2-b541-686c100900e5"));
    }

    @Test(expectedExceptions = org.openstack4j.api.exceptions.ResponseException.class)
    public void listTraitsRaisesOn404() throws Exception {
        respondWith(404, "{\"error_message\": \"not found\"}");
        try {
            nodes().listTraits("n9");
        } finally {
            takeRequest();
        }
    }
}
