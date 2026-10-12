package org.openstack4j.api.messaging;

import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Messaging/Ping")
public class MessagingPingTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.MESSAGING;
    }

    /** Zaqar on a closed port; an own project id keeps DefaultEndpointURLResolver's cache apart from MessagingTests. */
    @Override
    protected String adjustTokenJson(String json) {
        return json.replace("http://127.0.0.1:8888", "http://127.0.0.1:1")
                .replace("123ac695d4db400a9001b91bb3b8aa46", "123ac695d4db400a9001b91bb3b8aa48");
    }

    @Test(timeOut = 30_000)
    public void pingIsFalseWhenServiceIsUnreachable() {
        Assert.assertFalse(osv3().messaging().ping());
    }
}
