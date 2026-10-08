package org.openstack4j.api.registration;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/**
 * Registration and project self-service (Adjutant v1): tasks, tokens and notifications (admin), and the project's users,
 * roles, sign-up, password reset, email update and quotas. Results are {@code Map}s, as their shapes depend on the
 * deployment's configured task types and actions. Most actions create a task that may need admin approval before a
 * token is emailed; the response's {@code notes} say what happened.
 */
public interface RegistrationService extends RestService {

    /** @return the API versions ({@code GET /}) */
    Map<String, Object> versions();

    /** @return the v1 version document */
    Map<String, Object> version();

    /** @return the service status: last created/completed task and error notifications (admin) */
    Map<String, Object> status();

    /**
     * Lists tasks (admin).
     *
     * @param params  e.g. {@code page}, {@code tasks_per_page}, or {@code null}
     * @param filters Django-style filters sent as the JSON {@code filters} parameter, e.g.
     *                {@code {"task_type": {"exact": "invite_user_to_project"}}}, or {@code null}
     * @return {@code tasks}, {@code pages}, {@code has_more}, {@code has_prev}
     */
    Map<String, Object> listTasks(Map<String, String> params, Map<String, ?> filters);

    /** @return the task, or {@code null} when it does not exist */
    Map<String, Object> getTask(String taskId);

    /** Replaces the task's action data with {@code data} (admin, for tasks not yet approved). */
    ActionResponse updateTask(String taskId, Map<String, ?> data);

    /** Approves the task (admin). */
    ActionResponse approveTask(String taskId);

    /** Cancels the task (admin). */
    ActionResponse cancelTask(String taskId);

    /** @param filters as for {@link #listTasks}, or {@code null} @return the tokens (admin) */
    List<Map<String, Object>> listTokens(Map<String, ?> filters);

    /** Issues a new token for an approved task (admin). */
    ActionResponse reissueToken(String taskId);

    /** Deletes the expired tokens (admin). */
    ActionResponse deleteExpiredTokens();

    /** @return the token's task type, actions and {@code required_fields}, or {@code null} when it does not exist */
    Map<String, Object> getToken(String token);

    /** Submits the token with its required fields (e.g. {@code password}). */
    ActionResponse submitToken(String token, Map<String, ?> fields);

    /** @param filters as for {@link #listTasks}, or {@code null} @return the notifications (admin) */
    List<Map<String, Object>> listNotifications(Map<String, ?> filters);

    /** Acknowledges the notifications with these ids (admin). */
    ActionResponse acknowledgeNotifications(List<String> notificationIds);

    /** @return the notification, or {@code null} when it does not exist */
    Map<String, Object> getNotification(String notificationId);

    /** Acknowledges the notification (admin). */
    ActionResponse acknowledgeNotification(String notificationId);

    /** @return the project's users, including pending invites */
    List<Map<String, Object>> listUsers();

    /**
     * Invites a user to the current project.
     *
     * @param roles          the roles to grant
     * @param inheritedRoles roles inherited by sub-projects, or {@code null}
     * @param extra          other fields such as {@code username}, or {@code null}
     */
    ActionResponse inviteUser(String email, List<String> roles, List<String> inheritedRoles, Map<String, ?> extra);

    /** @return the user, or {@code null} when it does not exist */
    Map<String, Object> getUser(String userId);

    /** Cancels a pending invite. */
    ActionResponse cancelInvite(String userId);

    /** @return {@code roles} and {@code inherited_roles} of the user on the current project */
    Map<String, Object> getUserRoles(String userId);

    /** @param inheritedRoles or {@code null} */
    ActionResponse addUserRoles(String userId, List<String> roles, List<String> inheritedRoles);

    /** @param inheritedRoles or {@code null} */
    ActionResponse removeUserRoles(String userId, List<String> roles, List<String> inheritedRoles);

    /** @return the roles the caller may grant on the current project */
    List<Map<String, Object>> listRoles();

    /**
     * Requests a password reset; a token is emailed when the user exists.
     *
     * @param email    the user's email
     * @param username the user's name where usernames are not emails, or {@code null}
     */
    ActionResponse resetPassword(String email, String username);

    /** Requests a change of the caller's email; a token is emailed to the new address. */
    ActionResponse updateEmail(String newEmail);

    /** @param extra other fields such as {@code username}, {@code setup_network}, or {@code null} */
    ActionResponse signUp(String email, String projectName, Map<String, ?> extra);

    /** @param regions or {@code null} for all @return {@code regions}, {@code quota_sizes}, {@code quota_size_order}, {@code active_quota_tasks} */
    Map<String, Object> getQuotas(List<String> regions);

    /** @param regions or {@code null} for all */
    ActionResponse updateQuotas(String size, List<String> regions);
}
