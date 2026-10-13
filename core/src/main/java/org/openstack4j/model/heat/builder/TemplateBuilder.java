package org.openstack4j.model.heat.builder;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.Buildable;
import org.openstack4j.model.heat.Template;


/**
 * This interface describes a builder for {@link Template} objects
 *
 * @author Matthias Reisser
 */
public interface TemplateBuilder extends Buildable.Builder<TemplateBuilder, Template> {

    /**
     * Sets the template in JSON format. This value takes precedence over the template URL if both are supplied.
     *
     * @param template template in JSON format
     * @return modified TemplateBuilder
     */
    TemplateBuilder templateJson(String template);


    /**
     * The URL of the template to instantiate. This value is ignored if the template is supplied inline
     *
     * @param templateURL the template URL
     * @return TemplateBuilder
     */
    TemplateBuilder templateURL(String templateURL);

    /**
     * The environment as YAML or JSON text (sent as a string, like StackCreateBuilder.environment). The server ignores
     * it when {@link #environmentFiles(List)} is set.
     *
     * @param environment the environment text
     * @return TemplateBuilder
     */
    TemplateBuilder environment(String environment);

    /**
     * Names of environment files in {@link #files(Map)} to merge, in order.
     *
     * @param environmentFiles the environment file names
     * @return TemplateBuilder
     */
    TemplateBuilder environmentFiles(List<String> environmentFiles);

    /**
     * Files referenced by the template or the environment (name to content).
     *
     * @param files the files
     * @return TemplateBuilder
     */
    TemplateBuilder files(Map<String, String> files);

    /**
     * Template parameters; the server merges them into the environment's parameters.
     *
     * @param parameters the parameters
     * @return TemplateBuilder
     */
    TemplateBuilder parameters(Map<String, String> parameters);
}
