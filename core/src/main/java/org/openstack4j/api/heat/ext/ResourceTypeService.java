package org.openstack4j.api.heat.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;

/**
 * Heat resource types ({@code /resource_types}).
 */
public interface ResourceTypeService extends RestService {

    /**
     * Lists resource type names, optionally filtered (name, version, support_status, with_description).
     *
     * @return the result
     */
    List<String> list();

    /**
     * Lists resource type names, optionally filtered (name, version, support_status, with_description).
     *
     * @param filters the filters
     * @return the result
     */
    List<String> list(Map<String, String> filters);

    /**
     * Returns the schema (properties, attributes) of a resource type.
     *
     * @param type the type
     * @return the result
     */
    Map<String, Object> schema(String type);

    /**
     * Returns a template that uses the resource type; templateType is hot or cfn (null: server default).
     *
     * @param type the type
     * @param templateType the template type
     * @return the result
     */
    Map<String, Object> template(String type, String templateType);
}
