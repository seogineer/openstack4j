package org.openstack4j.openstack.optimization.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import com.fasterxml.jackson.annotation.JsonValue;
import org.openstack4j.api.optimization.OptimizationService;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.OSClientSession;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class OptimizationServiceImpl extends BaseOpenStackService implements OptimizationService {

    private static final String VERSION_HEADER = "OpenStack-API-Version";

    /** API versions chosen with {@link #useApiVersion} per session (client). */
    private static final Map<Object, String> VERSIONS = Collections.synchronizedMap(new WeakHashMap<>());

    public OptimizationServiceImpl() {
        // catalogs register http://host:9322 (or with /v1); paths carry /v1 themselves
        super(ServiceType.OPTIMIZATION, url -> url.replaceAll("/+$", "").replaceAll("/v1(/.*)?$", ""));
    }

    private static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }

    private static String id(String value) {
        Objects.requireNonNull(value, "id");
        if (value.isBlank() || value.indexOf('/') >= 0 || value.indexOf('?') >= 0 || value.indexOf('#') >= 0)
            throw new IllegalArgumentException("Not a valid identifier: '" + value + "'");
        return value;
    }

    @Override
    public void useApiVersion(String version) {
        if (version == null) {
            VERSIONS.remove(OSClientSession.getCurrent());
            return;
        }
        if (!version.equals("latest") && !version.matches("1\\.\\d+"))
            throw new IllegalArgumentException("Not an infra-optim API version: '" + version + "'");
        VERSIONS.put(OSClientSession.getCurrent(), version);
    }

    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        String version = VERSIONS.get(OSClientSession.getCurrent());
        return version == null ? invocation : invocation.header(VERSION_HEADER, "infra-optim " + version);
    }

    /** Sends at least {@code floor}: the session's version when it is newer, else {@code floor}. */
    private static <R> Invocation<R> at(Invocation<R> invocation, String floor) {
        String chosen = VERSIONS.get(OSClientSession.getCurrent());
        boolean newer = chosen != null && (chosen.equals("latest") || minor(chosen) >= minor(floor));
        return invocation.header(VERSION_HEADER, "infra-optim " + (newer ? chosen : floor));
    }

    private static int minor(String version) {
        return Integer.parseInt(version.substring(version.indexOf('.') + 1));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> strict(Invocation<Map> invocation) {
        Map<String, Object> body = invocation.execute(propagate404());
        return body == null ? new HashMap<>() : body;
    }

    private Map<String, Object> page(String path, Map<String, String> filters) {
        Invocation<Map> invocation = get(Map.class, path);
        if (filters != null)
            invocation.params(filters);
        return strict(invocation);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> show(String path) {
        return get(Map.class, path).execute();
    }

    private Map<String, Object> patched(String path, List<Map<String, Object>> patch) {
        return strict(patch(Map.class, path).entity(new ListBody(Objects.requireNonNull(patch, "patch"))));
    }

    @Override
    public Map<String, Object> versions() {
        return strict(get(Map.class, "/"));
    }

    @Override
    public Map<String, Object> version() {
        return strict(get(Map.class, "/v1"));
    }

    @Override
    public Map<String, Object> listAuditTemplates(Map<String, String> filters) {
        return page("/v1/audit_templates", filters);
    }

    @Override
    public Map<String, Object> listAuditTemplatesDetail(Map<String, String> filters) {
        return page("/v1/audit_templates/detail", filters);
    }

    @Override
    public Map<String, Object> getAuditTemplate(String auditTemplateIdent) {
        return show("/v1/audit_templates/" + id(auditTemplateIdent));
    }

    @Override
    public Map<String, Object> createAuditTemplate(Map<String, ?> auditTemplate) {
        Invocation<Map> invocation = post(Map.class, "/v1/audit_templates").entity(JsonBody.of(Objects.requireNonNull(auditTemplate, "auditTemplate")));
        if (auditTemplate.containsKey("default_parameters"))
            at(invocation, "1.7");
        return strict(invocation);
    }

    @Override
    public Map<String, Object> updateAuditTemplate(String auditTemplateIdent, List<Map<String, Object>> patch) {
        Objects.requireNonNull(patch, "patch");
        Invocation<Map> invocation = patch(Map.class, "/v1/audit_templates/" + id(auditTemplateIdent)).entity(new ListBody(patch));
        if (patch.stream().anyMatch(op -> "/default_parameters".equals(op.get("path"))))
            at(invocation, "1.7");
        return strict(invocation);
    }

    @Override
    public ActionResponse deleteAuditTemplate(String auditTemplateIdent) {
        return deleteWithResponse("/v1/audit_templates/" + id(auditTemplateIdent)).execute();
    }

    @Override
    public Map<String, Object> listAudits(Map<String, String> filters) {
        return page("/v1/audits", filters);
    }

    @Override
    public Map<String, Object> listAuditsDetail(Map<String, String> filters) {
        return page("/v1/audits/detail", filters);
    }

    @Override
    public Map<String, Object> getAudit(String auditIdent) {
        return show("/v1/audits/" + id(auditIdent));
    }

    @Override
    public Map<String, Object> createAudit(Map<String, ?> audit) {
        Objects.requireNonNull(audit, "audit");
        Invocation<Map> invocation = post(Map.class, "/v1/audits").entity(JsonBody.of(audit));
        if (audit.containsKey("force"))
            at(invocation, "1.2");
        else if (audit.containsKey("start_time") || audit.containsKey("end_time"))
            at(invocation, "1.1");
        return strict(invocation);
    }

    @Override
    public Map<String, Object> updateAudit(String auditIdent, List<Map<String, Object>> patch) {
        Objects.requireNonNull(patch, "patch");
        Invocation<Map> invocation = patch(Map.class, "/v1/audits/" + id(auditIdent)).entity(new ListBody(patch));
        if (patch.stream().map(op -> String.valueOf(op.get("path"))).anyMatch(p -> p.equals("/start_time") || p.equals("/end_time")))
            at(invocation, "1.1");
        return strict(invocation);
    }

    @Override
    public ActionResponse deleteAudit(String auditIdent) {
        return deleteWithResponse("/v1/audits/" + id(auditIdent)).execute();
    }

    @Override
    public Map<String, Object> listActionPlans(Map<String, String> filters) {
        return page("/v1/action_plans", filters);
    }

    @Override
    public Map<String, Object> listActionPlansDetail(Map<String, String> filters) {
        return page("/v1/action_plans/detail", filters);
    }

    @Override
    public Map<String, Object> getActionPlan(String actionPlanUuid) {
        return show("/v1/action_plans/" + id(actionPlanUuid));
    }

    @Override
    public Map<String, Object> updateActionPlan(String actionPlanUuid, List<Map<String, Object>> patch) {
        return patched("/v1/action_plans/" + id(actionPlanUuid), patch);
    }

    @Override
    public Map<String, Object> startActionPlan(String actionPlanUuid) {
        return strict(post(Map.class, "/v1/action_plans/" + id(actionPlanUuid) + "/start"));
    }

    @Override
    public ActionResponse deleteActionPlan(String actionPlanUuid) {
        return deleteWithResponse("/v1/action_plans/" + id(actionPlanUuid)).execute();
    }

    @Override
    public Map<String, Object> listActions(Map<String, String> filters) {
        return page("/v1/actions", filters);
    }

    @Override
    public Map<String, Object> listActionsDetail(Map<String, String> filters) {
        return page("/v1/actions/detail", filters);
    }

    @Override
    public Map<String, Object> getAction(String actionUuid) {
        return show("/v1/actions/" + id(actionUuid));
    }

    @Override
    public Map<String, Object> updateAction(String actionUuid, List<Map<String, Object>> patch) {
        return strict(at(patch(Map.class, "/v1/actions/" + id(actionUuid)), "1.5")
                .entity(new ListBody(Objects.requireNonNull(patch, "patch"))));
    }

    @Override
    public Map<String, Object> listGoals(Map<String, String> filters) {
        return page("/v1/goals", filters);
    }

    @Override
    public Map<String, Object> listGoalsDetail(Map<String, String> filters) {
        return page("/v1/goals/detail", filters);
    }

    @Override
    public Map<String, Object> getGoal(String goalIdent) {
        return show("/v1/goals/" + id(goalIdent));
    }

    @Override
    public Map<String, Object> listStrategies(Map<String, String> filters) {
        return page("/v1/strategies", filters);
    }

    @Override
    public Map<String, Object> listStrategiesDetail(Map<String, String> filters) {
        return page("/v1/strategies/detail", filters);
    }

    @Override
    public Map<String, Object> getStrategy(String strategyIdent) {
        return show("/v1/strategies/" + id(strategyIdent));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> getStrategyState(String strategyIdent) {
        List<Map<String, Object>> state = get(List.class, "/v1/strategies/" + id(strategyIdent) + "/state").execute(propagate404());
        return state == null ? Collections.emptyList() : state;
    }

    @Override
    public Map<String, Object> listScoringEngines(Map<String, String> filters) {
        return page("/v1/scoring_engines", filters);
    }

    @Override
    public Map<String, Object> listScoringEnginesDetail(Map<String, String> filters) {
        return page("/v1/scoring_engines/detail", filters);
    }

    @Override
    public Map<String, Object> getScoringEngine(String scoringEngineIdent) {
        return show("/v1/scoring_engines/" + id(scoringEngineIdent));
    }

    @Override
    public Map<String, Object> listServices(Map<String, String> filters) {
        return page("/v1/services", filters);
    }

    @Override
    public Map<String, Object> listServicesDetail(Map<String, String> filters) {
        return page("/v1/services/detail", filters);
    }

    @Override
    public Map<String, Object> getService(String serviceIdent) {
        return show("/v1/services/" + id(serviceIdent));
    }

    @Override
    public Map<String, Object> getDataModel(String dataModelType, String auditUuid) {
        Invocation<Map> invocation = at(get(Map.class, "/v1/data_model"), "1.3");
        if (dataModelType != null)
            invocation.param("data_model_type", dataModelType);
        if (auditUuid != null)
            invocation.param("audit_uuid", auditUuid);
        return strict(invocation);
    }

    @Override
    public ActionResponse triggerWebhook(String auditIdent) {
        // Watcher requires a JSON body (any object)
        return at(postWithResponse("/v1/webhooks/" + id(auditIdent)).entity(JsonBody.of(Map.of())), "1.4").execute();
    }

    /** A request body that is a JSON list. */
    static final class ListBody implements ModelEntity {
        private static final long serialVersionUID = 1L;
        private final List<?> items;

        ListBody(List<?> items) {
            this.items = items;
        }

        @JsonValue
        public List<?> items() {
            return items;
        }
    }
}
