package org.openstack4j.model.image.v2.ext;

import org.openstack4j.model.ModelEntity;

/** Usage of one image quota: limit and usage. */
public interface ImageUsage extends ModelEntity {
    Long getLimit();
    Long getUsage();
}
