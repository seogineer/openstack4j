package org.openstack4j.openstack.baremetal.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.baremetal.BaremetalMicroVersionService;
import org.openstack4j.api.baremetal.BaremetalService;
import org.openstack4j.api.baremetal.ConductorService;
import org.openstack4j.api.baremetal.VolumeTargetService;
import org.openstack4j.api.baremetal.VolumeConnectorService;
import org.openstack4j.api.baremetal.InspectionRuleService;
import org.openstack4j.api.baremetal.RunbookService;
import org.openstack4j.api.baremetal.DeployTemplateService;
import org.openstack4j.api.baremetal.AllocationService;
import org.openstack4j.api.baremetal.ChassisService;
import org.openstack4j.api.baremetal.DriverService;
import org.openstack4j.api.baremetal.NodeService;
import org.openstack4j.api.baremetal.PortService;
import org.openstack4j.api.baremetal.PortgroupService;

public class BaremetalServiceImpl implements BaremetalService {

    @Override public BaremetalMicroVersionService microVersions() { return Apis.get(BaremetalMicroVersionService.class); }
    @Override public NodeService nodes() { return Apis.get(NodeService.class); }
    @Override public PortService ports() { return Apis.get(PortService.class); }
    @Override public PortgroupService portgroups() { return Apis.get(PortgroupService.class); }
    @Override public ChassisService chassis() { return Apis.get(ChassisService.class); }
    @Override public DriverService drivers() { return Apis.get(DriverService.class); }
    @Override public AllocationService allocations() { return Apis.get(AllocationService.class); }
    @Override public DeployTemplateService deployTemplates() { return Apis.get(DeployTemplateService.class); }
    @Override public RunbookService runbooks() { return Apis.get(RunbookService.class); }
    @Override public InspectionRuleService inspectionRules() { return Apis.get(InspectionRuleService.class); }
    @Override public VolumeConnectorService volumeConnectors() { return Apis.get(VolumeConnectorService.class); }
    @Override public VolumeTargetService volumeTargets() { return Apis.get(VolumeTargetService.class); }
    @Override public ConductorService conductors() { return Apis.get(ConductorService.class); }
}
