package org.openstack4j.model.image.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A metadef property: a JSON schema fragment describing one image/flavor/volume property. */
public interface MetadefProperty extends ModelEntity {
    String getName();
    String getTitle();
    String getDescription();
    String getType();
    /** @return the whole schema as returned (enum, minimum, maximum, items, default ...) */
    Map<String, Object> getSchema();
}
