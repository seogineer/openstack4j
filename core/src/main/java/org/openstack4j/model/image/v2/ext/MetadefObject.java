package org.openstack4j.model.image.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A metadef object: a named group of properties. */
public interface MetadefObject extends ModelEntity {
    String getName();
    String getDescription();
    List<String> getRequired();
    Map<String, Object> getProperties();
    String getCreatedAt();
    String getUpdatedAt();
}
