package org.openstack4j.api.image.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.v2.ext.MetadefNamespace;
import org.openstack4j.model.image.v2.ext.MetadefResourceType;
import org.openstack4j.model.image.v2.ext.MetadefResourceTypeAssociation;
import org.openstack4j.model.image.v2.options.MetadefNamespaceOptions;

/**
 * Glance metadata definitions ({@code /v2/metadefs}): namespaces, resource types, objects, properties and tags.
 */
public interface MetadefService extends RestService {

    /**
     * Lists metadef namespaces, optionally filtered (resource_types, visibility).
     *
     * @return the result
     */
    List<? extends MetadefNamespace> listNamespaces();

    /**
     * Lists metadef namespaces, optionally filtered (resource_types, visibility).
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends MetadefNamespace> listNamespaces(Map<String, String> filters);

    /**
     * Returns a namespace with its properties, objects and resource type associations; null if missing.
     *
     * @param namespace the namespace
     * @return the result
     */
    MetadefNamespace getNamespace(String namespace);

    /**
     * Creates a namespace.
     *
     * @param options the options
     * @return the result
     */
    MetadefNamespace createNamespace(MetadefNamespaceOptions options);

    /**
     * Replaces a namespace (PUT, full body).
     *
     * @param namespace the namespace
     * @param options the options
     * @return the result
     */
    MetadefNamespace updateNamespace(String namespace, MetadefNamespaceOptions options);

    /**
     * @param namespace the namespace
     * @return the action response
     */
    ActionResponse deleteNamespace(String namespace);

    /**
     * Lists the resource types metadefs can apply to.
     *
     * @return the result
     */
    List<? extends MetadefResourceType> listResourceTypes();

    /**
     * Lists the resource types a namespace is associated with.
     *
     * @param namespace the namespace
     * @return the result
     */
    List<? extends MetadefResourceTypeAssociation> listResourceTypeAssociations(String namespace);

    /**
     * Associates a namespace with a resource type; prefix and propertiesTarget are optional.
     *
     * @param namespace the namespace
     * @param name the name
     * @param prefix the prefix
     * @param propertiesTarget the properties target
     * @return the result
     */
    MetadefResourceTypeAssociation associateResourceType(String namespace, String name, String prefix, String propertiesTarget);

    /**
     * Removes a resource type association.
     *
     * @param namespace the namespace
     * @param name the name
     * @return the action response
     */
    ActionResponse dissociateResourceType(String namespace, String name);
}
