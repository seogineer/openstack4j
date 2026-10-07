package org.openstack4j.openstack.dns.v2.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.dns.v2.DNSService;
import org.openstack4j.api.dns.v2.RecordsetService;
import org.openstack4j.api.dns.v2.ZoneService;


/**
 * DNS/Designate V2 service implementation
 */
public class DNSServiceImpl extends BaseDNSServices implements DNSService {

    @Override
    public ZoneService zones() {
        return Apis.get(ZoneService.class);
    }

    @Override
    public RecordsetService recordsets() {
        return Apis.get(RecordsetService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.DesignatePoolService pools() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.DesignatePoolService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.DesignateServiceStatusService serviceStatuses() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.DesignateServiceStatusService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.DesignateInfoService info() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.DesignateInfoService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.ZoneFileService zoneFiles() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.ZoneFileService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.ZoneShareService zoneShares() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.ZoneShareService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.ZoneTransferService zoneTransfers() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.ZoneTransferService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.DesignateTldService tlds() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.DesignateTldService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.DesignateTsigKeyService tsigKeys() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.DesignateTsigKeyService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.DesignateBlacklistService blacklists() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.DesignateBlacklistService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.DesignateQuotaService quotas() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.DesignateQuotaService.class);
    }

    @Override
    public org.openstack4j.api.dns.v2.ext.ReverseFloatingIpService reverseFloatingIps() {
        return Apis.get(org.openstack4j.api.dns.v2.ext.ReverseFloatingIpService.class);
    }
}
