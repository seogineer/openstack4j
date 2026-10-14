package org.openstack4j.api.heat;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.heat.Resource;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.openstack.heat.domain.HeatResource;
import org.openstack4j.openstack.heat.domain.HeatStack;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Heat resource creation_time/attributes and stack disable_rollback. */
@Test(suiteName = "heat/ResponseFields")
public class HeatResponseFieldTests extends AbstractTest {

    private static final String RESOURCE = "{\"resource\":{\"resource_name\":\"my_server\",\"logical_resource_id\":\"my_server\","
            + "\"resource_status\":\"CREATE_COMPLETE\",\"resource_type\":\"OS::Nova::Server\","
            + "\"creation_time\":\"2015-06-25T14:59:53\",\"updated_time\":\"2015-06-26T14:59:53\","
            + "\"attributes\":{\"name\":\"my_server\",\"networks\":{\"private\":[\"10.0.0.5\"]}}}}";
    private static final String RESOURCES_LIST = "{\"resources\":[{\"resource_name\":\"my_server\",\"resource_type\":\"OS::Nova::Server\","
            + "\"creation_time\":\"2015-06-25T14:59:53\",\"updated_time\":\"2015-06-26T14:59:53\"}]}";
    private static final String STACK = "{\"stack\":{\"id\":\"s1\",\"stack_name\":\"stack\",\"stack_status\":\"CREATE_COMPLETE\",\"disable_rollback\":true}}";

    @Override
    protected Service service() {
        return Service.ORCHESTRATION;
    }

    public void resourceShow() throws Exception {
        respondWith(200, RESOURCE);

        Resource resource = osv3().heat().resources().show("stack", "s1", "my_server");

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/stacks/stack/s1/resources/my_server"), request.getPath());
        Assert.assertEquals(resource.getCreationTime().getTime(), 1435244393000L);
        Assert.assertEquals(resource.getTime().getTime(), 1435330793000L);
        Assert.assertEquals(resource.getAttributes().get("name"), "my_server");
        Assert.assertTrue(resource.getAttributes().get("networks") instanceof java.util.Map);
    }

    public void resourceListWithoutAttributes() throws Exception {
        respondWith(200, RESOURCES_LIST);

        List<? extends Resource> resources = osv3().heat().resources().list("stack", "s1");

        takeRequest();
        Assert.assertEquals(resources.size(), 1);
        Assert.assertEquals(resources.get(0).getCreationTime().getTime(), 1435244393000L);
        Assert.assertNull(resources.get(0).getAttributes());
    }

    public void stackDetailsDisableRollback() throws Exception {
        respondWith(200, STACK);

        Stack stack = osv3().heat().stacks().getDetails("stack", "s1");

        takeRequest();
        Assert.assertEquals(stack.getDisableRollback(), Boolean.TRUE);
    }

    public void stackListHasNoDisableRollback() throws Exception {
        respondWith(200, "{\"stacks\":[{\"id\":\"s1\",\"stack_name\":\"stack\",\"stack_status\":\"CREATE_COMPLETE\"}]}");

        List<? extends Stack> stacks = osv3().heat().stacks().list();

        takeRequest();
        Assert.assertNull(stacks.get(0).getDisableRollback());
    }

    public void notSerialized() throws Exception {
        HeatResource resource = ObjectMapperSingleton.getContext(HeatResource.class).readValue(RESOURCE, HeatResource.class);
        Assert.assertNotNull(resource.getAttributes());
        String written = ObjectMapperSingleton.getContext(HeatResource.class).writeValueAsString(resource).toLowerCase(java.util.Locale.ROOT).replace("_", "");
        Assert.assertFalse(written.contains("\"attributes\""), written);
        Assert.assertFalse(written.contains("\"creationtime\""), written);

        HeatStack stack = ObjectMapperSingleton.getContext(HeatStack.class).readValue(STACK, HeatStack.class);
        Assert.assertEquals(stack.getDisableRollback(), Boolean.TRUE);
        String stackWritten = ObjectMapperSingleton.getContext(HeatStack.class).writeValueAsString(stack).toLowerCase(java.util.Locale.ROOT).replace("_", "");
        Assert.assertFalse(stackWritten.contains("\"disablerollback\""), stackWritten);
    }
}
