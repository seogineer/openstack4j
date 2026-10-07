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

    /** @return zone exports and imports (zone files) */
    org.openstack4j.api.dns.v2.ext.ZoneFileService zoneFiles();

    /** @return the shares of zones with other projects */
    org.openstack4j.api.dns.v2.ext.ZoneShareService zoneShares();

    /** @return zone transfer requests and accepts */
    org.openstack4j.api.dns.v2.ext.ZoneTransferService zoneTransfers();
}
