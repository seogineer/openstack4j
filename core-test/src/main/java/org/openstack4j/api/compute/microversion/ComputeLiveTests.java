package org.openstack4j.api.compute.microversion;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.ComputeVersion;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.RemoteConsole;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.network.Network;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Runs against a real OpenStack when the standard OS_* environment variables are set; skipped otherwise.
 * Negotiates compute microversions, reads existing servers and creates a throw-away key pair and server that are
 * deleted in {@code finally}.
 */
@Test(suiteName = "Compute/Live", groups = "compute-live")
public class ComputeLiveTests {

    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live compute tests");
        String domain = env("OS_USER_DOMAIN_NAME", "Default");
        os = OSFactory.builderV3()
                .endpoint(url.replaceAll("/+$", "").endsWith("/v3") ? url : url.replaceAll("/+$", "") + "/v3")
                .credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null), Identifier.byName(domain))
                .scopeToProject(Identifier.byName(env("OS_PROJECT_NAME", null)), Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default")))
                .authenticate();
        ComputeVersion version = os.compute().microVersions().negotiate();
        Assert.assertTrue(version.isEnabled());
        System.out.println("compute microversion " + version.getMicroVersion() + " (server max " + version.getServerMaxVersion() + ")");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live compute tests");
        }
        return value;
    }

    public void listsServersWithEmbeddedFlavor() {
        List<? extends Server> servers = os.compute().servers().list();
        if (servers.isEmpty())
            throw new SkipException("no servers");
        Server server = os.compute().servers().get(servers.get(0).getId());
        Assert.assertNotNull(server.getFlavorSummary().getOriginalName());
        Assert.assertNotNull(server.getTags());
        Assert.assertNotNull(os.compute().servers().ips(server.getId()));
        os.compute().servers().migrations(server.getId());
        os.compute().servers().instanceActions().list(server.getId());
        Assert.assertNotNull(os.compute().servers().topology(server.getId()));
        if (server.getStatus() == Server.Status.ACTIVE) {
            Assert.assertNotNull(os.compute().servers().diagnosticsStandard(server.getId()).getDriver());
            Assert.assertNotNull(os.compute().servers().remoteConsole(server.getId(), "vnc", "novnc").getUrl());
        }
    }

    public void legacyCallsStillWorkAfterNegotiation() {
        os.compute().flavors().list();
        os.compute().hypervisors().list();
        os.compute().services().list();
        os.compute().keypairs().list();
        os.compute().serverGroups().list();
        os.compute().hostAggregates().list();
    }

    public void keypairWithoutPublicKeyUsesCeiling() {
        String name = "os4j-live-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            Keypair keypair = os.compute().keypairs().create(name, null);
            Assert.assertNotNull(keypair.getPrivateKey());
        } finally {
            os.compute().keypairs().delete(name);
        }
    }

    public void temporaryServerLifecycle() {
        Flavor flavor = os.compute().flavors().list().stream()
                .min(Comparator.comparingInt(Flavor::getRam)).orElseThrow(() -> new SkipException("no flavor"));
        Image image = os.imagesV2().list().stream().filter(i -> "active".equalsIgnoreCase(String.valueOf(i.getStatus())))
                .findFirst().orElseThrow(() -> new SkipException("no active image"));
        String name = "os4j-live-" + UUID.randomUUID().toString().substring(0, 8);
        Server server = null;
        try {
            try {
                server = os.compute().servers().boot(os.compute().servers().serverBuilder()
                        .name(name).flavor(flavor.getId()).image(image.getId()).networks(Collections.singletonList(pickNetworkId()))
                        .tags(Collections.singletonList("os4j")).build());
            } catch (RuntimeException e) {
                throw new SkipException("cannot create a server here: " + e.getMessage());
            }
            server = os.compute().servers().waitForServerStatus(server.getId(), Server.Status.ACTIVE, 5, TimeUnit.MINUTES);
            if (server.getStatus() != Server.Status.ACTIVE)
                throw new SkipException("server did not become ACTIVE: " + server.getStatus()
                        + (server.getFault() != null ? " (" + server.getFault().getMessage() + ")" : ""));
            Assert.assertEquals(server.getTags(), Collections.singletonList("os4j"));
            Assert.assertTrue(os.compute().servers().lock(server.getId(), "os4j live test").isSuccess());
            Assert.assertEquals(os.compute().servers().get(server.getId()).getLockedReason(), "os4j live test");
            os.compute().servers().action(server.getId(), Action.UNLOCK);
            os.compute().servers().topology(server.getId());
            RemoteConsole console = os.compute().servers().remoteConsole(server.getId(), "vnc", "novnc");
            Assert.assertNotNull(console.getUrl());
            Assert.assertNotNull(os.compute().servers().diagnosticsStandard(server.getId()).getDriver());
        } finally {
            if (server != null)
                os.compute().servers().delete(server.getId());
        }
    }

    /** A network an existing server is attached to, otherwise the first network. */
    private String pickNetworkId() {
        List<? extends Network> networks = os.networking().network().list();
        if (networks.isEmpty())
            throw new SkipException("no networks");
        for (Server existing : os.compute().servers().list()) {
            if (existing.getAddresses() == null)
                continue;
            for (String label : existing.getAddresses().getAddresses().keySet())
                for (Network network : networks)
                    if (label.equals(network.getName()))
                        return network.getId();
        }
        return networks.get(0).getId();
    }
}
