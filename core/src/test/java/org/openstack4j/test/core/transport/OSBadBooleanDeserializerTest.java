package org.openstack4j.test.core.transport;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.openstack4j.core.transport.internal.OSBadBooleanDeserializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class OSBadBooleanDeserializerTest {

    static class Holder {
        @JsonDeserialize(using = OSBadBooleanDeserializer.class)
        public Boolean enabled;
    }

    private static Boolean read(String value) throws Exception {
        return new ObjectMapper().readValue("{\"enabled\":" + value + "}", Holder.class).enabled;
    }

    @Test
    public void acceptsBooleansNumbersAndStrings() throws Exception {
        Assert.assertEquals(read("true"), Boolean.TRUE);
        Assert.assertEquals(read("0"), Boolean.FALSE);
        Assert.assertEquals(read("1"), Boolean.TRUE);
        Assert.assertEquals(read("\"False\""), Boolean.FALSE);
        Assert.assertNull(read("\"\""));
    }

    @Test(expectedExceptions = JsonMappingException.class)
    public void rejectsUnexpectedTokens() throws Exception {
        read("[true]");
    }
}
