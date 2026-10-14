package org.openstack4j.openstack.ironic.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.ironic.DriverService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.ironic.Driver;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.ironic.domain.IronicDriver;
import org.openstack4j.openstack.ironic.domain.IronicDriver.IronicDriverList;

public class DriverServiceImpl extends BaseBaremetalServices implements DriverService {

    private static String driver(String driverName) {
        return "/drivers/" + id(driverName);
    }

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

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> listVendorPassthruMethods(String driverName) {
        return showStrict(Map.class, driver(driverName) + "/vendor_passthru/methods");
    }

    @Override
    public ActionResponse vendorPassthru(String driverName, String method, Map<String, ?> args) {
        return passthruAction(driver(driverName), method, args);
    }

    @Override
    public Map<String, Object> vendorPassthru(String driverName, HttpMethod httpMethod, String method, Map<String, ?> args) {
        return passthru(driver(driverName), httpMethod, method, args);
    }
}
