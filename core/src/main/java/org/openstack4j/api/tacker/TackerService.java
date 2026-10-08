package org.openstack4j.api.tacker;

import org.openstack4j.common.RestService;

/**
 * @author Vishvesh Deshmukh
 * @date Aug 11, 2016
 */
public interface TackerService extends RestService {

    /**
     * @return the Vnfd Service API
     */
    VnfdService vnfd();

    /**
     * @return the Vnf Service API
     */
    VnfService vnf();

    /**
     * @return the Vim Service API
     */
    VimService vim();

    /**
     * @return VNF package management (ETSI NFV-SOL 005, {@code /vnfpkgm/v1})
     */
    VnfPackageService vnfPackages();

    /**
     * @return VNF lifecycle management v2 (ETSI NFV-SOL 003, {@code /vnflcm/v2})
     */
    VnfLcmService vnfLcm();

    /**
     * @return VNF lifecycle management v1 ({@code /vnflcm/v1})
     */
    VnfLcmService vnfLcmV1();

    /**
     * @return VNF fault management ({@code /vnffm/v1})
     */
    VnfFmService vnfFaults();

    /**
     * @return VNF performance management ({@code /vnfpm/v2})
     */
    VnfPmService vnfPerformance();
}
