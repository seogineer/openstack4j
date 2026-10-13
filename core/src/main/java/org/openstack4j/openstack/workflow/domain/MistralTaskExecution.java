package org.openstack4j.openstack.workflow.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.openstack4j.util.ToStringHelper;
import org.openstack4j.model.workflow.TaskExecution;
import org.openstack4j.model.workflow.builder.TaskExecutionBuilder;
import org.openstack4j.openstack.common.JsonStringToMapDeserializer;
import org.openstack4j.openstack.common.ListResult;

/**
 * Mistral task execution.
 *
 * @author Renat Akhmerov
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MistralTaskExecution extends BaseExecution implements TaskExecution {

    private static final long serialVersionUID = 1L;

    private String name;

    private String type;

    @JsonProperty("workflow_id")
    private String workflowDefinitionId;

    @JsonProperty("workflow_execution_id")
    private String workflowExecutionId;

    @JsonDeserialize(using = JsonStringToMapDeserializer.class)
    private Map<String, Object> published;

    @JsonProperty("runtime_context")
    @JsonDeserialize(using = JsonStringToMapDeserializer.class)
    private Map<String, Object> runtimeContext;

    private Object result;

    private Boolean processed;

    private Boolean reset;

    @JsonProperty("env")
    @JsonDeserialize(using = JsonStringToMapDeserializer.class)
    private Map<String, Object> environment;


    public static MistralTaskExecutionBuilder builder() {
        return new MistralTaskExecutionBuilder();
    }

    @Override
    public MistralTaskExecutionBuilder toBuilder() {
        return new MistralTaskExecutionBuilder(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    @Override
    public String getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    @Override
    public Map<String, Object> getRuntimeContext() {
        return runtimeContext;
    }

    @Override
    public Object getResult() {
        return result;
    }

    @Override
    public Map<String, Object> getPublished() {
        return published;
    }

    @Override
    public Boolean isProcessed() {
        return processed;
    }

    @Override
    public Boolean isReset() {
        return reset;
    }

    @Override
    public Map<String, Object> getEnvironment() {
        return environment;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
                .add("createdAt", createdAt)
                .add("updatedAt", updatedAt)
                .add("id", id)
                .add("name", name)
                .toString();
    }

    /**
     * Mistral task execution builder.
     *
     * @author Renat Akhmerov
     */
    public static class MistralTaskExecutionBuilder extends
            BaseExecutionBuilder<MistralTaskExecutionBuilder, MistralTaskExecution>
            implements TaskExecutionBuilder<MistralTaskExecutionBuilder, MistralTaskExecution> {

        public MistralTaskExecutionBuilder() {
            this(new MistralTaskExecution());
        }

        public MistralTaskExecutionBuilder(MistralTaskExecution model) {
            super(model);
        }

        @Override
        public MistralTaskExecutionBuilder from(MistralTaskExecution in) {
            return null;
        }

        @Override
        public MistralTaskExecutionBuilder type(String type) {
            this.model.type = type;

            return this;
        }

        @Override
        public MistralTaskExecutionBuilder workflowDefinitionId(String workflowDefinitionId) {
            this.model.workflowDefinitionId = workflowDefinitionId;

            return this;
        }

        @Override
        public MistralTaskExecutionBuilder workflowExecutionId(String workflowExecutionId) {
            this.model.workflowExecutionId = workflowExecutionId;

            return this;
        }

        @Override
        public MistralTaskExecutionBuilder runtimeContext(Map<String, Object> runtimeContext) {
            this.model.runtimeContext = runtimeContext;

            return this;
        }

        @Override
        public MistralTaskExecutionBuilder result(Object result) {
            this.model.result = result;

            return this;
        }

        @Override
        public MistralTaskExecutionBuilder published(Map<String, Object> published) {
            this.model.published = published;

            return this;
        }

        @Override
        public MistralTaskExecutionBuilder processed(Boolean processed) {
            this.model.processed = processed;

            return this;
        }

        @Override
        public MistralTaskExecutionBuilder reset(Boolean reset) {
            this.model.reset = reset;

            return this;
        }

        @Override
        public MistralTaskExecutionBuilder environment(Map<String, Object> env) {
            this.model.environment = env;

            return this;
        }
    }

    public static class MistralTaskExecutions extends ListResult<MistralTaskExecution> {
        private static final long serialVersionUID = 1L;

        @JsonProperty("tasks")
        private List<MistralTaskExecution> list;

        @Override
        protected List<MistralTaskExecution> value() {
            return this.list;
        }
    }
}
