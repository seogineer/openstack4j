package org.openstack4j.api.tacker;

import org.openstack4j.api.Apis;

/**
 * @author Vishvesh Deshmukh
 * @date Aug 11, 2016
 */
public class TackerServiceImpl implements TackerService {

    @Override
    public VnfdService vnfd() {
        return Apis.get(VnfdService.class);
    }

    @Override
    public VnfService vnf() {
        return Apis.get(VnfService.class);
    }

    @Override
    public VimService vim() {
        return Apis.get(VimService.class);
    }

    @Override
    public VnfPackageService vnfPackages() {
        return Apis.get(VnfPackageService.class);
    }

    @Override
    public VnfLcmService vnfLcm() {
        return Apis.get(VnfLcmService.class);
    }

    @Override
    public VnfLcmService vnfLcmV1() {
        return new org.openstack4j.openstack.tacker.internal.sol.VnfLcmServiceImpl.V1();
    }

    @Override
    public VnfFmService vnfFaults() {
        return Apis.get(VnfFmService.class);
    }

    @Override
    public VnfPmService vnfPerformance() {
        return Apis.get(VnfPmService.class);
    }
}
