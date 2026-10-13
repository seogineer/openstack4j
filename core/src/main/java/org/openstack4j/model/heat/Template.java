package org.openstack4j.model.heat;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.Buildable;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.heat.builder.TemplateBuilder;

/**
 * This interface describes a template object.
 *
 * @author Matthias Reisser
 */
public interface Template extends ModelEntity, Buildable<TemplateBuilder> {

    /**
     * Returns the JSON-representation of the template
     *
     * @return the JSON formatted template
     */
    String getTemplateJson();

    /**
     * The URL of the template to instantiate. This value is ignored if the template is supplied inline.
     *
     * @return the template URL
     */
    String getTemplateURL();

    /** @return the environment as YAML or JSON text, or null */
    default String getEnvironment() {
        return null;
    }

    /** @return the names of the environment files (keys of {@link #getFiles()}) to merge, or null */
    default List<String> getEnvironmentFiles() {
        return null;
    }

    /** @return the files referenced by the template or the environment (name to content), or null */
    default Map<String, String> getFiles() {
        return null;
    }

    /** @return the template parameters, or null */
    default Map<String, String> getParameters() {
        return null;
    }

}
