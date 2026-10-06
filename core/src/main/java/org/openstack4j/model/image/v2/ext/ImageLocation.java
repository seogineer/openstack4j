package org.openstack4j.model.image.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A location of image data in a store. */
public interface ImageLocation extends ModelEntity {
    String getUrl();
    Map<String, Object> getMetadata();
}
