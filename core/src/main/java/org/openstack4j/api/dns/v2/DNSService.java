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

    /** @return the top-level domains (admin) */
    org.openstack4j.api.dns.v2.ext.DesignateTldService tlds();

    /** @return the TSIG keys (admin) */
    org.openstack4j.api.dns.v2.ext.DesignateTsigKeyService tsigKeys();

    /** @return the zone name blacklists (admin) */
    org.openstack4j.api.dns.v2.ext.DesignateBlacklistService blacklists();

    /** @return the DNS quotas */
    org.openstack4j.api.dns.v2.ext.DesignateQuotaService quotas();

    /** @return the PTR records of floating IPs */
    org.openstack4j.api.dns.v2.ext.ReverseFloatingIpService reverseFloatingIps();
}
