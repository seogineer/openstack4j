package org.openstack4j.api.dns.v2;

import org.openstack4j.common.RestService;

/**
 * DNS/Designate Service Operations API
 */
public interface DNSService extends RestService {

    /**
     * Zone Service API
     *
     * @return the zone service
     */
    ZoneService zones();

    /**
     * Recordset Service API
     *
     * @return the recordsets service
     */
    RecordsetService recordsets();

    /** @return the pools (admin) */
    org.openstack4j.api.dns.v2.ext.DesignatePoolService pools();

    /** @return the statuses of the Designate services (admin) */
    org.openstack4j.api.dns.v2.ext.DesignateServiceStatusService serviceStatuses();

    /** @return the limits */
    org.openstack4j.api.dns.v2.ext.DesignateInfoService info();
}
