package org.openstack4j.api.registration;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Registration")
public class RegistrationTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.REGISTRATION;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    private static String json(RecordedRequest r) throws Exception {
        return new ObjectMapper().readTree(r.getBody().readUtf8()).toString();
    }

    public void adminTasksTokensNotifications() throws Exception {
        respondWith(200, "{\"versions\": [{\"id\": \"v1\", \"status\": \"CURRENT\"}]}");
        respondWith(200, "{\"version\": {\"id\": \"v1\"}}");
        respondWith(200, "{\"last_created_task\": null, \"error_notifications\": []}");
        respondWith(200, "{\"tasks\": [{\"uuid\": \"t1\", \"task_type\": \"invite_user_to_project\"}], \"pages\": 1, \"has_more\": false, \"has_prev\": false}");
        respondWith(200, "{\"uuid\": \"t1\", \"approved\": false}");
        respondWith(200, "{\"notes\": [\"Task successfully updated.\"]}");
        respondWith(200, "{\"notes\": [\"created token\"]}");
        respondWith(200, "{\"notes\": [\"Task cancelled.\"]}");
        respondWith(200, "{\"tokens\": [{\"token\": \"k1\", \"task\": \"t1\"}]}");
        respondWith(200, "{\"notes\": [\"Token reissued.\"]}");
        respondWith(200, "{\"notes\": [\"Deleted all expired tokens.\"]}");
        respondWith(200, "{\"actions\": [\"NewUserAction\"], \"required_fields\": [\"password\"], \"task_type\": \"invite_user_to_project\"}");
        respondWith(200, "{\"notes\": [\"Token submitted successfully.\"]}");
        respondWith(200, "{\"notifications\": [{\"uuid\": \"n1\", \"acknowledged\": false}]}");
        respondWith(200, "{\"notes\": [\"Notifications acknowledged.\"]}");
        respondWith(200, "{\"uuid\": \"n1\", \"error\": true}");
        respondWith(200, "{\"notes\": [\"Notification acknowledged.\"]}");
        respondWith(404, "{\"errors\": [\"No task with this id.\"]}");

        var reg = osv3().registration();
        Assert.assertEquals(((List<?>) reg.versions().get("versions")).size(), 1);
        Assert.assertEquals(((Map<?, ?>) reg.version().get("version")).get("id"), "v1");
        Assert.assertTrue(reg.status().containsKey("error_notifications"));
        Map<String, Object> tasks = reg.listTasks(Map.of("page", "1", "tasks_per_page", "25"), Map.of("task_type", Map.of("exact", "invite_user_to_project")));
        Assert.assertEquals(((List<?>) tasks.get("tasks")).size(), 1);
        Assert.assertEquals(reg.getTask("t1").get("approved"), false);
        Assert.assertTrue(reg.updateTask("t1", Map.of("email", "a@example.com")).isSuccess());
        Assert.assertTrue(reg.approveTask("t1").isSuccess());
        Assert.assertTrue(reg.cancelTask("t1").isSuccess());
        Assert.assertEquals(reg.listTokens(null).get(0).get("token"), "k1");
        Assert.assertTrue(reg.reissueToken("t1").isSuccess());
        Assert.assertTrue(reg.deleteExpiredTokens().isSuccess());
        Assert.assertEquals(reg.getToken("k1").get("required_fields"), List.of("password"));
        Assert.assertTrue(reg.submitToken("k1", Map.of("password", "s3cret")).isSuccess());
        Assert.assertEquals(reg.listNotifications(Map.of("acknowledged", Map.of("exact", false))).get(0).get("uuid"), "n1");
        Assert.assertTrue(reg.acknowledgeNotifications(List.of("n1", "n2")).isSuccess());
        Assert.assertEquals(reg.getNotification("n1").get("error"), true);
        Assert.assertTrue(reg.acknowledgeNotification("n1").isSuccess());
        Assert.assertNull(reg.getTask("missing"));

        Assert.assertEquals(path(takeRequest()), "/");
        Assert.assertEquals(path(takeRequest()), "/v1");
        Assert.assertEquals(path(takeRequest()), "/v1/status");
        RecordedRequest r = takeRequest();
        Assert.assertTrue(path(r).startsWith("/v1/tasks?"), path(r));
        Assert.assertTrue(path(r).contains("page=1") && path(r).contains("tasks_per_page=25"), path(r));
        Assert.assertTrue(path(r).contains("filters={\"task_type\":{\"exact\":\"invite_user_to_project\"}}"), path(r));
        Assert.assertEquals(path(takeRequest()), "/v1/tasks/t1");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PUT");
        Assert.assertEquals(json(r), "{\"email\":\"a@example.com\"}");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "POST");
        Assert.assertEquals(path(r), "/v1/tasks/t1");
        Assert.assertEquals(json(r), "{\"approved\":true}");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "DELETE");
        Assert.assertEquals(path(r), "/v1/tasks/t1");
        Assert.assertEquals(path(takeRequest()), "/v1/tokens");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "POST");
        Assert.assertEquals(path(r), "/v1/tokens");
        Assert.assertEquals(json(r), "{\"task\":\"t1\"}");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "DELETE");
        Assert.assertEquals(path(r), "/v1/tokens");
        Assert.assertEquals(path(takeRequest()), "/v1/tokens/k1");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "POST");
        Assert.assertEquals(path(r), "/v1/tokens/k1");
        Assert.assertEquals(json(r), "{\"password\":\"s3cret\"}");
        Assert.assertEquals(path(takeRequest()), "/v1/notifications?filters={\"acknowledged\":{\"exact\":false}}");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "POST");
        Assert.assertEquals(json(r), "{\"notifications\":[\"n1\",\"n2\"]}");
        Assert.assertEquals(path(takeRequest()), "/v1/notifications/n1");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "POST");
        Assert.assertEquals(path(r), "/v1/notifications/n1");
        Assert.assertEquals(json(r), "{\"acknowledged\":true}");
        takeRequest();
    }

    public void openstackUsersRolesQuotas() throws Exception {
        respondWith(200, "{\"users\": [{\"id\": \"u1\", \"email\": \"a@example.com\", \"cohort\": \"Member\"}]}");
        respondWith(202, "{\"notes\": [\"task created\"]}");
        respondWith(200, "{\"id\": \"u1\", \"roles\": [\"member\"]}");
        respondWith(200, "{\"notes\": [\"Cancelled.\"]}");
        respondWith(200, "{\"roles\": [\"member\"], \"inherited_roles\": []}");
        respondWith(202, "{\"notes\": [\"task created\"]}");
        respondWith(202, "{\"notes\": [\"task created\"]}");
        respondWith(200, "{\"roles\": [{\"name\": \"member\"}, {\"name\": \"project_admin\"}]}");
        respondWith(202, "{\"notes\": [\"If user with email exists, reset token will be issued.\"]}");
        respondWith(202, "{\"notes\": [\"task created\"]}");
        respondWith(202, "{\"notes\": [\"task created\"]}");
        respondWith(200, "{\"regions\": [{\"region\": \"RegionOne\", \"current_quota_size\": \"small\"}], \"quota_size_order\": [\"small\", \"medium\"]}");
        respondWith(202, "{\"notes\": [\"Task processed. Awaiting Aprroval.\"]}");
        respondWith(404, "{\"errors\": [\"Not found\"]}");

        var reg = osv3().registration();
        Assert.assertEquals(reg.listUsers().get(0).get("cohort"), "Member");
        Assert.assertTrue(reg.inviteUser("a@example.com", List.of("member"), null, Map.of("username", "alice")).isSuccess());
        Assert.assertEquals(reg.getUser("u1").get("id"), "u1");
        Assert.assertTrue(reg.cancelInvite("u1").isSuccess());
        Assert.assertEquals(reg.getUserRoles("u1").get("roles"), List.of("member"));
        Assert.assertTrue(reg.addUserRoles("u1", List.of("project_admin"), List.of("member")).isSuccess());
        Assert.assertTrue(reg.removeUserRoles("u1", List.of("project_admin"), null).isSuccess());
        Assert.assertEquals(reg.listRoles().size(), 2);
        Assert.assertTrue(reg.resetPassword("a@example.com", null).isSuccess());
        Assert.assertTrue(reg.updateEmail("b@example.com").isSuccess());
        Assert.assertTrue(reg.signUp("c@example.com", "new-project", null).isSuccess());
        Assert.assertEquals(reg.getQuotas(List.of("RegionOne")).get("quota_size_order"), List.of("small", "medium"));
        Assert.assertTrue(reg.updateQuotas("medium", List.of("RegionOne")).isSuccess());
        Assert.assertNull(reg.getUser("missing"));

        Assert.assertEquals(path(takeRequest()), "/v1/openstack/users");
        RecordedRequest r = takeRequest();
        Assert.assertEquals(r.getMethod(), "POST");
        Assert.assertEquals(path(r), "/v1/openstack/users");
        Map<?, ?> invite = new ObjectMapper().readValue(r.getBody().readUtf8(), Map.class);
        Assert.assertEquals(invite.get("email"), "a@example.com");
        Assert.assertEquals(invite.get("roles"), List.of("member"));
        Assert.assertEquals(invite.get("username"), "alice");
        Assert.assertFalse(invite.containsKey("inherited_roles"));
        Assert.assertEquals(path(takeRequest()), "/v1/openstack/users/u1");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "DELETE");
        Assert.assertEquals(path(r), "/v1/openstack/users/u1");
        Assert.assertEquals(path(takeRequest()), "/v1/openstack/users/u1/roles");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "PUT");
        Assert.assertEquals(path(r), "/v1/openstack/users/u1/roles");
        Map<?, ?> add = new ObjectMapper().readValue(r.getBody().readUtf8(), Map.class);
        Assert.assertEquals(add.get("roles"), List.of("project_admin"));
        Assert.assertEquals(add.get("inherited_roles"), List.of("member"));
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "DELETE");
        Assert.assertEquals(path(r), "/v1/openstack/users/u1/roles");
        Assert.assertEquals(json(r), "{\"roles\":[\"project_admin\"]}");
        Assert.assertEquals(path(takeRequest()), "/v1/openstack/roles");
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/openstack/users/password-reset");
        Assert.assertEquals(json(r), "{\"email\":\"a@example.com\"}");
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/openstack/users/email-update");
        Assert.assertEquals(json(r), "{\"new_email\":\"b@example.com\"}");
        r = takeRequest();
        Assert.assertEquals(path(r), "/v1/openstack/sign-up");
        Map<?, ?> signUp = new ObjectMapper().readValue(r.getBody().readUtf8(), Map.class);
        Assert.assertEquals(signUp.get("email"), "c@example.com");
        Assert.assertEquals(signUp.get("project_name"), "new-project");
        Assert.assertEquals(path(takeRequest()), "/v1/openstack/quotas?regions=RegionOne");
        r = takeRequest();
        Assert.assertEquals(r.getMethod(), "POST");
        Assert.assertEquals(path(r), "/v1/openstack/quotas");
        Map<?, ?> quota = new ObjectMapper().readValue(r.getBody().readUtf8(), Map.class);
        Assert.assertEquals(quota.get("size"), "medium");
        Assert.assertEquals(quota.get("regions"), List.of("RegionOne"));
        takeRequest();
    }

    public void inheritedRolesOnlyAndNotificationPages() throws Exception {
        respondWith(202, "{\"notes\": [\"task created\"]}");
        respondWith(202, "{\"notes\": [\"task created\"]}");
        respondWith(200, "{\"notifications\": [{\"uuid\": \"n1\"}], \"pages\": 3, \"has_more\": true, \"has_prev\": false}");

        var reg = osv3().registration();
        Assert.assertTrue(reg.inviteUser("a@example.com", null, List.of("member"), null).isSuccess());
        Assert.assertTrue(reg.addUserRoles("u1", null, List.of("member")).isSuccess());
        Map<String, Object> page = reg.listNotifications(Map.of("page", "2", "notifications_per_page", "10"), null);
        Assert.assertEquals(page.get("pages"), 3);
        Assert.assertThrows(IllegalArgumentException.class, () -> reg.addUserRoles("u1", null, null));

        Map<?, ?> invite = new ObjectMapper().readValue(takeRequest().getBody().readUtf8(), Map.class);
        Assert.assertFalse(invite.containsKey("roles"));
        Assert.assertEquals(invite.get("inherited_roles"), List.of("member"));
        Assert.assertEquals(json(takeRequest()), "{\"inherited_roles\":[\"member\"]}");
        String p = path(takeRequest());
        Assert.assertTrue(p.startsWith("/v1/notifications?") && p.contains("page=2") && p.contains("notifications_per_page=10"), p);
    }

    public void failuresCarryAdjutantErrors() throws Exception {
        respondWith(404, "\"Not found.\"");
        respondWith(501, "\"Revoking keystone users not implemented. Try removing all roles instead.\"");
        respondWith(400, "{\"errors\": [\"Task already approved.\"]}");
        respondWith(400, "{\"errors\": {\"email\": [\"Enter a valid email address.\"]}}");
        respondWith(200, "\"Cancelled pending invite task!\"");

        var reg = osv3().registration();
        var notFound = reg.cancelInvite("deadbeef");
        Assert.assertFalse(notFound.isSuccess());
        Assert.assertEquals(notFound.getCode(), 404);
        Assert.assertEquals(notFound.getFault(), "Not found.");
        var notImplemented = reg.cancelInvite("u1");
        Assert.assertFalse(notImplemented.isSuccess());
        Assert.assertEquals(notImplemented.getCode(), 501);
        Assert.assertTrue(notImplemented.getFault().startsWith("Revoking keystone users"), notImplemented.getFault());
        Assert.assertEquals(reg.approveTask("t1").getFault(), "Task already approved.");
        Assert.assertEquals(reg.inviteUser("bad", List.of("member"), null, null).getFault(), "email: Enter a valid email address.");
        Assert.assertTrue(reg.cancelInvite("u2").isSuccess());
        for (int i = 0; i < 5; i++)
            takeRequest();
    }

    public void failedActionsReportTheError() throws Exception {
        respondWith(400, "{\"errors\": [\"Task already approved.\"]}");
        respondWith(500, "{\"errors\": [\"boom\"]}");
        respondWith(404, "{\"errors\": [\"Not found\"]}");

        var reg = osv3().registration();
        Assert.assertFalse(reg.approveTask("t1").isSuccess());
        Assert.assertFalse(reg.submitToken("k1", Map.of()).isSuccess());
        Assert.assertThrows(RuntimeException.class, () -> reg.listUsers());
        takeRequest();
        takeRequest();
        takeRequest();
    }
}
