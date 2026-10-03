package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.RbacPolicyService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.RbacPolicy;
import org.openstack4j.model.network.options.RbacPolicyOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronRbacPolicy;
import org.openstack4j.openstack.networking.domain.ext.NeutronRbacPolicy.RbacPolicies;

public class RbacPolicyServiceImpl extends BaseNeutronExtService implements RbacPolicyService {

    private static final String PATH = "/rbac-policies";
    private static final String ROOT = "rbac_policy";

    @Override public List<? extends RbacPolicy> list() { return listOf(RbacPolicies.class, PATH, null); }
    @Override public List<? extends RbacPolicy> list(Map<String, String> filters) { return listOf(RbacPolicies.class, PATH, filters); }
    @Override public RbacPolicy get(String id) { return show(NeutronRbacPolicy.class, PATH + "/" + id(id)); }
    @Override public RbacPolicy create(RbacPolicyOptions options) { return create(NeutronRbacPolicy.class, PATH, ROOT, options); }
    @Override public RbacPolicy update(String id, RbacPolicyOptions options) { return update(NeutronRbacPolicy.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
