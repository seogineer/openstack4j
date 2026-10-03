package org.openstack4j.model.identity.v3;

import java.util.Date;
import java.util.List;

import org.openstack4j.model.ModelEntity;

/** A trust (OS-TRUST): the trustor delegates roles on a project to the trustee. */
public interface Trust extends ModelEntity {
    String getId();
    String getTrustorUserId();
    String getTrusteeUserId();
    String getProjectId();
    Boolean getImpersonation();
    Date getExpiresAt();
    Integer getRemainingUses();
    Integer getRedelegationCount();
    String getRedelegatedTrustId();
    List<? extends Role> getRoles();
}
