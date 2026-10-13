package org.openstack4j.openstack.workflow.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.openstack4j.model.workflow.CronTrigger;
import org.openstack4j.model.workflow.Scope;
import org.openstack4j.model.workflow.builder.CronTriggerBuilder;
import org.openstack4j.openstack.common.JsonStringToMapDeserializer;
import org.openstack4j.openstack.common.ListResult;

/**
 * Mistral cron trigger.
 *
 * @author Renat Akhmerov
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MistralCronTrigger implements CronTrigger {
    private static final long serialVersionUID = 1L;

    private String id;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    private Scope scope;

    private String name;

    private String pattern;

    @JsonProperty("workflow_name")
    private String workflowName;

    @JsonProperty("workflow_id")
    private String workflowId;

    @JsonProperty("workflow_input")
    @JsonDeserialize(using = JsonStringToMapDeserializer.class)
    private Map<String, Object> workflowInput;

    @JsonProperty("workflow_params")
    @JsonDeserialize(using = JsonStringToMapDeserializer.class)
    private Map<String, Object> workflowParameters;

    @JsonProperty("remaining_executions")
    private Integer remainingExecutionsCount;

    @JsonProperty("first_execution_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstExecutionTime;

    @JsonProperty("next_execution_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextExecutionTime;

    public static MistralCronTriggerBuilder builder() {
        return new MistralCronTriggerBuilder();
    }

    @Override
    public MistralCronTriggerBuilder toBuilder() {
        return new MistralCronTriggerBuilder(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public Date getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public String getWorkflowName() {
        return workflowName;
    }

    @Override
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public Map<String, ?> getWorkflowInput() {
        return workflowInput;
    }

    @Override
    public Map<String, ?> getWorkflowParameters() {
        return workflowParameters;
    }

    @Override
    public Integer getRemainingExecutionsCount() {
        return remainingExecutionsCount;
    }

    @Override
    public Date getFirstExecutionTime() {
        return firstExecutionTime;
    }

    @Override
    public Date getNextExecutionTime() {
        return nextExecutionTime;
    }

    /**
     * Mistral cron trigger builder.
     *
     * @author Renat Akhmerov
     */
    public static class MistralCronTriggerBuilder implements
            CronTriggerBuilder<MistralCronTriggerBuilder, MistralCronTrigger> {

        protected MistralCronTrigger model;

        MistralCronTriggerBuilder() {
            this(new MistralCronTrigger());
        }

        MistralCronTriggerBuilder(MistralCronTrigger model) {
            this.model = model;
        }

        @Override
        public MistralCronTrigger build() {
            return model;
        }

        @Override
        public MistralCronTriggerBuilder from(MistralCronTrigger in) {
            return null;
        }

        @Override
        public MistralCronTriggerBuilder id(String id) {
            this.model.id = id;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder createdAt(Date createdAt) {
            this.model.createdAt = createdAt;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder updatedAt(Date updatedAt) {
            this.model.updatedAt = updatedAt;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder scope(Scope scope) {
            this.model.scope = scope;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder name(String name) {
            this.model.name = name;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder pattern(String pattern) {
            this.model.pattern = pattern;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder workflowName(String workflowName) {
            this.model.workflowName = workflowName;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder workflowId(String workflowId) {
            this.model.workflowId = workflowId;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder workflowInput(Map<String, Object> workflowInput) {
            this.model.workflowInput = workflowInput;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder workflowParameters(Map<String, Object> workflowParameters) {
            this.model.workflowParameters = workflowParameters;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder remainingExecutionsCount(Integer cnt) {
            this.model.remainingExecutionsCount = cnt;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder firstExecutionTime(Date firstExecutionTime) {
            this.model.firstExecutionTime = firstExecutionTime;

            return this;
        }

        @Override
        public MistralCronTriggerBuilder nextExecutionTime(Date nextExecutionTime) {
            this.model.nextExecutionTime = nextExecutionTime;

            return this;
        }
    }

    public static class MistralCronTriggers extends ListResult<MistralCronTrigger> {
        private static final long serialVersionUID = 1L;

        @JsonProperty("cron_triggers")
        private List<MistralCronTrigger> list;

        @Override
        protected List<MistralCronTrigger> value() {
            return this.list;
        }
    }
}
