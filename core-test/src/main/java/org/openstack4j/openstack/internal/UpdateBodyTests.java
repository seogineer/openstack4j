package org.openstack4j.openstack.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.ext.PortPair;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests of {@link UpdateBody}: no server involved. */
@Test(suiteName = "Internal/UpdateBody")
public class UpdateBodyTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A model with renamed properties, null fields, a list and a nested map. */
    public static class Sample {
        @JsonProperty("id")
        public String id = "i1";
        @JsonProperty("tenant_id")
        public String tenantId = "t1";
        @JsonProperty("name")
        public String name = "n";
        @JsonProperty("description")
        public String description = null;
        @JsonProperty("port_pairs")
        public List<String> portPairs = Arrays.asList("a", "b");
        @JsonProperty("params")
        public Map<String, String> params = new LinkedHashMap<>();
    }

    private static JsonNode json(JsonBody body) {
        return MAPPER.valueToTree(body);
    }

    public void wrapsInRootAndKeepsOtherFields() {
        JsonNode node = json(UpdateBody.of("sample", new Sample(), "id"));
        Assert.assertEquals(node.size(), 1, node.toString());
        JsonNode inner = node.get("sample");
        Assert.assertNotNull(inner, node.toString());
        Assert.assertEquals(inner.get("name").asText(), "n");
        Assert.assertEquals(inner.get("tenant_id").asText(), "t1");
        Assert.assertEquals(inner.get("port_pairs").size(), 2);
        Assert.assertTrue(inner.has("params"));
    }

    public void removesOnlyTheNamedKeys() {
        JsonNode inner = json(UpdateBody.of("sample", new Sample(), "id", "tenant_id")).get("sample");
        Assert.assertFalse(inner.has("id"), inner.toString());
        Assert.assertFalse(inner.has("tenant_id"), inner.toString());
        Assert.assertTrue(inner.has("name"), inner.toString());
        Assert.assertTrue(inner.has("port_pairs"), inner.toString());
    }

    public void noKeysRemovesNothing() {
        JsonNode inner = json(UpdateBody.of("sample", new Sample())).get("sample");
        Assert.assertTrue(inner.has("id"));
        Assert.assertTrue(inner.has("tenant_id"));
        Assert.assertTrue(inner.has("name"));
    }

    public void removingAbsentKeyIsHarmless() {
        JsonNode with = json(UpdateBody.of("sample", new Sample(), "does_not_exist", "description")).get("sample");
        JsonNode without = json(UpdateBody.of("sample", new Sample())).get("sample");
        Assert.assertEquals(with, without);
    }

    public void nullFieldsAreOmittedLikeOnCreate() {
        JsonNode inner = json(UpdateBody.of("sample", new Sample(), "id")).get("sample");
        Assert.assertFalse(inner.has("description"), "null field must not be sent as null: " + inner);
    }

    public void nullsInsideNestedMapsAreOmittedLikeOnCreate() throws Exception {
        Sample s = new Sample();
        s.params.put("keep", "v");
        s.params.put("gone", null);
        JsonNode inner = json(UpdateBody.of("sample", s)).get("sample");
        // create path: the client mapper is NON_NULL, which also drops null map values
        ObjectMapper create = new ObjectMapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        JsonNode created = create.readTree(create.writeValueAsString(s));
        Assert.assertEquals(inner, created);
        Assert.assertFalse(inner.get("params").has("gone"), inner.toString());
    }

    public void inputObjectIsNotModified() throws Exception {
        Sample s = new Sample();
        String before = new ObjectMapper().writeValueAsString(s);
        UpdateBody.of("sample", s, "id", "tenant_id", "name", "port_pairs");
        Assert.assertEquals(new ObjectMapper().writeValueAsString(s), before);
        Assert.assertEquals(s.id, "i1");
        Assert.assertEquals(s.tenantId, "t1");
        Assert.assertEquals(s.portPairs, Arrays.asList("a", "b"));
    }

    public void repeatedCallsGiveEqualBodies() {
        Sample s = new Sample();
        Assert.assertEquals(json(UpdateBody.of("sample", s, "id")), json(UpdateBody.of("sample", s, "id")));
    }

    public void realModelUsesJsonNamesNotJavaNames() {
        PortPair pair = Builders.portPair().name("x").description("y").ingressId("in1").egressId("out1")
                .serviceFunctionParameters(Collections.singletonMap("a", "b")).build();
        JsonNode inner = json(UpdateBody.of("port_pair", pair, "ingress", "egress")).get("port_pair");
        Assert.assertEquals(inner.get("name").asText(), "x");
        Assert.assertEquals(inner.get("service_function_parameters").get("a").asText(), "b");
        Assert.assertFalse(inner.has("ingress"), inner.toString());
        Assert.assertFalse(inner.has("egress"), inner.toString());
        Assert.assertFalse(inner.has("ingressId"), inner.toString());
        Assert.assertEquals(pair.getIngressId(), "in1");
        // without the exclusion the same keys are present, so the assertion above is not vacuous
        JsonNode full = json(UpdateBody.of("port_pair", pair)).get("port_pair");
        Assert.assertTrue(full.has("ingress"), full.toString());
        Assert.assertTrue(full.has("egress"), full.toString());
    }
}
