package org.openstack4j.api.image.v2.ext;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.ResponseException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/DownloadStream")
public class ImageDownloadStreamTests extends AbstractImageExtTest {

    public void downloadStreams() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/octet-stream").setBody("binarydata"));

        try (InputStream in = osv3().imagesV2().download("i1")) {
            Assert.assertNotNull(in);
            Assert.assertEquals(new String(in.readAllBytes(), StandardCharsets.UTF_8), "binarydata");
        }

        RecordedRequest request = expect("GET", "/images/i1/file");
        Assert.assertEquals(request.getHeader("Accept"), "application/octet-stream");
    }

    public void downloadStreamCanBeClosedBeforeItIsRead() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/octet-stream").setBody("binarydata"));
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/octet-stream").setBody("again"));

        InputStream first = osv3().imagesV2().download("i1");
        Assert.assertEquals(first.read(), 'b');
        first.close();
        first.close();
        try (InputStream second = osv3().imagesV2().download("i1")) {
            Assert.assertEquals(new String(second.readAllBytes(), StandardCharsets.UTF_8), "again");
        }

        expect("GET", "/images/i1/file");
        expect("GET", "/images/i1/file");
    }

    public void download204Empty() throws Exception {
        respondWith(204);

        try (InputStream in = osv3().imagesV2().download("i1")) {
            Assert.assertNotNull(in);
            Assert.assertEquals(in.read(), -1);
        }
        expect("GET", "/images/i1/file");
    }

    public void download404Null() throws Exception {
        respondWith(404, "{\"message\": \"Image not found\"}");

        Assert.assertNull(osv3().imagesV2().download("missing"));
        expect("GET", "/images/missing/file");
    }

    public void download403Raises() throws Exception {
        respondWith(403, "{\"message\": \"Image i1 is deactivated<br /><br />\\n\\n\", \"code\": \"403 Forbidden\", \"title\": \"Forbidden\"}");

        try {
            osv3().imagesV2().download("i1");
            Assert.fail("expected a ResponseException");
        } catch (ResponseException expected) {
            Assert.assertEquals(expected.getStatus(), 403);
            // a webob top-level message is not used as the fault (see GlanceJsonFaultTests): the status line is
            Assert.assertTrue(expected.getMessage().contains("403"), expected.getMessage());
            Assert.assertFalse(expected.getMessage().contains("<br"), expected.getMessage());
        }
        expect("GET", "/images/i1/file");
    }
}
