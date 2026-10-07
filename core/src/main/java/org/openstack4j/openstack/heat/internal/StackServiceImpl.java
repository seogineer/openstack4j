package org.openstack4j.openstack.heat.internal;

import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.openstack.heat.domain.ext.HeatStackSnapshot.Snapshots;
import org.openstack4j.openstack.heat.domain.ext.HeatStackSnapshot;
import org.openstack4j.model.heat.ext.StackSnapshot;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.heat.internal.ext.BaseHeatExtService;
import org.openstack4j.openstack.heat.domain.ext.HeatStackOutput.Outputs;
import org.openstack4j.openstack.heat.domain.ext.HeatStackOutput;
import org.openstack4j.model.heat.ext.StackOutput;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.Builders;
import org.openstack4j.api.heat.StackService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.heat.AdoptStackData;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.heat.StackCreate;
import org.openstack4j.model.heat.StackUpdate;
import org.openstack4j.openstack.compute.functions.ToActionResponseFunction;
import org.openstack4j.openstack.heat.domain.HeatAdoptStackData;
import org.openstack4j.openstack.heat.domain.HeatStack;
import org.openstack4j.openstack.heat.domain.HeatStack.Stacks;
import org.openstack4j.openstack.heat.domain.HeatStackAdopt;

/**
 * This class implements all methods for manipulation of {@link HeatStack} objects. The
 * non-exhaustive list of methods is oriented along
 * http://developer.openstack.org/api-ref-orchestration-v1.html#stacks
 *
 * @author Matthias Reisser
 */
public class StackServiceImpl extends BaseHeatServices implements StackService {

    @Override
    public Stack create(StackCreate newStack) {
        Objects.requireNonNull(newStack);
        return post(HeatStack.class, uri("/stacks")).entity(newStack).execute();
    }

    @Override
    public Stack create(String name, String template,
            Map<String, String> parameters, boolean disableRollback,
            Long timeoutMins) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(template);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(timeoutMins);

        return create(Builders.stack().name(name).template(template)
                .parameters(parameters).timeoutMins(timeoutMins).build());
    }

    @Override
    public List<? extends Stack> list() {
        return get(Stacks.class, uri("/stacks")).execute().getList();
    }

    @Override
    public List<? extends Stack> list(Map<String, String> filteringParams) {
        Invocation<Stacks> req = get(Stacks.class, uri("/stacks"));
        if (filteringParams != null) {
            for (Map.Entry<String, String> entry : filteringParams.entrySet()) {
                req = req.param(entry.getKey(), entry.getValue());
            }
        }
        return req.execute().getList();
    }

    @Override
    public ActionResponse delete(String stackName, String stackId) {
        Objects.requireNonNull(stackId);
        return deleteWithResponse(uri("/stacks/%s/%s", stackName, stackId)).execute();
    }

    @Override
    public Stack getDetails(String stackName, String stackId) {
        Objects.requireNonNull(stackName);
        Objects.requireNonNull(stackId);
        return get(HeatStack.class, uri("/stacks/%s/%s", stackName, stackId)).execute();
    }

    @Override
    public ActionResponse update(String stackName, String stackId, StackUpdate stackUpdate) {
        Objects.requireNonNull(stackName);
        Objects.requireNonNull(stackId);
        Objects.requireNonNull(stackUpdate);

        return ToActionResponseFunction.INSTANCE
                .apply(put(Void.class, uri("/stacks/%s/%s", stackName, stackId))
                        .entity(stackUpdate)
                        .executeWithResponse());
    }

    @Override
    public Stack getStackByName(String stackName) {
        Objects.requireNonNull(stackName);
        return get(HeatStack.class, uri("/stacks/%s", stackName)).execute();
    }

    @Override
    public AdoptStackData abandon(String stackName, String stackId) {
        Objects.requireNonNull(stackId);
        return delete(HeatAdoptStackData.class, uri("/stacks/%s/%s/abandon", stackName, stackId)).execute();
    }

    @Override
    public Stack adopt(AdoptStackData adoptStackData, Map<String, String> parameters, boolean disableRollback, Long timeoutMins, String template) {
        Objects.requireNonNull(adoptStackData);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(timeoutMins);
        HeatStackAdopt heatStackAdopt = HeatStackAdopt.builder()
                .adoptStackData(adoptStackData)
                .template(template)
                .disableRollback(disableRollback)
                .name(adoptStackData.getName())
                .parameters(parameters)
                .timeoutMins(timeoutMins)
                .build();
        return post(HeatStack.class, uri("/stacks")).entity(heatStackAdopt).execute();
    }

    private static String stack(String stackName, String stackId) {
        return "/stacks/" + Objects.requireNonNull(stackName) + "/" + Objects.requireNonNull(stackId);
    }

    /**
     * {@code DELETE /stacks/{identity}} answers with a redirect that clients turn into a GET for DELETE, so the stack is
     * looked up first and deleted by name and id.
     */
    @Override
    public ActionResponse delete(String stackIdentity) {
        Stack found = getStackByName(Objects.requireNonNull(stackIdentity));
        if (found == null)
            return ActionResponse.actionFailed("Stack " + stackIdentity + " not found", 404);
        return delete(found.getName(), found.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> environment(String stackName, String stackId) {
        Map<String, Object> env = get(Map.class, stack(stackName, stackId) + "/environment").execute(BaseHeatExtService.propagate404());
        return env == null ? Collections.emptyMap() : env;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> export(String stackName, String stackId) {
        Map<String, Object> data = get(Map.class, stack(stackName, stackId) + "/export").execute(BaseHeatExtService.propagate404());
        return data == null ? Collections.emptyMap() : data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> files(String stackName, String stackId) {
        Map<String, String> files = get(Map.class, stack(stackName, stackId) + "/files").execute(BaseHeatExtService.propagate404());
        return files == null ? Collections.emptyMap() : files;
    }

    @Override
    public List<? extends StackOutput> outputs(String stackName, String stackId) {
        return get(Outputs.class, stack(stackName, stackId) + "/outputs").execute(BaseHeatExtService.propagate404()).getList();
    }

    @Override
    public StackOutput output(String stackName, String stackId, String outputKey) {
        return get(HeatStackOutput.class, stack(stackName, stackId) + "/outputs/" + Objects.requireNonNull(outputKey)).execute();
    }

    @Override
    public ActionResponse patchUpdate(String stackName, String stackId, StackUpdate stackUpdate) {
        return ToActionResponseFunction.INSTANCE.apply(patch(Void.class, stack(stackName, stackId)).entity(Objects.requireNonNull(stackUpdate)).executeWithResponse());
    }

    /** Stack actions take {@code {"<action>": null}}; JsonBody keeps the explicit null. */
    private ActionResponse action(String stackName, String stackId, String action) {
        return postWithResponse(stack(stackName, stackId) + "/actions").entity(JsonBody.of(Collections.singletonMap(action, null))).execute();
    }

    @Override public ActionResponse suspend(String stackName, String stackId) { return action(stackName, stackId, "suspend"); }
    @Override public ActionResponse resume(String stackName, String stackId) { return action(stackName, stackId, "resume"); }
    @Override public ActionResponse check(String stackName, String stackId) { return action(stackName, stackId, "check"); }
    @Override public ActionResponse cancelUpdate(String stackName, String stackId) { return action(stackName, stackId, "cancel_update"); }
    @Override public ActionResponse cancelWithoutRollback(String stackName, String stackId) { return action(stackName, stackId, "cancel_without_rollback"); }

    @Override
    public List<? extends StackSnapshot> snapshots(String stackName, String stackId) {
        return get(Snapshots.class, stack(stackName, stackId) + "/snapshots").execute(BaseHeatExtService.propagate404()).getList();
    }

    /** The create response is the snapshot without a root key (unlike show), so it is read as a Map and converted with the non-root mapper. */
    @Override
    public StackSnapshot snapshot(String stackName, String stackId, String snapshotName) {
        Map<String, Object> body = snapshotName == null ? Collections.emptyMap() : Collections.singletonMap("name", snapshotName);
        Map<?, ?> created = post(Map.class, stack(stackName, stackId) + "/snapshots").entity(JsonBody.of(body)).execute(BaseHeatExtService.propagate404());
        return created == null ? null : ObjectMapperSingleton.getContext(Map.class).convertValue(created, HeatStackSnapshot.class);
    }

    @Override
    public StackSnapshot getSnapshot(String stackName, String stackId, String snapshotId) {
        return get(HeatStackSnapshot.class, stack(stackName, stackId) + "/snapshots/" + Objects.requireNonNull(snapshotId)).execute();
    }

    @Override
    public ActionResponse deleteSnapshot(String stackName, String stackId, String snapshotId) {
        return deleteWithResponse(stack(stackName, stackId) + "/snapshots/" + Objects.requireNonNull(snapshotId)).execute();
    }

    @Override
    public ActionResponse restoreSnapshot(String stackName, String stackId, String snapshotId) {
        return postWithResponse(stack(stackName, stackId) + "/snapshots/" + Objects.requireNonNull(snapshotId) + "/restore").execute();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> preview(StackCreate stackCreate) {
        Map<String, Object> result = post(Map.class, "/stacks/preview").entity(Objects.requireNonNull(stackCreate)).execute(BaseHeatExtService.propagate404());
        Object stack = result == null ? null : result.get("stack");
        return stack instanceof Map ? (Map<String, Object>) stack : (result == null ? Collections.emptyMap() : result);
    }

    @Override
    public Map<String, List<Map<String, Object>>> previewUpdate(String stackName, String stackId, StackUpdate stackUpdate) {
        return changes(put(Map.class, stack(stackName, stackId) + "/preview").entity(Objects.requireNonNull(stackUpdate)).execute(BaseHeatExtService.propagate404()));
    }

    @Override
    public Map<String, List<Map<String, Object>>> previewPatchUpdate(String stackName, String stackId, StackUpdate stackUpdate) {
        return changes(patch(Map.class, stack(stackName, stackId) + "/preview").entity(Objects.requireNonNull(stackUpdate)).execute(BaseHeatExtService.propagate404()));
    }

    /** {@code {"resource_changes": {"unchanged": [...], "updated": [...], ...}}}; an unwrapped body is accepted too. */
    @SuppressWarnings("unchecked")
    private static Map<String, List<Map<String, Object>>> changes(Map<?, ?> result) {
        if (result == null)
            return Collections.emptyMap();
        Object inner = result.get("resource_changes");
        return (Map<String, List<Map<String, Object>>>) (inner instanceof Map ? inner : result);
    }
}
