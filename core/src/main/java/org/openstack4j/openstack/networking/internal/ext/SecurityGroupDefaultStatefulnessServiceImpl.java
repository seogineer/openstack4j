package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.SecurityGroupDefaultStatefulnessService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.SecurityGroupDefaultStatefulness;
import org.openstack4j.model.network.options.SecurityGroupDefaultStatefulnessOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronSecurityGroupDefaultStatefulness;
import org.openstack4j.openstack.networking.domain.ext.NeutronSecurityGroupDefaultStatefulness.Items;

public class SecurityGroupDefaultStatefulnessServiceImpl extends BaseNeutronExtService implements SecurityGroupDefaultStatefulnessService {

    private static final String PATH = "/security-groups-default-statefulness";
    private static final String ROOT = "security_groups_default_statefulness";

    @Override public List<? extends SecurityGroupDefaultStatefulness> list() { return listOf(Items.class, PATH, null); }
    @Override public List<? extends SecurityGroupDefaultStatefulness> list(Map<String, String> filters) { return listOf(Items.class, PATH, filters); }
    @Override public SecurityGroupDefaultStatefulness get(String id) { return show(NeutronSecurityGroupDefaultStatefulness.class, PATH + "/" + id(id)); }
    @Override public SecurityGroupDefaultStatefulness create(SecurityGroupDefaultStatefulnessOptions options) { return create(NeutronSecurityGroupDefaultStatefulness.class, PATH, ROOT, options); }
    @Override public SecurityGroupDefaultStatefulness update(String id, SecurityGroupDefaultStatefulnessOptions options) { return update(NeutronSecurityGroupDefaultStatefulness.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
