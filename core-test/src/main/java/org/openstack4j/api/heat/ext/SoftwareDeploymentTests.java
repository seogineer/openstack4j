package org.openstack4j.api.heat.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.heat.SoftwareConfig;
import org.openstack4j.model.heat.ext.SoftwareDeployment;
import org.openstack4j.model.heat.options.SoftwareDeploymentOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Heat/Ext/SoftwareDeployments")
public class SoftwareDeploymentTests extends AbstractHeatExtTest {

    private static final String DEP = "{\"status\": \"IN_PROGRESS\", \"server_id\": \"ec14c864\", \"config_id\": \"3d5ec2a8\", \"output_values\": null, \"input_values\": {\"foo\": \"bar\"},"
            + " \"action\": \"CREATE\", \"status_reason\": \"Deploy data available\", \"id\": \"06e87bcc\", \"creation_time\": \"2015-01-31T15:12:36Z\", \"updated_time\": null}";

    public void softwareConfigList() throws Exception {
        respondWith(200, "{\"software_configs\": [{\"group\": \"script\", \"id\": \"a6ff3598\", \"name\": \"test_config\", \"creation_time\": \"2015-01-31T15:12:36Z\"}]}");
        respondWith(200, "{\"software_configs\": []}");
        List<? extends SoftwareConfig> configs = osv3().heat().softwareConfig().list();
        osv3().heat().softwareConfig().list(Map.of("limit", "10"));
        expect("GET", "/software_configs");
        expect("GET", "/software_configs?limit=10");
        Assert.assertEquals(configs.get(0).getName(), "test_config");
    }

    public void deployments() throws Exception {
        respondWith(200, "{\"software_deployment\": " + DEP + "}");
        respondWith(200, "{\"software_deployments\": [" + DEP + "]}");
        respondWith(200, "{\"software_deployment\": " + DEP + "}");
        respondWith(200, "{\"software_deployment\": " + DEP + "}");
        respondWith(204);
        respondWith(200, "{\"metadata\": [{\"inputs\": [], \"group\": \"script\", \"name\": \"config_name\", \"config\": \"#!/bin/bash\", \"id\": \"3d5ec2a8\"}]}");

        var deployments = osv3().heat().softwareDeployments();
        SoftwareDeployment created = deployments.create(SoftwareDeploymentOptions.create("ec14c864", "3d5ec2a8").action("CREATE").status("IN_PROGRESS").inputValues(Map.of("foo", "bar")));
        List<? extends SoftwareDeployment> all = deployments.list();
        deployments.get("06e87bcc");
        deployments.update("06e87bcc", SoftwareDeploymentOptions.update().status("COMPLETE").outputValues(Map.of("deploy_status_code", 0)));
        Assert.assertTrue(deployments.delete("06e87bcc").isSuccess());
        List<Map<String, Object>> metadata = deployments.metadata("ec14c864");

        RecordedRequest create = expect("POST", "/software_deployments");
        Assert.assertEquals(body(create).get("server_id").asText(), "ec14c864");
        Assert.assertEquals(body(create).get("input_values").get("foo").asText(), "bar");
        Assert.assertFalse(body(create).has("software_deployment"));
        expect("GET", "/software_deployments");
        expect("GET", "/software_deployments/06e87bcc");
        RecordedRequest update = expect("PUT", "/software_deployments/06e87bcc");
        Assert.assertEquals(body(update).get("status").asText(), "COMPLETE");
        Assert.assertEquals(body(update).size(), 2);
        expect("DELETE", "/software_deployments/06e87bcc");
        expect("GET", "/software_deployments/metadata/ec14c864");
        Assert.assertEquals(created.getConfigId(), "3d5ec2a8");
        Assert.assertEquals(created.getInputValues().get("foo"), "bar");
        Assert.assertNull(all.get(0).getOutputValues());
        Assert.assertEquals(metadata.get(0).get("group"), "script");
    }
}
