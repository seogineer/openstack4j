package org.openstack4j.openstack.compute.internal.ext;

import org.openstack4j.openstack.internal.microversion.MicroVersions;

import org.openstack4j.model.compute.ext.HypervisorListOptions;

import static org.openstack4j.openstack.compute.internal.ComputeMicroVersions.V;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.compute.ext.HypervisorService;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.compute.ext.HypervisorStatistics;
import org.openstack4j.openstack.compute.domain.ext.ExtHypervisor;
import org.openstack4j.openstack.compute.domain.ext.ExtHypervisor.Hypervisors;
import org.openstack4j.openstack.compute.domain.ext.ExtHypervisorStatistics;
import org.openstack4j.openstack.compute.internal.BaseComputeServices;

/**
 * Implementation for the OSHypervisors.
 */
public class HypervisorServiceImpl extends BaseComputeServices implements HypervisorService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Hypervisor> list() {
        return get(Hypervisors.class, "/os-hypervisors/detail").execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HypervisorStatistics statistics() {
        return capped(get(ExtHypervisorStatistics.class, "/os-hypervisors/statistics"), V(87)).execute();    // removed in 2.88
    }

    /**
     * <p>Description:Get the specified Hypervisor by ID</p>
     * <p>Author:Wang Ting/王婷</p>
     *
     * @param hypervisorId the UUID of the hypervisor
     * @return the hypervisor
     */
    @Override
    public Hypervisor get(String hypervisorId) {
        Objects.requireNonNull(hypervisorId);
        return get(ExtHypervisor.class, uri("/os-hypervisors/%s", hypervisorId)).execute();
    }

    @Override
    public List<? extends Hypervisor> list(HypervisorListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Hypervisor list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(Hypervisors.class, "/os-hypervisors/detail").params(options.toQueryParams()).execute().getList();
    }
}
