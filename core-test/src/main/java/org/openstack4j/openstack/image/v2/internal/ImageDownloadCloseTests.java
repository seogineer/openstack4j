package org.openstack4j.openstack.image.v2.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.openstack4j.core.transport.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

/** The download stream must release the HTTP response when it is closed, whatever the connector. */
@Test(suiteName = "Image/DownloadStreamClose")
public class ImageDownloadCloseTests {

    private static HttpResponse response(InputStream body, AtomicInteger closed) {
        return (HttpResponse) Proxy.newProxyInstance(HttpResponse.class.getClassLoader(), new Class<?>[]{HttpResponse.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getInputStream":
                    return body;
                case "close":
                    closed.incrementAndGet();
                    return null;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });
    }

    public void closingTheStreamClosesTheStreamAndTheResponse() throws IOException {
        AtomicInteger responseClosed = new AtomicInteger();
        AtomicInteger bodyClosed = new AtomicInteger();
        InputStream body = new ByteArrayInputStream(new byte[]{1, 2, 3}) {
            @Override
            public void close() throws IOException {
                bodyClosed.incrementAndGet();
                super.close();
            }
        };

        InputStream stream = ImageServiceImpl.streamOf(response(body, responseClosed));
        Assert.assertEquals(stream.read(), 1);
        Assert.assertEquals(responseClosed.get(), 0, "the response stays open while the caller reads");

        stream.close();
        Assert.assertEquals(bodyClosed.get(), 1);
        Assert.assertEquals(responseClosed.get(), 1);
    }

    public void responseIsClosedEvenWhenClosingTheBodyFails() {
        AtomicInteger responseClosed = new AtomicInteger();
        InputStream body = new ByteArrayInputStream(new byte[0]) {
            @Override
            public void close() throws IOException {
                throw new IOException("boom");
            }
        };

        InputStream stream = ImageServiceImpl.streamOf(response(body, responseClosed));
        try {
            stream.close();
            Assert.fail("expected the IOException");
        } catch (IOException expected) {
            Assert.assertEquals(expected.getMessage(), "boom");
        }
        Assert.assertEquals(responseClosed.get(), 1);
    }

    public void noBodyClosesTheResponseAtOnce() throws IOException {
        AtomicInteger responseClosed = new AtomicInteger();

        InputStream stream = ImageServiceImpl.streamOf(response(null, responseClosed));

        Assert.assertEquals(stream.read(), -1);
        Assert.assertEquals(responseClosed.get(), 1);
    }
}
