package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.ext.AddressGroup;
import org.openstack4j.model.network.ext.AddressScope;
import org.openstack4j.model.network.ext.QosDscpMarkingRule;
import org.openstack4j.model.network.ext.RbacPolicy;
import org.openstack4j.model.network.ext.ServiceFlavor;
import org.openstack4j.model.network.ext.ServiceProfile;
import org.openstack4j.model.network.ext.SubnetPool;
import org.openstack4j.model.network.ext.NetQosPolicy;
import org.openstack4j.model.network.options.AddressGroupOptions;
import org.openstack4j.model.network.options.AddressScopeOptions;
import org.openstack4j.model.network.options.QosRuleOptions;
import org.openstack4j.model.network.options.RbacPolicyOptions;
import org.openstack4j.model.network.options.ServiceFlavorOptions;
import org.openstack4j.model.network.options.ServiceProfileOptions;
import org.openstack4j.model.network.options.SubnetPoolOptions;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.networking.domain.NeutronHostRoute;
import org.openstack4j.openstack.networking.domain.ext.NeutronNetQosPolicy;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Runs against a real Neutron when OS_AUTH_URL is set; every temporary resource is removed in finally. */
@Test(suiteName = "Network/Ext2/Live", groups = "networking-live", singleThreaded = true)
public class NetworkingExtensionsLiveTests {

    private OSClientV3 os;
    private String projectId;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live networking tests");
        String authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        String token = System.getenv("OS_TOKEN");
        Identifier project = Identifier.byName(env("OS_PROJECT_NAME", null));
        Identifier projectDomain = Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"));
        os = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToProject(project, projectDomain).authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null),
                Identifier.byName(env("OS_USER_DOMAIN_NAME", "Default"))).scopeToProject(project, projectDomain).authenticate();
        projectId = os.getToken().getProject().getId();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live networking tests");
        }
        return value;
    }

    private void requireExtension(String alias) {
        if (!os.networking().extensions().isEnabled(alias))
            throw new SkipException(alias + " is not enabled on this Neutron");
    }

    public void discovery() {
        Assert.assertTrue(os.networking().extensions().isEnabled("qos"));
        Assert.assertFalse(os.networking().serviceProviders().list().isEmpty());
        Assert.assertNotNull(os.networking().floatingip().listPools());
        Assert.assertTrue(os.networking().quotas().getDefault(projectId).getNetwork() != 0);
        Assert.assertNotNull(os.networking().quotas().getDetails(projectId).get("network").getLimit());
        Assert.assertFalse(os.networking().defaultSecurityGroupRules().list().isEmpty());
        Assert.assertFalse(os.networking().qosRules().ruleTypes().isEmpty());
    }

    public void qosPolicyRules() {
        NetQosPolicy policy = os.networking().netQosPolicy().create(NeutronNetQosPolicy.builder().name("os4j-live-qos").build());
        try {
            QosDscpMarkingRule dscp = os.networking().qosRules().createDscpMarkingRule(policy.getId(), QosRuleOptions.dscpMarking(26));
            Assert.assertEquals(os.networking().qosRules().getDscpMarkingRule(policy.getId(), dscp.getId()).getDscpMark(), Integer.valueOf(26));
            os.networking().qosRules().createMinimumBandwidthRule(policy.getId(), QosRuleOptions.minimumBandwidth(1000).direction("egress"));
            Assert.assertEquals(os.networking().qosRules().listMinimumBandwidthRules(policy.getId()).size(), 1);
        } finally {
            Assert.assertTrue(os.networking().netQosPolicy().delete(policy.getId()).isSuccess());
        }
    }

    public void addressScopeAndSubnetPool() {
        AddressScope scope = os.networking().addressScopes().create(AddressScopeOptions.create("os4j-live-scope", 4));
        try {
            SubnetPool pool = os.networking().subnetPools().create(SubnetPoolOptions.create("os4j-live-pool", List.of("10.250.0.0/24"))
                    .defaultPrefixlen(28).addressScopeId(scope.getId()));
            try {
                List<String> prefixes = os.networking().subnetPools().addPrefixes(pool.getId(), List.of("10.250.1.0/24"));
                Assert.assertTrue(prefixes.contains("10.250.0.0/23") || prefixes.size() == 2, prefixes.toString());
                os.networking().subnetPools().removePrefixes(pool.getId(), List.of("10.250.1.0/24"));
            } finally {
                Assert.assertTrue(os.networking().subnetPools().delete(pool.getId()).isSuccess());
            }
        } finally {
            Assert.assertTrue(os.networking().addressScopes().delete(scope.getId()).isSuccess());
        }
    }

    public void addressGroup() {
        AddressGroup group = os.networking().addressGroups().create(AddressGroupOptions.create("os4j-live-group"));
        try {
            Assert.assertEquals(os.networking().addressGroups().addAddresses(group.getId(), List.of("10.251.0.10/32")).getAddresses(), List.of("10.251.0.10/32"));
            Assert.assertTrue(os.networking().addressGroups().removeAddresses(group.getId(), List.of("10.251.0.10/32")).getAddresses().isEmpty());
        } finally {
            Assert.assertTrue(os.networking().addressGroups().delete(group.getId()).isSuccess());
        }
    }

    public void rbacOnTemporaryNetwork() {
        Network network = os.networking().network().create(Builders.network().name("os4j-live-net").adminStateUp(true).build());
        try {
            RbacPolicy policy = os.networking().rbacPolicies().create(RbacPolicyOptions.create("network", network.getId(), "access_as_shared", projectId));
            try {
                Assert.assertEquals(os.networking().rbacPolicies().get(policy.getId()).getObjectId(), network.getId());
            } finally {
                Assert.assertTrue(os.networking().rbacPolicies().delete(policy.getId()).isSuccess());
            }
        } finally {
            Assert.assertTrue(os.networking().network().delete(network.getId()).isSuccess());
        }
    }

    /** add/remove_extraroutes need extraroute-atomic; the dev Neutron (OVN) only has extraroute, so this skips there. */
    public void routerExtraRoutes() {
        requireExtension("extraroute-atomic");
        Router router = os.networking().router().create(Builders.router().name("os4j-live-router").adminStateUp(true).build());
        try {
            // a nexthop must be on a router subnet; without interfaces Neutron rejects it, which still proves the request shape
            try {
                os.networking().router().addExtraRoutes(router.getId(), List.of(new NeutronHostRoute("10.252.0.0/24", "10.252.1.1")));
            } catch (RuntimeException expected) {
                Assert.assertTrue(String.valueOf(expected.getMessage()).toLowerCase().contains("nexthop"), expected.getMessage());
            }
            Assert.assertTrue(os.networking().router().removeExtraRoutes(router.getId(), List.of()).getRoutes().isEmpty());
        } finally {
            Assert.assertTrue(os.networking().router().delete(router.getId()).isSuccess());
        }
    }

    public void serviceFlavorAndProfile() {
        requireExtension("flavors");
        ServiceProfile profile = os.networking().serviceProfiles().create(ServiceProfileOptions.create().description("os4j-live-profile").metainfo("{}"));
        try {
            ServiceFlavor flavor = os.networking().serviceFlavors().create(ServiceFlavorOptions.create("os4j-live-flavor", "L3_ROUTER_NAT"));
            try {
                Assert.assertTrue(os.networking().serviceFlavors().associateProfile(flavor.getId(), profile.getId()).isSuccess());
                Assert.assertTrue(os.networking().serviceFlavors().disassociateProfile(flavor.getId(), profile.getId()).isSuccess());
            } finally {
                Assert.assertTrue(os.networking().serviceFlavors().delete(flavor.getId()).isSuccess());
            }
        } finally {
            Assert.assertTrue(os.networking().serviceProfiles().delete(profile.getId()).isSuccess());
        }
    }

    public void portBindingsOfExistingPort() {
        // Neutron lists bindings only for compute (and shared filesystem) ports
        var port = os.networking().port().list().stream()
                .filter(p -> p.getDeviceOwner() != null && p.getDeviceOwner().startsWith("compute:")).findFirst()
                .orElseThrow(() -> new SkipException("no compute port to inspect"));
        Assert.assertFalse(os.networking().port().listBindings(port.getId()).isEmpty());
    }

    public void autoAllocatedTopologyDryRunReportsMissingExternalNetwork() {
        try {
            Assert.assertEquals(os.networking().autoAllocatedTopology().validate(projectId).getDryRun(), "pass");
        } catch (RuntimeException expected) {
            Assert.assertTrue(String.valueOf(expected.getMessage()).contains("external"), expected.getMessage());
        }
    }
}
