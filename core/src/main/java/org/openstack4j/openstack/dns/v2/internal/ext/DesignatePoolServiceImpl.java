package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.dns.v2.ext.DesignatePoolService;
import org.openstack4j.model.dns.v2.ext.Pool;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignatePool;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignatePool.DesignatePoolList;

public class DesignatePoolServiceImpl extends BaseDesignateExtService implements DesignatePoolService {

    @Override
    public List<? extends Pool> list() {
        return list(null);
    }

    @Override
    public List<? extends Pool> list(Map<String, String> filters) {
        return listOf(DesignatePoolList.class, "/pools", filters);
    }

    @Override
    public Pool get(String id) {
        return show(DesignatePool.class, "/pools/" + id(id));
    }
}
