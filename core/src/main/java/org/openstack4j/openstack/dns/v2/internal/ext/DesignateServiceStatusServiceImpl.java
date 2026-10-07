package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.dns.v2.ext.DesignateServiceStatusService;
import org.openstack4j.model.dns.v2.ext.ServiceStatus;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateServiceStatus;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateServiceStatus.DesignateServiceStatusList;

public class DesignateServiceStatusServiceImpl extends BaseDesignateExtService implements DesignateServiceStatusService {

    @Override
    public List<? extends ServiceStatus> list() {
        return list(null);
    }

    @Override
    public List<? extends ServiceStatus> list(Map<String, String> filters) {
        return listOf(DesignateServiceStatusList.class, "/service_statuses", filters);
    }

    @Override
    public ServiceStatus get(String id) {
        return show(DesignateServiceStatus.class, "/service_statuses/" + id(id));
    }
}
