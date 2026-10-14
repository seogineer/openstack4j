package org.openstack4j.openstack.ironic.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.ironic.IronicMicroVersionService;
import org.openstack4j.api.ironic.IronicService;
import org.openstack4j.api.ironic.ConductorService;
import org.openstack4j.api.ironic.VolumeTargetService;
import org.openstack4j.api.ironic.VolumeConnectorService;
import org.openstack4j.api.ironic.InspectionRuleService;
import org.openstack4j.api.ironic.RunbookService;
import org.openstack4j.api.ironic.DeployTemplateService;
import org.openstack4j.api.ironic.AllocationService;
import org.openstack4j.api.ironic.ChassisService;
import org.openstack4j.api.ironic.DriverService;
import org.openstack4j.api.ironic.NodeService;
import org.openstack4j.api.ironic.PortService;
import org.openstack4j.api.ironic.PortgroupService;

public class IronicServiceImpl implements IronicService {

    @Override public IronicMicroVersionService microVersions() { return Apis.get(IronicMicroVersionService.class); }
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
