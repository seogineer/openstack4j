package org.openstack4j.api.barbican.ext;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.barbican.ext.BarbicanAcl;
import org.openstack4j.model.common.ActionResponse;

/** Read ACLs of secrets and containers ({@code …/acl}). */
public interface BarbicanAclService extends RestService {

    /** @return the read ACL of a secret; a missing secret raises */
    BarbicanAcl getSecretAcl(String secretId);

    /**
     * Replaces the read ACL of a secret.
     *
     * @param users         the users allowed to read; {@code null} leaves the list out
     * @param projectAccess {@code false} limits reading to {@code users}; {@code null} leaves it out (the server
     *                      then uses its defaults: no users, project access {@code true}). At least one must be set.
     */
    ActionResponse setSecretAcl(String secretId, List<String> users, Boolean projectAccess);

    /** Changes only the given parts of the read ACL of a secret ({@code null} parts are left unchanged). */
    ActionResponse updateSecretAcl(String secretId, List<String> users, Boolean projectAccess);

    /** Removes the ACL of a secret (back to project-wide access). */
    ActionResponse deleteSecretAcl(String secretId);

    /** @return the read ACL of a container; a missing container raises */
    BarbicanAcl getContainerAcl(String containerId);

    ActionResponse setContainerAcl(String containerId, List<String> users, Boolean projectAccess);

    ActionResponse updateContainerAcl(String containerId, List<String> users, Boolean projectAccess);

    ActionResponse deleteContainerAcl(String containerId);
}
