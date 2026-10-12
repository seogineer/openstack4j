package org.openstack4j.api.heat.ext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.heat.ext.StackSnapshot;
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

    private static final String TEMPLATE_10 = TEMPLATE.replace("length: 8", "length: 10");

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

            // previews change nothing; the same template leaves the resource unchanged
            Map<String, List<Map<String, Object>>> same = os.heat().stacks().previewUpdate(name, stack.getId(),
                    Builders.stackUpdate().template(TEMPLATE).build());
            Assert.assertTrue(names(same.get("unchanged")).contains("random"), String.valueOf(same));
            Assert.assertTrue(names(same.get("added")).isEmpty() && names(same.get("deleted")).isEmpty(), String.valueOf(same));
            Map<String, List<Map<String, Object>>> longer = os.heat().stacks().previewPatchUpdate(name, stack.getId(),
                    Builders.stackUpdate().template(TEMPLATE_10).build());
            Assert.assertFalse(names(longer.get("unchanged")).contains("random"), String.valueOf(longer));

            // PATCH update: RandomString is rebuilt with the new length
            Assert.assertTrue(os.heat().stacks().patchUpdate(name, stack.getId(), Builders.stackUpdate().template(TEMPLATE_10).build()).isSuccess());
            waitFor(name, stack.getId(), "UPDATE_COMPLETE");
            Assert.assertEquals(String.valueOf(os.heat().stacks().output(name, stack.getId(), "value").getOutputValue()).length(), 10);

            // snapshot: create, wait, read, list, delete
            StackSnapshot snapshot = os.heat().stacks().snapshot(name, stack.getId(), "os4j-live-snap");
            waitForSnapshot(name, stack.getId(), snapshot.getId());
            waitFor(name, stack.getId(), "SNAPSHOT_COMPLETE");
            StackSnapshot shown = os.heat().stacks().getSnapshot(name, stack.getId(), snapshot.getId());
            Assert.assertEquals(shown.getName(), "os4j-live-snap");
            Assert.assertNotNull(shown.getData());
            Assert.assertTrue(os.heat().stacks().snapshots(name, stack.getId()).stream().anyMatch(s -> snapshot.getId().equals(s.getId())));
            Assert.assertTrue(os.heat().stacks().deleteSnapshot(name, stack.getId(), snapshot.getId()).isSuccess());
            waitForNoSnapshots(name, stack.getId());

            Assert.assertTrue(os.heat().stacks().suspend(name, stack.getId()).isSuccess());
            waitFor(name, stack.getId(), "SUSPEND_COMPLETE");
            Assert.assertTrue(os.heat().stacks().resume(name, stack.getId()).isSuccess());
            waitFor(name, stack.getId(), "RESUME_COMPLETE");
            exportUnlessAbandonDisabled(name, stack.getId());
        } finally {
            deleted = os.heat().stacks().delete(name).isSuccess();
        }
        Assert.assertTrue(deleted, "stack not deleted");
    }

    /** Export shares Heat's enable_stack_abandon gate; a cloud with it off answers 400 "Stack Abandon is not supported". */
    private void exportUnlessAbandonDisabled(String name, String id) {
        Map<String, Object> export;
        try {
            export = os.heat().stacks().export(name, id);
        } catch (ClientResponseException e) {
            if (e.getStatus() == 400 && String.valueOf(e.getMessage()).contains("Abandon"))
                return;
            throw e;
        }
        Assert.assertEquals(export.get("name"), name);
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

    private static List<String> names(List<Map<String, Object>> resources) {
        List<String> names = new ArrayList<>();
        if (resources != null)
            for (Map<String, Object> r : resources)
                names.add(String.valueOf(r.get("resource_name")));
        return names;
    }

    private void waitForSnapshot(String name, String id, String snapshotId) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            StackSnapshot s = os.heat().stacks().getSnapshot(name, id, snapshotId);
            String status = s == null ? null : s.getStatus();
            if ("COMPLETE".equals(status))
                return;
            if ("FAILED".equals(status))
                Assert.fail("snapshot FAILED: " + s.getStatusReason());
            Thread.sleep(1000);
        }
        Assert.fail("snapshot did not complete");
    }

    private void waitForNoSnapshots(String name, String id) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            if (os.heat().stacks().snapshots(name, id).isEmpty())
                return;
            Thread.sleep(1000);
        }
        Assert.fail("snapshot not deleted");
    }

    public void softwareListings() {
        Assert.assertNotNull(os.heat().softwareConfig().list());
        Assert.assertNotNull(os.heat().softwareDeployments().list());
    }
}
