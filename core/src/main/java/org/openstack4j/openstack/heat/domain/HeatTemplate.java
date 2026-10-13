package org.openstack4j.openstack.heat.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import org.openstack4j.model.heat.Template;
import org.openstack4j.model.heat.builder.TemplateBuilder;

/**
 * This class represents a HeatTemplate. It uses jackson for (de)serialization
 * of contents.
 *
 * @author Matthias Reisser
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeatTemplate implements Template {
    private static final long serialVersionUID = 4056106815634314225L;

    @JsonRawValue
    @JsonProperty("template")
    private String templateJson;

    @JsonProperty("template_url")
    private String templateURL;

    /** YAML or JSON text, so not a raw value. */
    @JsonProperty("environment")
    private String environment;

    @JsonProperty("environment_files")
    private List<String> environmentFiles;

    @JsonProperty("files")
    private Map<String, String> files;

    @JsonProperty("parameters")
    private Map<String, String> parameters;

    /**
     * returns a {@link HeatTemplateConcreteBuilder} for configuration and
     * creation of a {@link HeatTemplate}
     *
     * @return a {@link HeatTemplateConcreteBuilder}
     */
    public static HeatTemplateConcreteBuilder build() {
        return new HeatTemplateConcreteBuilder();
    }

    @Override
    public String getTemplateJson() {
        return templateJson;
    }

    @Override
    public String getTemplateURL() {
        return templateURL;
    }

    @Override
    public String getEnvironment() {
        return environment;
    }

    @Override
    public List<String> getEnvironmentFiles() {
        return environmentFiles;
    }

    @Override
    public Map<String, String> getFiles() {
        return files;
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public TemplateBuilder toBuilder() {
        return new HeatTemplateConcreteBuilder(this);
    }

    /**
     * builder class for configuration and creation of {@link HeatTemplate}
     * objects. Use {@link #build()} to create the {@link HeatTemplate} object.
     *
     * @author Matthias Reisser
     */
    public static class HeatTemplateConcreteBuilder implements TemplateBuilder {
        HeatTemplate model;

        /**
         * Constructor to create a {@link HeatTemplateConcreteBuilder} object
         * with a new, empty {@link HeatTemplate} object.
         */
        public HeatTemplateConcreteBuilder() {
            this(new HeatTemplate());
        }

        /**
         * Constructor to create a {@link HeatTemplateConcreteBuilder} object
         * for an existing {@link HeatTemplate} object.
         *
         * @param template existing {@link HeatTemplate} object.
         */
        public HeatTemplateConcreteBuilder(HeatTemplate template) {
            this.model = template;
        }

        @Override
        public Template build() {
            return model;
        }

        @Override
        public TemplateBuilder from(Template in) {
            model = (HeatTemplate) in;
            return this;
        }

        @Override
        public TemplateBuilder templateJson(String template) {
            model.templateJson = template;
            return this;
        }

        @Override
        public TemplateBuilder templateURL(String templateURL) {
            model.templateURL = templateURL;
            return this;
        }

        @Override
        public TemplateBuilder environment(String environment) {
            model.environment = environment;
            return this;
        }

        @Override
        public TemplateBuilder environmentFiles(List<String> environmentFiles) {
            model.environmentFiles = environmentFiles;
            return this;
        }

        @Override
        public TemplateBuilder files(Map<String, String> files) {
            model.files = files;
            return this;
        }

        @Override
        public TemplateBuilder parameters(Map<String, String> parameters) {
            model.parameters = parameters;
            return this;
        }
    }
}
