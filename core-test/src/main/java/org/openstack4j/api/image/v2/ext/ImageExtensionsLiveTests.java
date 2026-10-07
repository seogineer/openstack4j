package org.openstack4j.api.image.v2.ext;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.image.v2.ContainerFormat;
import org.openstack4j.model.image.v2.DiskFormat;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.image.v2.ext.ImageVersions;
import org.openstack4j.model.image.v2.options.ImageImportOptions;
import org.openstack4j.model.image.v2.options.MetadefNamespaceOptions;
import org.openstack4j.model.image.v2.options.MetadefObjectOptions;
import org.openstack4j.model.image.v2.options.MetadefPropertyOptions;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Runs against a real Glance when OS_AUTH_URL is set; every temporary resource is removed in finally. */
@Test(suiteName = "Image/V2/Ext/Live", groups = "image-live", singleThreaded = true)
public class ImageExtensionsLiveTests {

    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live image tests");
        String authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        String token = System.getenv("OS_TOKEN");
        Identifier project = Identifier.byName(env("OS_PROJECT_NAME", null));
        Identifier projectDomain = Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"));
        os = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToProject(project, projectDomain).authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null),
                Identifier.byName(env("OS_USER_DOMAIN_NAME", "Default"))).scopeToProject(project, projectDomain).authenticate();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live image tests");
        }
        return value;
    }

    public void versionsAndInfo() {
        ImageVersions versions = os.imagesV2().versions();
        Assert.assertTrue(versions.supports("2.6"), versions.getCurrent());
        Assert.assertTrue(os.imagesV2().info().importMethods().contains("glance-direct"));
        Assert.assertNotNull(os.imagesV2().info().usage());
    }

    public void stageAndImport() throws Exception {
        Image image = os.imagesV2().create(Builders.imageV2().name("os4j-live-import").containerFormat(ContainerFormat.BARE).diskFormat(DiskFormat.RAW).build());
        try {
            Assert.assertTrue(os.imagesV2().stage(image.getId(), Payloads.create(new ByteArrayInputStream(new byte[1024]))).isSuccess());
            Assert.assertTrue(os.imagesV2().importImage(image.getId(), ImageImportOptions.glanceDirect()).isSuccess());
            for (int i = 0; i < 30 && os.imagesV2().get(image.getId()).getStatus() != Image.ImageStatus.ACTIVE; i++)
                Thread.sleep(1000);
            Assert.assertEquals(os.imagesV2().get(image.getId()).getStatus(), Image.ImageStatus.ACTIVE);
            Assert.assertFalse(os.imagesV2().listTasks(image.getId()).isEmpty());
            if (os.imagesV2().versions().supports("2.17")) {
                // GET locations is service-to-service (policy role:service); an admin/member token gets 403
                try {
                    Assert.assertNotNull(os.imagesV2().listLocations(image.getId()));
                } catch (org.openstack4j.api.exceptions.ResponseException e) {
                    if (e.getStatus() != 403) throw e;
                }
            }
        } finally {
            Assert.assertTrue(os.imagesV2().delete(image.getId()).isSuccess());
        }
    }

    public void schemas() {
        Assert.assertEquals(os.imagesV2().schemas().image().get("name"), "image");
        Assert.assertEquals(os.imagesV2().schemas().metadefNamespace().get("name"), "namespace");
    }

    public void metadefLifecycle() {
        String ns = "OS4J::Live::" + System.nanoTime();
        var metadefs = os.imagesV2().metadefs();
        metadefs.createNamespace(MetadefNamespaceOptions.create(ns).displayName("os4j live").visibility("private"));
        try {
            metadefs.createObject(ns, MetadefObjectOptions.create("obj").description("o"));
            metadefs.createProperty(ns, MetadefPropertyOptions.create("prop", "Prop", "string"));
            metadefs.createTags(ns, List.of("t1", "t2"), false);
            metadefs.associateResourceType(ns, "OS::Glance::Image", "os4j_", null);
            Assert.assertEquals(metadefs.listObjects(ns).size(), 1);
            Assert.assertTrue(metadefs.listProperties(ns).containsKey("prop"));
            Assert.assertEquals(metadefs.listTags(ns).size(), 2);
            Assert.assertEquals(metadefs.listResourceTypeAssociations(ns).get(0).getName(), "OS::Glance::Image");
        } finally {
            Assert.assertTrue(metadefs.deleteNamespace(ns).isSuccess());
        }
    }

    public void cacheWhenEnabled() {
        try {
            Assert.assertNotNull(os.imagesV2().cache().list());
        } catch (RuntimeException e) {
            throw new SkipException("image cache API not enabled: " + e.getMessage());
        }
    }
}
