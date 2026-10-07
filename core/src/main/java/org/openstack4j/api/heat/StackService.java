package org.openstack4j.api.heat;

import org.openstack4j.model.heat.ext.StackSnapshot;
import org.openstack4j.model.heat.ext.StackOutput;
import java.util.List;
import java.util.Map;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.heat.AdoptStackData;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.heat.StackCreate;
import org.openstack4j.model.heat.StackUpdate;

/**
 * This interface defines all methods for the manipulation of stacks
 *
 * @author Matthias Reisser
 */
public interface StackService {
    /**
     * <code>POST /v1/{tenant_id}/stacks</code><br \>
     * <p>
     * Creates a new {@link Stack} out of a {@link StackCreate} object
     *
     * @param newStack {@link StackCreate} object out of which stack is to be created
     * @return new {@link Stack} as returned from the server
     */
    Stack create(StackCreate newStack);

    /**
     * Updates an existing Stack
     *
     * @param stackName the stack name
     * @param stackId the specific stack identifier
     * @param stackUpdate the stack update options
     * @return the action response
     */
    ActionResponse update(String stackName, String stackId, StackUpdate stackUpdate);

    /**
     * <code> POST /v1/{tenant_id}/stacks </code> <br\>
     * Creates a new {@link StackCreate} Object and returns a new {@link Stack} as sent from the
     * server.
     *
     * @param name Name of Stack
     * @param template Template in Json-Format or YAML format
     * @param parameters Map of parameters
     * @param disableRollback boolean to enable or disable rollback
     * @param timeOutMins timeout in minutes
     * @return new {@link Stack} as returned from the server
     */
    Stack create(String name, String template, Map<String, String> parameters,
            boolean disableRollback, Long timeOutMins);

    /**
     * returns details of a {@link Stack}.
     *
     * @param stackName Name of {@link Stack}
     * @return {@link Stack}
     */
    Stack getStackByName(String name);

    /**
     * Gets a list of currently existing {@link Stack}s.
     *
     * @return the list of {@link Stack}s
     */
    List<? extends Stack> list();

    /**
     * Gets a list of currently existing {@link Stack} objects, filtered by parameters.
     *
     * @param filteringParams The parameters used to filter the stacks returned.
     * @return the list of {@link Stack} objects.
     */
    public List<? extends Stack> list(Map<String, String> filteringParams);

    /**
     * returns details of a {@link Stack}.
     *
     * @param stackName Name of {@link Stack}
     * @param stackId Id of {@link Stack}
     */
    Stack getDetails(String stackName, String stackId);

    /**
     * Deletes the specified {@link Stack} from the server.
     *
     * @param stackName Name of {@link Stack}
     * @param stackId Id of {@link Stack}
     * @return the action response
     */
    ActionResponse delete(String stackName, String stackId);

    /**
     * Deletes a stack but leaves its resources intact, and returns data that describes the stack and its resources.
     *
     * @param stackName Name of {@link Stack}
     * @param stackId Id of {@link Stack}
     * @return <code>adopt_stack_data</code> element representing by {@link AdoptStackData}
     */
    AdoptStackData abandon(String stackName, String stackId);

    /**
     * Creates a stack from existing resources.
     *
     * @param adoptStackData Structure {@link AdoptStackData}, representing existing resources
     * @param parameters Map of parameters
     * @param disableRollback Enable or disable rollback
     * @param timeOutMins Timeout in minutes
     * @param template Template in Json-Format or YAML format. It is optional, used just in case there will be new resources (not included in adoptStackData)
     */
    Stack adopt(AdoptStackData adoptStackData, Map<String, String> parameters,
            boolean disableRollback, Long timeOutMins, String template);

    /**
     * Deletes a stack given only its name or id (looked up first, then deleted by name and id).
     *
     * @param stackIdentity the stack name or id
     * @return the action response; a failed response with code 404 when the stack does not exist
     */
    ActionResponse delete(String stackIdentity);

    /**
     * @param stackName the stack name
     * @param stackId   the stack id
     * @return the effective environment of the stack (parameters, parameter_defaults, resource_registry ...)
     */
    Map<String, Object> environment(String stackName, String stackId);

    /**
     * @param stackName the stack name
     * @param stackId   the stack id
     * @return the exported stack data, usable for adopt
     */
    Map<String, Object> export(String stackName, String stackId);

    /**
     * @param stackName the stack name
     * @param stackId   the stack id
     * @return the files of the stack, path to content
     */
    Map<String, String> files(String stackName, String stackId);

    /**
     * @param stackName the stack name
     * @param stackId   the stack id
     * @return the outputs of the stack (keys and descriptions, without values)
     */
    List<? extends StackOutput> outputs(String stackName, String stackId);

    /**
     * @param stackName the stack name
     * @param stackId   the stack id
     * @param outputKey the output key
     * @return the output with its value; null if missing
     */
    StackOutput output(String stackName, String stackId, String outputKey);

    /**
     * Updates a stack with PATCH: only the given template, parameters and files change, the rest are kept.
     *
     * @param stackName   the stack name
     * @param stackId     the stack id
     * @param stackUpdate the changes
     * @return the action response
     */
    ActionResponse patchUpdate(String stackName, String stackId, StackUpdate stackUpdate);

    /**
     * @param stackName the stack name
     * @param stackId   the stack id
     * @return the action response
     */
    ActionResponse suspend(String stackName, String stackId);

    /**
     * @param stackName the stack name
     * @param stackId   the stack id
     * @return the action response
     */
    ActionResponse resume(String stackName, String stackId);

    /**
     * Checks that the stack resources still exist and are healthy.
     *
     * @param stackName the stack name
     * @param stackId   the stack id
     * @return the action response
     */
    ActionResponse check(String stackName, String stackId);

    /**
     * Cancels an in-progress update and rolls it back.
     *
     * @param stackName the stack name
     * @param stackId   the stack id
     * @return the action response
     */
    ActionResponse cancelUpdate(String stackName, String stackId);

    /**
     * Cancels an in-progress create or update without rolling back.
     *
     * @param stackName the stack name
     * @param stackId   the stack id
     * @return the action response
     */
    ActionResponse cancelWithoutRollback(String stackName, String stackId);

    /**
     * @param stackName the stack name
     * @param stackId   the stack id
     * @return the snapshots of the stack
     */
    List<? extends StackSnapshot> snapshots(String stackName, String stackId);

    /**
     * Takes a snapshot of the stack (asynchronous; poll {@link #getSnapshot}).
     *
     * @param stackName    the stack name
     * @param stackId      the stack id
     * @param snapshotName the snapshot name; null lets Heat name it
     * @return the snapshot being created
     */
    StackSnapshot snapshot(String stackName, String stackId, String snapshotName);

    /**
     * @param stackName  the stack name
     * @param stackId    the stack id
     * @param snapshotId the snapshot
     * @return the snapshot with its data; null if missing
     */
    StackSnapshot getSnapshot(String stackName, String stackId, String snapshotId);

    /**
     * @param stackName  the stack name
     * @param stackId    the stack id
     * @param snapshotId the snapshot
     * @return the action response
     */
    ActionResponse deleteSnapshot(String stackName, String stackId, String snapshotId);

    /**
     * Restores the stack to a snapshot (asynchronous).
     *
     * @param stackName  the stack name
     * @param stackId    the stack id
     * @param snapshotId the snapshot
     * @return the action response
     */
    ActionResponse restoreSnapshot(String stackName, String stackId, String snapshotId);

    /**
     * Previews a stack create without creating anything.
     *
     * @param stackCreate the stack to preview
     * @return the previewed stack with its resources
     */
    Map<String, Object> preview(StackCreate stackCreate);

    /**
     * Previews a full (PUT) update.
     *
     * @param stackName   the stack name
     * @param stackId     the stack id
     * @param stackUpdate the update
     * @return the resource changes: unchanged, updated, replaced, added, deleted
     */
    Map<String, List<Map<String, Object>>> previewUpdate(String stackName, String stackId, StackUpdate stackUpdate);

    /**
     * Previews a PATCH update.
     *
     * @param stackName   the stack name
     * @param stackId     the stack id
     * @param stackUpdate the changes
     * @return the resource changes: unchanged, updated, replaced, added, deleted
     */
    Map<String, List<Map<String, Object>>> previewPatchUpdate(String stackName, String stackId, StackUpdate stackUpdate);
}
