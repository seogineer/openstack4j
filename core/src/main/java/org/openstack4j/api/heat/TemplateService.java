package org.openstack4j.api.heat;

import java.util.Map;

import org.openstack4j.model.heat.Template;
import org.openstack4j.model.heat.TemplateResponse;
import org.openstack4j.model.heat.TemplateValidateOptions;

/**
 * This Interface contains a non-exhaustive list of methods for the manipulation of Heat Templates
 *
 * @author Matthias Reisser
 */
public interface TemplateService {

    /**
     * Validates the template
     *
     * @param template to validate, passed as a {@link Template}
     * @return TemplateResponse indicating valid or the error condition if not valid
     */
    TemplateResponse validateTemplate(Template template);

    /**
     * Validates the template
     *
     * @param template to validate, passed as {@link String} in JSON Format
     * @return TemplateResponse indicating valid or the error condition if not valid
     */
    TemplateResponse validateTemplate(String template);

    /**
     * Validates the template
     *
     * @param templateURL the remote template via URL to validate
     * @return TemplateResponse indicating valid or the error condition if not valid
     */
    TemplateResponse validateTemplateByURL(String templateURL);

    /**
     * Validates a template and returns the server's answer (Description, Parameters, ParameterGroups, Environment ...).
     * Unlike {@link #validateTemplate(Template)}, an invalid template (400) raises a ResponseException carrying the
     * server's message.
     *
     * @param template the template to validate, with the optional environment, files and parameters
     * @param options show_nested / ignore_errors query options, may be null
     * @return the server's answer
     * @throws org.openstack4j.api.exceptions.ResponseException if the template is invalid or an error occurs
     */
    Map<String, Object> validate(Template template, TemplateValidateOptions options);

    /**
     * Retrieves the original template in original String form JSON or YAML
     *
     * @param stackName the stack name
     * @param stackId the stack identifier
     * @return the template
     * @throws ResponseException if an error occurs
     */
    String getTemplateAsString(String stackName, String stackId);

    /**
     * Retrieves the original template as Map<String,Object>
     *
     * @param stackName the stack name
     * @param stackId the stack identifier
     * @return the template
     * @throws ResponseException if an error occurs
     */
    Map<String, Object> getTemplateAsMap(String stackName, String stackId);

    /**
     * Retrieves the original template as Map<String,Object> when you know only the stack name or stack id
     *
     * @param stackNameOrId the stack name or stackId
     * @return the template
     * @throws ResponseException if an error occurs
     */
    Map<String, Object> getTemplateAsMap(String stackNameOrId);


}
