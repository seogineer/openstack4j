package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A share type as the session-microversion APIs return it. Fields without a getter are in getAttributes(). */
public interface ShareTypeInfo extends ModelEntity {
    String getId();
    String getName();
    /** @return the description (2.41+) */
    String getDescription();
    /** @return {@code share_type_access:is_public} (2.7+; {@code os-share-type-access:is_public} below) */
    Boolean getIsPublic();
    /** @return whether this is the default share type (2.46+) */
    Boolean getIsDefault();
    Map<String, String> getExtraSpecs();
    Map<String, String> getRequiredExtraSpecs();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
