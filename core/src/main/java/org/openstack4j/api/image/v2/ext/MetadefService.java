package org.openstack4j.api.image.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.v2.ext.MetadefNamespace;
import org.openstack4j.model.image.v2.ext.MetadefObject;
import org.openstack4j.model.image.v2.ext.MetadefProperty;
import org.openstack4j.model.image.v2.ext.MetadefResourceType;
import org.openstack4j.model.image.v2.ext.MetadefResourceTypeAssociation;
import org.openstack4j.model.image.v2.ext.MetadefTag;
import org.openstack4j.model.image.v2.options.MetadefNamespaceOptions;
import org.openstack4j.model.image.v2.options.MetadefObjectOptions;
import org.openstack4j.model.image.v2.options.MetadefPropertyOptions;

/**
 * Glance metadata definitions ({@code /v2/metadefs}): namespaces, resource types, objects, properties and tags.
 */
public interface MetadefService extends RestService {

    /**
     * Lists metadef namespaces, optionally filtered (resource_types, visibility). Glance pages results (25 by default);
     * pass {@code limit}/{@code marker} through the filters overload for more.
     *
     * @return the result
     */
    List<? extends MetadefNamespace> listNamespaces();

    /**
     * Lists metadef namespaces, optionally filtered (resource_types, visibility). Glance pages results (25 by default);
     * pass {@code limit}/{@code marker} through the filters overload for more.
     *
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

    /**
     * Lists the objects of a namespace.
     *
     * @param namespace the namespace
     * @return the result
     */
    List<? extends MetadefObject> listObjects(String namespace);

    /**
     * Returns an object; null if missing.
     *
     * @param namespace the namespace
     * @param name the name
     * @return the result
     */
    MetadefObject getObject(String namespace, String name);

    /**
     * Creates an object.
     *
     * @param namespace the namespace
     * @param options the options
     * @return the result
     */
    MetadefObject createObject(String namespace, MetadefObjectOptions options);

    /**
     * Replaces an object (PUT, full body).
     *
     * @param namespace the namespace
     * @param name the name
     * @param options the options
     * @return the result
     */
    MetadefObject updateObject(String namespace, String name, MetadefObjectOptions options);

    /**
     * @param namespace the namespace
     * @param name the name
     * @return the action response
     */
    ActionResponse deleteObject(String namespace, String name);

    /**
     * Lists the properties of a namespace as name to property.
     *
     * @param namespace the namespace
     * @return the result
     */
    Map<String, ? extends MetadefProperty> listProperties(String namespace);

    /**
     * Returns a property; null if missing.
     *
     * @param namespace the namespace
     * @param name the name
     * @return the result
     */
    MetadefProperty getProperty(String namespace, String name);

    /**
     * Creates a property.
     *
     * @param namespace the namespace
     * @param options the options
     * @return the result
     */
    MetadefProperty createProperty(String namespace, MetadefPropertyOptions options);

    /**
     * Replaces a property (PUT, full body).
     *
     * @param namespace the namespace
     * @param name the name
     * @param options the options
     * @return the result
     */
    MetadefProperty updateProperty(String namespace, String name, MetadefPropertyOptions options);

    /**
     * @param namespace the namespace
     * @param name the name
     * @return the action response
     */
    ActionResponse deleteProperty(String namespace, String name);

    /**
     * Lists the tags of a namespace.
     *
     * @param namespace the namespace
     * @return the result
     */
    List<? extends MetadefTag> listTags(String namespace);

    /**
     * Returns a tag; null if missing.
     *
     * @param namespace the namespace
     * @param name the name
     * @return the result
     */
    MetadefTag getTag(String namespace, String name);

    /**
     * Creates one tag.
     *
     * @param namespace the namespace
     * @param name the name
     * @return the result
     */
    MetadefTag createTag(String namespace, String name);

    /**
     * Creates tags in one request; with append=false the namespace's existing tags are replaced by this list (X-Openstack-Append).
     *
     * @param namespace the namespace
     * @param names the names
     * @param append the append
     * @return the result
     */
    List<? extends MetadefTag> createTags(String namespace, List<String> names, boolean append);

    /**
     * Renames a tag.
     *
     * @param namespace the namespace
     * @param name the name
     * @param newName the new name
     * @return the result
     */
    MetadefTag updateTag(String namespace, String name, String newName);

    /**
     * Deletes a tag.
     *
     * @param namespace the namespace
     * @param name the name
     * @return the action response
     */
    ActionResponse deleteTag(String namespace, String name);

    /**
     * Deletes every tag of the namespace.
     *
     * @param namespace the namespace
     * @return the action response
     */
    ActionResponse deleteAllTags(String namespace);
}
