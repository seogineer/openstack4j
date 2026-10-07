package org.openstack4j.api.heat.ext;

import java.util.Map;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.heat.domain.HeatStackCreate;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Runs against a real Heat when OS_AUTH_URL is set; uses only OS::Heat::RandomString, so no compute is needed. */
@Test(suiteName = "Heat/Ext/Live", groups = "heat-live", singleThreaded = true)
public class HeatExtensionsLiveTests {

    private static final String TEMPLATE = "heat_template_version: 2016-10-14\nresources:\n  random:\n    type: OS::Heat::RandomString\n    properties:\n      length: 8\n"
            + "outputs:\n  value:\n    value: {get_attr: [random, value]}\n";

    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live heat tests");
        String authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        String token = System.getenv("OS_TOKEN");
        Identifier project = Identifier.byName(env("OS_PROJECT_NAME", null));
        Identifier projectDomain = Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"));
        os = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToProject(project, projectDomain).authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null),
                Identifier.byName(env("OS_USER_DOMAIN_NAME", "Default"))).scopeToProject(project, projectDomain).authenticate();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live heat tests");
        }
        return value;
    }

    public void infoAndTypes() {
        Assert.assertFalse(os.heat().info().buildInfo().isEmpty());
        Assert.assertFalse(os.heat().templateVersions().list().isEmpty());
        Assert.assertTrue(os.heat().resourceTypes().list().contains("OS::Heat::RandomString"));
        Assert.assertTrue(os.heat().resourceTypes().schema("OS::Heat::RandomString").containsKey("properties"));
    }

    public void stackLifecycle() throws Exception {
        String name = "os4j-live-" + Long.toString(System.currentTimeMillis(), 36);
        Assert.assertTrue(os.heat().stacks().preview(HeatStackCreate.build().name(name).template(TEMPLATE).build()).containsKey("resources"));
        Stack stack = os.heat().stacks().create(HeatStackCreate.build().name(name).template(TEMPLATE).build());
        boolean deleted = false;
        try {
            waitFor(name, stack.getId(), "CREATE_COMPLETE");
            Assert.assertEquals(os.heat().stacks().outputs(name, stack.getId()).get(0).getOutputKey(), "value");
            Assert.assertEquals(String.valueOf(os.heat().stacks().output(name, stack.getId(), "value").getOutputValue()).length(), 8);
            Assert.assertNotNull(os.heat().stacks().environment(name, stack.getId()));
            Assert.assertNotNull(os.heat().stacks().files(name, stack.getId()));
            Assert.assertFalse(os.heat().events().list(name).isEmpty());
            Assert.assertTrue(os.heat().stacks().suspend(name, stack.getId()).isSuccess());
            waitFor(name, stack.getId(), "SUSPEND_COMPLETE");
            Assert.assertTrue(os.heat().stacks().resume(name, stack.getId()).isSuccess());
            waitFor(name, stack.getId(), "RESUME_COMPLETE");
            Map<String, Object> export = os.heat().stacks().export(name, stack.getId());
            Assert.assertEquals(export.get("name"), name);
        } finally {
            deleted = os.heat().stacks().delete(name).isSuccess();
        }
        Assert.assertTrue(deleted, "stack not deleted");
    }

    private void waitFor(String name, String id, String status) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            String current = os.heat().stacks().getDetails(name, id).getStatus();
            if (status.equals(current))
                return;
            if (current != null && current.endsWith("FAILED"))
                Assert.fail("stack " + current);
            Thread.sleep(1000);
        }
        Assert.fail("stack did not reach " + status);
    }

    public void softwareListings() {
        Assert.assertNotNull(os.heat().softwareConfig().list());
        Assert.assertNotNull(os.heat().softwareDeployments().list());
    }
}
