package org.openstack4j.api.storage;

import java.util.List;

import org.openstack4j.api.AbstractTest;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.storage.object.SwiftContainer;
import org.openstack4j.openstack.storage.object.domain.SwiftContainerImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/** last_modified of the container listing. */
@Test(suiteName = "ObjectStorage/ContainerLastModified")
public class SwiftContainerLastModifiedTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.OBJECT_STORAGE;
    }

    public void listCarriesLastModified() throws Exception {
        respondWith(200, "[{\"name\":\"c\",\"count\":1,\"bytes\":2,\"last_modified\":\"2016-04-29T16:23:50.460230\"}]");

        List<? extends SwiftContainer> containers = osv3().objectStorage().containers().list();

        takeRequest();
        Assert.assertEquals(containers.size(), 1);
        Assert.assertEquals(containers.get(0).getName(), "c");
        Assert.assertEquals(containers.get(0).getObjectCount(), 1);
        Assert.assertEquals(containers.get(0).getTotalSize(), 2L);
        // 2016-04-29T16:23:50.460Z
        Assert.assertEquals(containers.get(0).getLastModified().getTime(), 1461947030460L);
    }

    public void listWithoutLastModifiedIsNull() throws Exception {
        respondWith(200, "[{\"name\":\"c\",\"count\":1,\"bytes\":2}]");

        List<? extends SwiftContainer> containers = osv3().objectStorage().containers().list();

        takeRequest();
        Assert.assertNull(containers.get(0).getLastModified());
    }

    public void notSerialized() throws Exception {
        // getMetadata() is a lazy server call, so the serialized properties are inspected instead of writing a value
        com.fasterxml.jackson.databind.ObjectMapper mapper = ObjectMapperSingleton.getContext(SwiftContainerImpl.class);
        for (com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition property
                : mapper.getSerializationConfig().introspect(mapper.constructType(SwiftContainerImpl.class)).findProperties()) {
            if (property.couldSerialize()) {
                String normalized = property.getName().replace("_", "").toLowerCase(java.util.Locale.ROOT);
                Assert.assertNotEquals(normalized, "lastmodified", "last_modified is serialized");
            }
        }
        SwiftContainerImpl container = mapper.readValue(
                "{\"name\":\"c\",\"count\":1,\"bytes\":2,\"last_modified\":\"2016-04-29T16:23:50.460230\"}", SwiftContainerImpl.class);
        Assert.assertNotNull(container.getLastModified());
    }
}
