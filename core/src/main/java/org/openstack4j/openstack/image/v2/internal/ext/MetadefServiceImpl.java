package org.openstack4j.openstack.image.v2.internal.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.image.v2.ext.MetadefService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.v2.ext.MetadefNamespace;
import org.openstack4j.model.image.v2.ext.MetadefResourceType;
import org.openstack4j.model.image.v2.ext.MetadefResourceTypeAssociation;
import org.openstack4j.model.image.v2.options.MetadefNamespaceOptions;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceMetadefNamespace;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceMetadefNamespace.Namespaces;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceMetadefResourceType.ResourceTypes;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceMetadefResourceTypeAssociation;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceMetadefResourceTypeAssociation.Associations;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class MetadefServiceImpl extends BaseImageExtService implements MetadefService {

    private static final String NS = "/metadefs/namespaces";

    protected static String ns(String namespace) {
        return NS + "/" + id(namespace);
    }

    @Override public List<? extends MetadefNamespace> listNamespaces() { return listOf(Namespaces.class, NS, null); }
    @Override public List<? extends MetadefNamespace> listNamespaces(Map<String, String> filters) { return listOf(Namespaces.class, NS, filters); }
    @Override public MetadefNamespace getNamespace(String namespace) { return show(GlanceMetadefNamespace.class, ns(namespace)); }
    @Override public MetadefNamespace createNamespace(MetadefNamespaceOptions options) { return create(GlanceMetadefNamespace.class, NS, options); }
    @Override public MetadefNamespace updateNamespace(String namespace, MetadefNamespaceOptions options) { return replace(GlanceMetadefNamespace.class, ns(namespace), options); }
    @Override public ActionResponse deleteNamespace(String namespace) { return remove(ns(namespace)); }

    @Override public List<? extends MetadefResourceType> listResourceTypes() { return listOf(ResourceTypes.class, "/metadefs/resource_types", null); }
    @Override public List<? extends MetadefResourceTypeAssociation> listResourceTypeAssociations(String namespace) { return listOf(Associations.class, ns(namespace) + "/resource_types", null); }

    @Override
    public MetadefResourceTypeAssociation associateResourceType(String namespace, String name, String prefix, String propertiesTarget) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", id(name));
        if (prefix != null) body.put("prefix", prefix);
        if (propertiesTarget != null) body.put("properties_target", propertiesTarget);
        return post(GlanceMetadefResourceTypeAssociation.class, ns(namespace) + "/resource_types").entity(JsonBody.of(body)).execute(propagate404());
    }

    @Override public ActionResponse dissociateResourceType(String namespace, String name) { return remove(ns(namespace) + "/resource_types/" + id(name)); }
}
