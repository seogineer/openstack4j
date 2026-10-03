package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.RbacPolicy;
import org.openstack4j.model.network.options.RbacPolicyOptions;

/**
 * RBAC policies ({@code /v2.0/rbac-policies}, rbac-policies).
 */
public interface RbacPolicyService extends RestService {

    /**
     * Lists RBAC policies, optionally filtered (object_type, object_id, action, target_tenant).
     *
     * @return the result
     */
    List<? extends RbacPolicy> list();

    /**
     * Lists RBAC policies, optionally filtered (object_type, object_id, action, target_tenant).
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends RbacPolicy> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    RbacPolicy get(String id);

    /**
     * Shares an object with a project (or every project with target "*").
     *
     * @param options the options
     * @return the result
     */
    RbacPolicy create(RbacPolicyOptions options);

    /**
     * Changes the target project of a policy.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    RbacPolicy update(String id, RbacPolicyOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
