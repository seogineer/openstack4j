package org.openstack4j.api.ironic;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Ironic/ReviewFixes2")
public class IronicReviewFix2Tests extends AbstractIronicTest {

    public void fieldsFilterSendsNoDetail() throws Exception {
        respondWith(200, "{\"deploy_templates\": []}");
        respondWith(200, "{\"conductors\": []}");
        respondWith(200, "{\"connectors\": []}");
        osv3().ironic().deployTemplates().list(Map.of("fields", "uuid,name"));
        osv3().ironic().conductors().list(Map.of("fields", "hostname"));
        osv3().ironic().volumeConnectors().list(Map.of("fields", "uuid"));
        for (int i = 0; i < 3; i++) {
            String path = decodedPath(takeRequest());
            Assert.assertTrue(path.contains("fields="), path);
            Assert.assertFalse(path.contains("detail"), path);
        }
    }

    public void biosSettingsWithFilters() throws Exception {
        respondWith(200, "{\"bios\": [{\"name\": \"virtualization\", \"value\": \"Enabled\", \"attribute_type\": \"Enumeration\"}]}");
        var bios = osv3().ironic().nodes().listBiosSettings("n1", Map.of("detail", "true"));
        expect("GET", "/v1/nodes/n1/bios?detail=true");
        Assert.assertEquals(bios.get(0).get("attribute_type"), "Enumeration");
    }
}
