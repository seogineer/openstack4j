package org.openstack4j.model.image.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A metadata definition namespace. */
public interface MetadefNamespace extends ModelEntity {
    String getNamespace();
    String getDisplayName();
    String getDescription();
    String getVisibility();
    Boolean isProtected();
    String getOwner();
    String getCreatedAt();
    String getUpdatedAt();
    Map<String, Object> getProperties();
    List<Map<String, Object>> getObjects();
    List<Map<String, Object>> getResourceTypeAssociations();
    List<Map<String, Object>> getTags();
}
