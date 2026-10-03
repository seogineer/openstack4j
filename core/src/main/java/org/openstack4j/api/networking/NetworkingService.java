package org.openstack4j.api.networking;

import org.openstack4j.api.networking.ext.*;
import org.openstack4j.common.RestService;

/**
 * OpenStack Networking Operations API
 *
 * @author Jeremy Unruh
 */
public interface NetworkingService extends RestService {

    /**
     * @return the Network Service API
     */
    NetworkService network();

    /**
     * @return the Subnet Service API
     */
    SubnetService subnet();

    /**
     * @return the Port Service API
     */
    PortService port();

    /**
     * @return the Router Service API
     */
    RouterService router();

    /**
     * @return the FloatingIP Service API
     */
    NetFloatingIPService floatingip();

    /**
     * @return the Security Group Service API
     */
    SecurityGroupService securitygroup();

    /**
     * @return the (Neutron) Resource Tag Service API
     */
    NeutronResourceTagService resourceTags();

    /**
     * @return the Security Group Rule Service API
     */
    SecurityGroupRuleService securityrule();

    /**
     * @return the network quota service
     */
    NetQuotaService quotas();

    /**
     * @return the LBaaS service
     */
    LoadBalancerService loadbalancers();

    /**
     * @return the Neutron agent API
     */
    AgentService agent();

    /**
     * @return the LBaaS V2 service
     */
    LbaasV2Service lbaasV2();

    /**
     * <p>OpenStack Firewall As a Service <code>(FwaaS) : Firewall</code> Operations API</p>
     *
     * @return the FwaaS service
     */
    FirewallAsService firewalls();

    /**
     * @return the Availability Zone Service API
     */
    AvailabilityZoneService availabilityzone();

    /**
     * @return the Network IP Availability Service API
     */
    NetworkIPAvailabilityService networkIPAvailability();

    /**
     * @return the Trunk API
     */
    TrunkService trunk();

    /**
     * @return the Networking (Neutron) Qos Policy Extension API
     */
    NetQosPolicyService netQosPolicy();

    /**
     * @return the Networking (Neutron) Qos Policy Bandwidth Limit Rule Extension API
     */
    NetQosPolicyBLRuleService netQosPolicyBandwidthLimitRule();


    /**
     * Neutron API extensions: list, get, isEnabled.
     *
     * @return the NeutronExtensionService
     */
    org.openstack4j.api.networking.ext.NeutronExtensionService extensions();

    /**
     * Neutron service providers.
     *
     * @return the ServiceProviderService
     */
    org.openstack4j.api.networking.ext.ServiceProviderService serviceProviders();

    /**
     * Auto-allocated topology (get-me-a-network).
     *
     * @return the AutoAllocatedTopologyService
     */
    org.openstack4j.api.networking.ext.AutoAllocatedTopologyService autoAllocatedTopology();

    /**
     * QoS rule types and DSCP/minimum bandwidth/packet rate rules, including alias rules.
     *
     * @return the QosRuleService
     */
    org.openstack4j.api.networking.ext.QosRuleService qosRules();

    /**
     * Subnet pools with prefix operations and subnet onboarding.
     *
     * @return the SubnetPoolService
     */
    org.openstack4j.api.networking.ext.SubnetPoolService subnetPools();

    /**
     * Address scopes.
     *
     * @return the AddressScopeService
     */
    org.openstack4j.api.networking.ext.AddressScopeService addressScopes();

    /**
     * Address groups.
     *
     * @return the AddressGroupService
     */
    org.openstack4j.api.networking.ext.AddressGroupService addressGroups();

    /**
     * RBAC policies.
     *
     * @return the RbacPolicyService
     */
    org.openstack4j.api.networking.ext.RbacPolicyService rbacPolicies();

    /**
     * Default security group rules.
     *
     * @return the DefaultSecurityGroupRuleService
     */
    org.openstack4j.api.networking.ext.DefaultSecurityGroupRuleService defaultSecurityGroupRules();

    /**
     * Security group default statefulness per project.
     *
     * @return the SecurityGroupDefaultStatefulnessService
     */
    org.openstack4j.api.networking.ext.SecurityGroupDefaultStatefulnessService securityGroupDefaultStatefulness();

    /**
     * Network segments.
     *
     * @return the SegmentService
     */
    org.openstack4j.api.networking.ext.SegmentService segments();

    /**
     * Network segment ranges.
     *
     * @return the NetworkSegmentRangeService
     */
    org.openstack4j.api.networking.ext.NetworkSegmentRangeService networkSegmentRanges();

    /**
     * Local IPs and their port associations.
     *
     * @return the LocalIpService
     */
    org.openstack4j.api.networking.ext.LocalIpService localIps();

    /**
     * Router NDP proxies.
     *
     * @return the NdpProxyService
     */
    org.openstack4j.api.networking.ext.NdpProxyService ndpProxies();
}
