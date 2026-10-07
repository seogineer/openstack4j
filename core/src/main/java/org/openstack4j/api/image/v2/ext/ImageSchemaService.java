package org.openstack4j.api.image.v2.ext;

import java.util.Map;

import org.openstack4j.common.RestService;

/**
 * Glance JSON schemas ({@code /v2/schemas}); each method returns the raw JSON schema.
 */
public interface ImageSchemaService extends RestService {

    /**
     * @return the result
     */
    Map<String, Object> image();

    /**
     * @return the result
     */
    Map<String, Object> images();

    /**
     * @return the result
     */
    Map<String, Object> member();

    /**
     * @return the result
     */
    Map<String, Object> members();

    /**
     * @return the result
     */
    Map<String, Object> task();

    /**
     * @return the result
     */
    Map<String, Object> tasks();

    /**
     * @return the result
     */
    Map<String, Object> metadefNamespace();

    /**
     * @return the result
     */
    Map<String, Object> metadefNamespaces();

    /**
     * @return the result
     */
    Map<String, Object> metadefObject();

    /**
     * @return the result
     */
    Map<String, Object> metadefObjects();

    /**
     * @return the result
     */
    Map<String, Object> metadefProperty();

    /**
     * @return the result
     */
    Map<String, Object> metadefProperties();

    /**
     * @return the result
     */
    Map<String, Object> metadefResourceType();

    /**
     * @return the result
     */
    Map<String, Object> metadefResourceTypes();

    /**
     * @return the result
     */
    Map<String, Object> metadefTag();

    /**
     * @return the result
     */
    Map<String, Object> metadefTags();
}
