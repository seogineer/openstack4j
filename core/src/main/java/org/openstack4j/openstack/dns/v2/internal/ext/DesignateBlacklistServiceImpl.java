package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.dns.v2.ext.DesignateBlacklistService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.Blacklist;
import org.openstack4j.model.dns.v2.options.BlacklistCreate;
import org.openstack4j.model.dns.v2.options.BlacklistUpdate;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateBlacklist;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateBlacklist.DesignateBlacklistList;

public class DesignateBlacklistServiceImpl extends BaseDesignateExtService implements DesignateBlacklistService {

    @Override
    public List<? extends Blacklist> list() {
        return list(null);
    }

    @Override
    public List<? extends Blacklist> list(Map<String, String> filters) {
        return listOf(DesignateBlacklistList.class, "/blacklists", filters);
    }

    @Override
    public Blacklist get(String id) {
        return show(DesignateBlacklist.class, "/blacklists/" + id(id));
    }

    @Override
    public Blacklist create(BlacklistCreate create) {
        return create(DesignateBlacklist.class, "/blacklists", create);
    }

    @Override
    public Blacklist update(String id, BlacklistUpdate update) {
        return update(DesignateBlacklist.class, "/blacklists/" + id(id), update);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove("/blacklists/" + id(id));
    }
}
