package org.openstack4j.model.identity.v3;

import java.util.Date;

import org.openstack4j.model.ModelEntity;

/** A token revocation event (OS-REVOKE). */
public interface RevocationEvent extends ModelEntity {
    String getUserId();
    String getProjectId();
    String getDomainId();
    String getAuditId();
    String getAuditChainId();
    String getRoleId();
    String getTrustId();
    String getConsumerId();
    String getAccessTokenId();
    String getExpiresAt();
    Date getIssuedBefore();
    Date getRevokedAt();
}
