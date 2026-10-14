package org.openstack4j.test.masakari;

import java.lang.reflect.Field;
import java.util.function.Function;

import org.openstack4j.openstack.masakari.internal.SegmentServiceImpl;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Masakari catalog endpoints may have no version (kolla: {@code http://vip:15868}); the services add {@code /v1}
 * then and leave an endpoint that already has one alone.
 */
@Test(suiteName = "Masakari Endpoint")
public class MasakariEndpointTest {

    @SuppressWarnings("unchecked")
    private static Function<String, String> endpointFunc() throws Exception {
        Field field = BaseOpenStackService.class.getDeclaredField("endpointFunc");
        field.setAccessible(true);
        return (Function<String, String>) field.get(new SegmentServiceImpl());
    }

    public void versionlessEndpointGetsV1() throws Exception {
        Function<String, String> func = endpointFunc();
        assertNotNull(func, "Masakari services must enforce /v1");
        assertEquals(func.apply("http://h:15868"), "http://h:15868/v1");
        assertEquals(func.apply("http://h:15868/"), "http://h:15868/v1");
    }

    public void versionedEndpointIsKept() throws Exception {
        Function<String, String> func = endpointFunc();
        assertNotNull(func);
        assertEquals(func.apply("http://h:15868/v1"), "http://h:15868/v1");
        assertEquals(func.apply("http://h:15868/v1/0123abcd"), "http://h:15868/v1/0123abcd");
    }
}
