package org.openstack4j.openstack.image.v2.internal.ext;

import java.util.Collections;
import java.util.Map;

import org.openstack4j.api.image.v2.ext.ImageSchemaService;

public class ImageSchemaServiceImpl extends BaseImageExtService implements ImageSchemaService {

    @SuppressWarnings("unchecked")
    private Map<String, Object> schema(String name) {
        Map<String, Object> schema = showStrict(Map.class, "/schemas/" + name);
        return schema == null ? Collections.emptyMap() : schema;
    }

    @Override public Map<String, Object> image() { return schema("image"); }
    @Override public Map<String, Object> images() { return schema("images"); }
    @Override public Map<String, Object> member() { return schema("member"); }
    @Override public Map<String, Object> members() { return schema("members"); }
    @Override public Map<String, Object> task() { return schema("task"); }
    @Override public Map<String, Object> tasks() { return schema("tasks"); }
    @Override public Map<String, Object> metadefNamespace() { return schema("metadefs/namespace"); }
    @Override public Map<String, Object> metadefNamespaces() { return schema("metadefs/namespaces"); }
    @Override public Map<String, Object> metadefObject() { return schema("metadefs/object"); }
    @Override public Map<String, Object> metadefObjects() { return schema("metadefs/objects"); }
    @Override public Map<String, Object> metadefProperty() { return schema("metadefs/property"); }
    @Override public Map<String, Object> metadefProperties() { return schema("metadefs/properties"); }
    @Override public Map<String, Object> metadefResourceType() { return schema("metadefs/resource_type"); }
    @Override public Map<String, Object> metadefResourceTypes() { return schema("metadefs/resource_types"); }
    @Override public Map<String, Object> metadefTag() { return schema("metadefs/tag"); }
    @Override public Map<String, Object> metadefTags() { return schema("metadefs/tags"); }
}
