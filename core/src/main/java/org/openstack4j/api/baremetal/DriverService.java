package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.Driver;

/** Bare metal drivers ({@code /v1/drivers}). */
public interface DriverService extends RestService {

    /** @return the drivers enabled on the conductors */
    List<? extends Driver> list();

    /** @param filters query parameters {@code type} ({@code classic} or {@code dynamic}) and {@code detail} ({@code true}), both microversion 1.30 */
    List<? extends Driver> list(Map<String, String> filters);

    /** @return the driver, or {@code null} when it is not enabled */
    Driver get(String driverName);

    /** @return the driver's {@code driver_info} properties and their descriptions; an unknown driver raises */
    Map<String, String> getProperties(String driverName);

    /** @return the RAID logical disk properties the driver accepts (microversion 1.12); an unknown driver raises */
    Map<String, Object> getRaidLogicalDiskProperties(String driverName);
}
