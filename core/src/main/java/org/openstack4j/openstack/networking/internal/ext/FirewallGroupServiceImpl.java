package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.FirewallGroupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.FirewallGroup;
import org.openstack4j.model.network.options.FirewallGroupOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronFirewallGroup;
import org.openstack4j.openstack.networking.domain.ext.NeutronFirewallGroup.NeutronFirewallGroupList;

public class FirewallGroupServiceImpl extends BaseNeutronExtService implements FirewallGroupService {

    private static final String PATH = "/fwaas/firewall_groups";
    private static final String ROOT = "firewall_group";

    @Override
    public List<? extends FirewallGroup> list() {
        return list(null);
    }

    @Override
    public List<? extends FirewallGroup> list(Map<String, String> filters) {
        return listOf(NeutronFirewallGroupList.class, PATH, filters);
    }

    @Override
    public FirewallGroup get(String id) {
        return show(NeutronFirewallGroup.class, PATH + "/" + id(id));
    }

    @Override
    public FirewallGroup create(FirewallGroupOptions options) {
        return create(NeutronFirewallGroup.class, PATH, ROOT, options);
    }

    @Override
    public FirewallGroup update(String id, FirewallGroupOptions options) {
        return update(NeutronFirewallGroup.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
