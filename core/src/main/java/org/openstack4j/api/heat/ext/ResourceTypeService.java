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
     * @return the resource type names
     */
    List<String> list();

    /**
     * Lists resource type names, optionally filtered (name, version, support_status, with_description).
     *
     * @param filters the filters
     * @return the resource type names
     */
    List<String> list(Map<String, String> filters);

    /**
     * Returns the schema (properties, attributes) of a resource type.
     *
     * @throws org.openstack4j.api.exceptions.ResponseException if the resource type does not exist (404)
     * @param type the type
     * @return the schema
     */
    Map<String, Object> schema(String type);

    /**
     * Returns a template that uses the resource type; templateType is hot or cfn (null: server default).
     *
     * @throws org.openstack4j.api.exceptions.ResponseException if the resource type does not exist (404)
     * @param type the type
     * @param templateType the template type
     * @return the template
     */
    Map<String, Object> template(String type, String templateType);
}
