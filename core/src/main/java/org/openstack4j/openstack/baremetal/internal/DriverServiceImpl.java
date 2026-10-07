package org.openstack4j.openstack.baremetal.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.DriverService;
import org.openstack4j.model.baremetal.Driver;
import org.openstack4j.openstack.baremetal.domain.IronicDriver;
import org.openstack4j.openstack.baremetal.domain.IronicDriver.IronicDriverList;

public class DriverServiceImpl extends BaseBaremetalServices implements DriverService {

    @Override
    public List<? extends Driver> list() {
        return list(null);
    }

    @Override
    public List<? extends Driver> list(Map<String, String> filters) {
        return listOf(IronicDriverList.class, "/drivers", filters);
    }

    @Override
    public Driver get(String driverName) {
        return show(IronicDriver.class, "/drivers/" + id(driverName));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getProperties(String driverName) {
        return showStrict(Map.class, "/drivers/" + id(driverName) + "/properties");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getRaidLogicalDiskProperties(String driverName) {
        return showStrict(Map.class, "/drivers/" + id(driverName) + "/raid/logical_disk_properties");
    }
}
