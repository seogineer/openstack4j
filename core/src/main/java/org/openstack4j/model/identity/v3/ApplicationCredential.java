package org.openstack4j.model.identity.v3;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An application credential: a secret bound to a user, a project and a set of roles. */
public interface ApplicationCredential extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getUserId();
    String getProjectId();
    Map<String, Object> getSystem();
    Date getExpiresAt();
    /** @return whether the credential may create other credentials and trusts */
    Boolean getUnrestricted();
    List<? extends Role> getRoles();
    List<? extends AccessRule> getAccessRules();
    /** @return the secret; only present in the create response */
    String getSecret();
}
