package org.openstack4j.model.storage.block;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A backend volume Cinder could manage ({@code GET /manageable_volumes[/detail]}, 3.8+). */
public interface ManageableVolume extends ModelEntity {
    /** @return the driver reference, such as {@code source-name} */
    Map<String, String> getReference();
    Integer getSize();
    Boolean getSafeToManage();
    String getReasonNotSafe();
    /** @return the Cinder volume id when already managed */
    String getCinderId();
    Map<String, Object> getExtraInfo();
}
