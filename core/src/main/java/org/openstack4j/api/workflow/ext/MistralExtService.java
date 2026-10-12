package org.openstack4j.api.workflow.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/**
 * Mistral validation, code sources, dynamic actions, event triggers, sub-executions, execution reports and workflow
 * sharing. Responses are returned as {@code Map}s.
 */
public interface MistralExtService extends RestService {

    /** @param definition a workflow definition (YAML) @return {@code valid} and {@code error} */
    Map<String, Object> validateWorkflow(String definition);

    /** @param definition a workbook definition (YAML) @return {@code valid} and {@code error} */
    Map<String, Object> validateWorkbook(String definition);

    /** @param definition an ad-hoc action definition (YAML) @return {@code valid} and {@code error} */
    Map<String, Object> validateAction(String definition);

    /** @param filters e.g. {@code name}, {@code scope}, {@code namespace}, {@code limit}, {@code marker} */
    List<Map<String, Object>> listCodeSources(Map<String, String> filters);

    /** @return the code source (name or id), or {@code null} when it does not exist */
    Map<String, Object> getCodeSource(String identifier);

    /**
     * Uploads Python code that dynamic actions can use.
     *
     * @param scope {@code private} (default when {@code null}) or {@code public}
     */
    Map<String, Object> createCodeSource(String name, String content, String scope);

    /**
     * Replaces the code of a code source.
     *
     * @param scope {@code private} or {@code public}; {@code null} leaves it out of the request and Mistral then resets
     *              the scope to {@code private}, so pass the current scope to keep a public code source public
     */
    Map<String, Object> updateCodeSource(String identifier, String content, String scope);

    ActionResponse deleteCodeSource(String identifier);

    List<Map<String, Object>> listDynamicActions(Map<String, String> filters);

    /** @return the dynamic action (name or id), or {@code null} when it does not exist */
    Map<String, Object> getDynamicAction(String identifier);

    /** @param action {@code name}, {@code class_name}, {@code code_source_id} (or name), optional {@code scope}, {@code namespace} */
    Map<String, Object> createDynamicAction(Map<String, ?> action);

    /** @param action {@code id} or {@code name} plus the fields to change; Mistral also needs {@code class_name} on every update */
    Map<String, Object> updateDynamicAction(Map<String, ?> action);

    ActionResponse deleteDynamicAction(String identifier);

    List<Map<String, Object>> listEventTriggers(Map<String, String> filters);

    /** @return the event trigger, or {@code null} when it does not exist */
    Map<String, Object> getEventTrigger(String id);

    /** @param trigger {@code name}, {@code workflow_id}, {@code exchange}, {@code topic}, {@code event}, optional {@code workflow_input}, {@code workflow_params}, {@code scope} */
    Map<String, Object> createEventTrigger(Map<String, ?> trigger);

    /** @param fields e.g. {@code name}, {@code workflow_input}, {@code workflow_params} */
    Map<String, Object> updateEventTrigger(String id, Map<String, ?> fields);

    ActionResponse deleteEventTrigger(String id);

    /** @param filters e.g. {@code errors_only}, {@code max_depth} @return the sub-workflow executions of an execution */
    List<Map<String, Object>> listSubExecutions(String executionId, Map<String, String> filters);

    /** @param filters e.g. {@code errors_only}, {@code max_depth}, {@code statistics_only} @return the execution report */
    Map<String, Object> executionReport(String executionId, Map<String, String> filters);

    /** @return the workflow executions started by a task */
    List<Map<String, Object>> listTaskExecutions(String taskExecutionId);

    /**
     * @param workflowId the workflow UUID (not its name)
     * @return the projects a workflow is shared with ({@code member_id}, {@code status} …)
     */
    List<Map<String, Object>> listMembers(String workflowId);

    /**
     * @param workflowId the workflow UUID (not its name)
     * @return the membership, or {@code null} when it does not exist
     */
    Map<String, Object> getMember(String workflowId, String memberId);

    /**
     * Shares a workflow with a project.
     *
     * @param workflowId the workflow UUID (not its name)
     */
    Map<String, Object> addMember(String workflowId, String memberId);

    /**
     * @param workflowId the workflow UUID (not its name)
     * @param status {@code accepted} or {@code rejected} (by the member project)
     */
    Map<String, Object> updateMember(String workflowId, String memberId, String status);

    /**
     * Removes the membership.
     *
     * @param workflowId the workflow UUID (not its name)
     */
    ActionResponse removeMember(String workflowId, String memberId);
}
