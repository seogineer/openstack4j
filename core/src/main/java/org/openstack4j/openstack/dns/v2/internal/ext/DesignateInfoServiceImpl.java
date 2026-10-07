package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.Map;

import org.openstack4j.api.dns.v2.ext.DesignateInfoService;

public class DesignateInfoServiceImpl extends BaseDesignateExtService implements DesignateInfoService {

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> limits() {
        return showStrict(Map.class, "/limits");
    }
}
