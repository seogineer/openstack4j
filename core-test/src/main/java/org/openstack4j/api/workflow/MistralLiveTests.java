package org.openstack4j.api.workflow;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.workflow.ActionDefinition;
import org.openstack4j.model.workflow.Scope;
import org.openstack4j.model.workflow.State;
import org.openstack4j.model.workflow.TaskExecution;
import org.openstack4j.model.workflow.WorkflowDefinition;
import org.openstack4j.model.workflow.WorkflowExecution;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.workflow.domain.MistralCronTrigger;
import org.openstack4j.openstack.workflow.domain.MistralWorkflowEnvironment;
import org.openstack4j.openstack.workflow.domain.MistralWorkflowExecution;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Runs against a real Mistral when OS_AUTH_URL is set; every temporary resource is removed in finally. */
@Test(suiteName = "Workflow/Live", groups = "mistral-live", singleThreaded = true)
public class MistralLiveTests {

    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live mistral tests");
        String authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        String token = System.getenv("OS_TOKEN");
        Identifier project = Identifier.byName(env("OS_PROJECT_NAME", null));
        Identifier projectDomain = Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"));
        os = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToProject(project, projectDomain).authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null),
                Identifier.byName(env("OS_USER_DOMAIN_NAME", "Default"))).scopeToProject(project, projectDomain).authenticate();
        requireService(ServiceType.WORKFLOW);
    }

    /** Matches on the resolved service type: the catalog type of a kolla mistral is "workflowv2", only its name says mistral. */
    private void requireService(ServiceType type) {
        if (os.getToken().getCatalog().stream().noneMatch(s -> ServiceType.forCatalogEntry(s.getType(), s.getName()) == type))
            throw new SkipException(type + " not in the service catalog");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live mistral tests");
        }
        return value;
    }

    /** Unique per run so a run that died before its cleanup cannot collide with the next one (Mistral names are unique per project). */
    private static final String SUFFIX = Long.toString(System.currentTimeMillis(), 36);

    /** Skips on a status that only says this cloud is not configured for the feature. */
    private static <T> T skipOn(int status, String why, Supplier<T> call) {
        try {
            return call.get();
        } catch (ResponseException e) {
            if (e.getStatus() == status)
                throw new SkipException(why + ": " + e.getMessage());
            throw e;
        }
    }

    private static <T> T waitUntil(String what, int seconds, Supplier<T> fetch, Predicate<T> done, Predicate<T> failed)
            throws InterruptedException {
        T last = null;
        for (int i = 0; i < seconds; i++) {
            last = fetch.get();
            if (last != null && done.test(last)) return last;
            if (last != null && failed.test(last)) Assert.fail(what + " failed: " + last);
            Thread.sleep(1000);
        }
        Assert.fail(what + " not reached in " + seconds + "s: " + last);
        return last;
    }

    private static String workflowYaml(String name) {
        return "---\n"
                + "version: '2.0'\n\n"
                + name + ":\n"
                + "  type: direct\n"
                + "  output:\n"
                + "    out: <% $.out %>\n"
                + "  tasks:\n"
                + "    hello:\n"
                + "      action: std.echo output=\"os4j\"\n"
                + "      publish:\n"
                + "        out: <% task().result %>\n";
    }

    private static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    public void catalogReads() {
        Assert.assertNotNull(os.workflow().actionDefinitions().get("std.echo"));
        // the full list is large on a stock Mistral; drop this line if the response is too big for the test JVM
        Assert.assertFalse(os.workflow().actionDefinitions().list().isEmpty());
        Map<String, Object> valid = os.workflow().extensions().validateWorkflow(workflowYaml("os4j_live_validate_" + SUFFIX));
        Assert.assertEquals(valid.get("valid"), Boolean.TRUE, "validateWorkflow: " + valid);
        Map<String, Object> broken = os.workflow().extensions().validateWorkflow("version: '2.0'\nbroken: [");
        Assert.assertEquals(broken.get("valid"), Boolean.FALSE, "validateWorkflow: " + broken);
        Assert.assertTrue(broken.containsKey("error"), "validateWorkflow: " + broken);
    }

    public void workflowDefinitionAndExecution() throws InterruptedException {
        String wf = "os4j_live_" + SUFFIX;
        List<? extends WorkflowDefinition> defs = os.workflow().workflowDefinitions().create(stream(workflowYaml(wf)), Scope.PRIVATE);
        Assert.assertEquals(defs.size(), 1);
        Assert.assertEquals(defs.get(0).getName(), wf);
        Assert.assertEquals(defs.get(0).getScope(), Scope.PRIVATE);
        boolean defDeleted;
        try {
            Assert.assertTrue(os.workflow().workflowDefinitions().get(wf).getDefinition().contains("std.echo"));
            WorkflowExecution exec = os.workflow().workflowExecutions().create(
                    MistralWorkflowExecution.builder().workflowName(wf).description("os4j live").build());
            String execId = exec.getId();
            boolean execDeleted;
            try {
                WorkflowExecution done = waitUntil("execution SUCCESS", 30, () -> os.workflow().workflowExecutions().get(execId),
                        e -> e.getState() == State.SUCCESS, e -> e.getState() == State.ERROR);
                Assert.assertEquals(done.getOutput().get("out"), "os4j");

                List<? extends TaskExecution> tasks = os.workflow().taskExecutions().list().stream()
                        .filter(t -> execId.equals(t.getWorkflowExecutionId())).collect(Collectors.toList());
                Assert.assertEquals(tasks.size(), 1);
                Assert.assertEquals(tasks.get(0).getName(), "hello");
                Assert.assertEquals(tasks.get(0).getState(), State.SUCCESS);
                Assert.assertEquals(tasks.get(0).getPublished().get("out"), "os4j");
                Assert.assertFalse(os.workflow().extensions().listTaskExecutions(tasks.get(0).getId()).isEmpty());

                Assert.assertTrue(os.workflow().extensions().executionReport(execId, null).containsKey("root_workflow_execution"));
                Assert.assertTrue(os.workflow().extensions().listSubExecutions(execId, null).isEmpty());

                String cron = "os4j_live_cron_" + SUFFIX;
                os.workflow().cronTriggers().create(MistralCronTrigger.builder().name(cron).pattern("0 0 1 1 *").workflowName(wf).build());
                boolean cronDeleted;
                try {
                    Assert.assertEquals(os.workflow().cronTriggers().get(cron).getPattern(), "0 0 1 1 *");
                    Assert.assertEquals(os.workflow().cronTriggers().get(cron).getWorkflowName(), wf);
                } finally {
                    cronDeleted = os.workflow().cronTriggers().delete(cron).isSuccess();
                }
                Assert.assertTrue(cronDeleted, "cron trigger not deleted");
            } finally {
                execDeleted = os.workflow().workflowExecutions().delete(execId).isSuccess();
            }
            Assert.assertTrue(execDeleted, "execution not deleted");
        } finally {
            // last: Mistral refuses to delete a workflow that a trigger or execution still uses
            defDeleted = os.workflow().workflowDefinitions().delete(wf).isSuccess();
        }
        Assert.assertTrue(defDeleted, "workflow definition not deleted");
    }

    public void environmentLifecycle() {
        String name = "os4j_live_env_" + SUFFIX;
        os.workflow().environments().create(MistralWorkflowEnvironment.builder().name(name).description("os4j live")
                .scope(Scope.PRIVATE).variables(Map.of("k", "v")).build());
        boolean deleted;
        try {
            Assert.assertEquals(os.workflow().environments().get(name).getVariables().get("k"), "v");
            Assert.assertEquals(os.workflow().environments().get(name).getScope(), Scope.PRIVATE);
            Assert.assertTrue(os.workflow().environments().list().stream().anyMatch(e -> name.equals(e.getName())));
        } finally {
            deleted = os.workflow().environments().delete(name).isSuccess();
        }
        Assert.assertTrue(deleted, "environment not deleted");
        Assert.assertNull(os.workflow().environments().get(name));
    }

    public void actionDefinitionLifecycle() {
        String name = "os4j_live_echo_" + SUFFIX;
        String yaml = "---\n"
                + "version: '2.0'\n\n"
                + name + ":\n"
                + "  base: std.echo\n"
                + "  base-input:\n"
                + "    output: <% $.msg %>\n"
                + "  input:\n"
                + "    - msg\n";
        List<? extends ActionDefinition> defs = os.workflow().actionDefinitions().create(stream(yaml), Scope.PRIVATE);
        Assert.assertEquals(defs.size(), 1);
        Assert.assertEquals(defs.get(0).getName(), name);
        boolean deleted;
        try {
            Assert.assertTrue(os.workflow().actionDefinitions().get(name).getInput().contains("msg"));
        } finally {
            deleted = os.workflow().actionDefinitions().delete(name).isSuccess();
        }
        Assert.assertTrue(deleted, "action definition not deleted");
    }

    /** Code sources need Mistral 2024.2 or later; an older Mistral answers 404. */
    public void codeSourceAndDynamicAction() {
        String content = "from mistral_lib import actions\n\nclass Os4jAction(actions.Action):\n    def run(self, context):\n        return 'os4j'\n";
        Map<String, Object> cs = skipOn(404, "code sources API not available",
                () -> os.workflow().extensions().createCodeSource("os4j_live_cs_" + SUFFIX, content, "private"));
        String csId = String.valueOf(cs.get("id"));
        boolean csDeleted;
        try {
            Assert.assertNotNull(os.workflow().extensions().getCodeSource(csId));
            String name = "os4j_live_da_" + SUFFIX;
            os.workflow().extensions().createDynamicAction(Map.of("name", name, "class_name", "Os4jAction", "code_source_id", csId, "scope", "private"));
            boolean daDeleted;
            try {
                Assert.assertEquals(os.workflow().extensions().getDynamicAction(name).get("class_name"), "Os4jAction");
            } finally {
                daDeleted = os.workflow().extensions().deleteDynamicAction(name).isSuccess();
            }
            Assert.assertTrue(daDeleted, "dynamic action not deleted");
        } finally {
            csDeleted = os.workflow().extensions().deleteCodeSource(csId).isSuccess();
        }
        Assert.assertTrue(csDeleted, "code source not deleted");
    }
}
