package org.openstack4j.api.optimization;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/**
 * Resource optimization (Watcher v1): audit templates, audits, action plans, actions, goals, strategies, scoring engines,
 * services, the data model and webhooks. Results are {@code Map}s. A list returns its page: the items under the
 * resource's key (e.g. {@code audits}) and {@code next}, the URL of the following page, if any.
 * <p>
 * Methods that need a newer API send {@code OpenStack-API-Version: infra-optim <version>} themselves (audit
 * {@code start_time}/{@code end_time} 1.1, audit {@code force} 1.2, data model 1.3, webhooks 1.4, action updates 1.5,
 * audit template {@code default_parameters} 1.7);
 * the rest send no version header, so Watcher answers at 1.0 and leaves out the fields added later (audit
 * {@code start_time}, {@code end_time}, {@code force}, {@code status_message}, template {@code default_parameters},
 * action and action plan {@code status_message}). Call {@link #useApiVersion} to send a version on every request.
 */
public interface OptimizationService extends RestService {

    /**
     * Sends {@code OpenStack-API-Version: infra-optim <version>} on the current client's requests (a method needing a
     * newer version still sends its own).
     *
     * @param version e.g. {@code 1.7} or {@code latest}, or {@code null} to send none again
     */
    void useApiVersion(String version);

    /** @return the API versions ({@code GET /}) */
    Map<String, Object> versions();

    /** @return the v1 document with its media types and links */
    Map<String, Object> version();

    /** @param filters e.g. {@code goal}, {@code strategy}, {@code limit}, {@code marker}, {@code sort_key}, or {@code null} */
    Map<String, Object> listAuditTemplates(Map<String, String> filters);

    Map<String, Object> listAuditTemplatesDetail(Map<String, String> filters);

    /** @return the audit template (by UUID or name), or {@code null} when it does not exist */
    Map<String, Object> getAuditTemplate(String auditTemplateIdent);

    /** @param auditTemplate {@code name}, {@code goal}, optional {@code strategy}, {@code description}, {@code scope}, {@code default_parameters} (1.7, needs a strategy) */
    Map<String, Object> createAuditTemplate(Map<String, ?> auditTemplate);

    /** @param patch JSON Patch operations, e.g. {@code {"op": "replace", "path": "/name", "value": "x"}} */
    Map<String, Object> updateAuditTemplate(String auditTemplateIdent, List<Map<String, Object>> patch);

    ActionResponse deleteAuditTemplate(String auditTemplateIdent);

    /** @param filters e.g. {@code audit_template}, {@code goal}, {@code strategy}, {@code limit}, {@code marker}, or {@code null} */
    Map<String, Object> listAudits(Map<String, String> filters);

    Map<String, Object> listAuditsDetail(Map<String, String> filters);

    /** @return the audit (by UUID or name), or {@code null} when it does not exist */
    Map<String, Object> getAudit(String auditIdent);

    /**
     * Creates an audit.
     *
     * @param audit e.g. {@code audit_template_uuid} or {@code goal}, {@code audit_type} ({@code ONESHOT}, {@code CONTINUOUS},
     *              {@code EVENT}), {@code parameters}, {@code interval}, {@code name}, {@code auto_trigger},
     *              {@code start_time}/{@code end_time} (1.1), {@code force} (1.2)
     */
    Map<String, Object> createAudit(Map<String, ?> audit);

    /** @param patch JSON Patch operations, e.g. replacing {@code /state} with {@code CANCELLED} or {@code SUSPENDED} */
    Map<String, Object> updateAudit(String auditIdent, List<Map<String, Object>> patch);

    ActionResponse deleteAudit(String auditIdent);

    /** @param filters e.g. {@code audit_uuid}, {@code strategy}, {@code limit}, {@code marker}, or {@code null} */
    Map<String, Object> listActionPlans(Map<String, String> filters);

    Map<String, Object> listActionPlansDetail(Map<String, String> filters);

    /** @return the action plan, or {@code null} when it does not exist */
    Map<String, Object> getActionPlan(String actionPlanUuid);

    /** @param patch JSON Patch operations, e.g. replacing {@code /state} with {@code PENDING} or {@code CANCELLED} */
    Map<String, Object> updateActionPlan(String actionPlanUuid, List<Map<String, Object>> patch);

    /** Starts (triggers) a recommended action plan. @return the action plan */
    Map<String, Object> startActionPlan(String actionPlanUuid);

    ActionResponse deleteActionPlan(String actionPlanUuid);

    /** @param filters e.g. {@code action_plan_uuid}, {@code audit_uuid}, {@code limit}, {@code marker}, or {@code null} */
    Map<String, Object> listActions(Map<String, String> filters);

    Map<String, Object> listActionsDetail(Map<String, String> filters);

    /** @return the action, or {@code null} when it does not exist */
    Map<String, Object> getAction(String actionUuid);

    /**
     * Updates an action (infra-optim 1.5): only {@code PENDING} to {@code SKIPPED}, optionally with a {@code status_message},
     * while its action plan is {@code RECOMMENDED} or {@code PENDING}.
     */
    Map<String, Object> updateAction(String actionUuid, List<Map<String, Object>> patch);

    Map<String, Object> listGoals(Map<String, String> filters);

    Map<String, Object> listGoalsDetail(Map<String, String> filters);

    /** @return the goal (by UUID or name), or {@code null} when it does not exist */
    Map<String, Object> getGoal(String goalIdent);

    /** @param filters e.g. {@code goal}, {@code limit}, {@code marker}, or {@code null} */
    Map<String, Object> listStrategies(Map<String, String> filters);

    Map<String, Object> listStrategiesDetail(Map<String, String> filters);

    /** @return the strategy (by UUID or name), or {@code null} when it does not exist */
    Map<String, Object> getStrategy(String strategyIdent);

    /** @return the state of the strategy's requirements (datasource, metrics, CDM), one entry per requirement */
    List<Map<String, Object>> getStrategyState(String strategyIdent);

    Map<String, Object> listScoringEngines(Map<String, String> filters);

    Map<String, Object> listScoringEnginesDetail(Map<String, String> filters);

    /** @return the scoring engine (by UUID or name), or {@code null} when it does not exist */
    Map<String, Object> getScoringEngine(String scoringEngineIdent);

    Map<String, Object> listServices(Map<String, String> filters);

    Map<String, Object> listServicesDetail(Map<String, String> filters);

    /** @return the Watcher service (by id or name), or {@code null} when it does not exist */
    Map<String, Object> getService(String serviceIdent);

    /**
     * Returns the compute data model (infra-optim 1.3; {@code server_pinned_az} and {@code server_flavor_extra_specs}
     * need 1.6 and are not returned).
     *
     * @param dataModelType {@code compute}, or {@code null} for the default
     * @param auditUuid     limits the model to the audit's scope, or {@code null}
     * @return {@code context}: the model's elements
     */
    Map<String, Object> getDataModel(String dataModelType, String auditUuid);

    /** Triggers an {@code EVENT} audit in state {@code PENDING} or {@code SUCCEEDED} (infra-optim 1.4; 202). */
    ActionResponse triggerWebhook(String auditIdent);
}
