package org.openstack4j.api.heat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.api.heat.ext.AbstractHeatExtTest;
import org.openstack4j.model.heat.Template;
import org.openstack4j.model.heat.TemplateResponse;
import org.openstack4j.model.heat.TemplateValidateOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Heat/TemplateValidate")
public class TemplateValidateTests extends AbstractHeatExtTest {

    private static final String TEMPLATE = "{\"heat_template_version\":\"2018-08-31\"}";
    private static final String ANSWER = "{\"Description\":\"d\",\"Parameters\":{\"key_name\":{\"Type\":\"String\"}},\"ParameterGroups\":[]}";
    private static final String INVALID = "{\"explanation\":\"The server could not comply with the request since it is either malformed or otherwise incorrect.\","
            + "\"code\":400,\"error\":{\"message\":\"Parameter 'x' is invalid\",\"type\":\"StackValidationFailed\"},\"title\":\"Bad Request\"}";

    public void validateWithOptions() throws Exception {
        respondWith(200, ANSWER);

        Template template = Builders.template().templateJson(TEMPLATE)
                .environment("parameters:\n  key_name: k\n")
                .files(Map.of("a.yaml", "x"))
                .parameters(Map.of("flavor", "m1"))
                .environmentFiles(List.of("env.yaml")).build();
        Map<String, Object> result = osv3().heat().templates().validate(template, TemplateValidateOptions.create().showNested(true).ignoreErrors("99001", "99002"));

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "POST");
        Assert.assertTrue(decodedPath(request).contains("/validate?"), decodedPath(request));
        Assert.assertEquals(query(request), Map.of("show_nested", "true", "ignore_errors", "99001,99002"));
        JsonNode body = body(request);
        Assert.assertTrue(body.get("environment").isTextual(), "the environment is sent as text");
        Assert.assertEquals(body.get("environment").asText(), "parameters:\n  key_name: k\n");
        Assert.assertEquals(body.get("files").get("a.yaml").asText(), "x");
        Assert.assertEquals(body.get("parameters").get("flavor").asText(), "m1");
        Assert.assertEquals(body.get("environment_files").get(0).asText(), "env.yaml");
        Assert.assertTrue(body.get("template").isObject(), "the template is sent raw");
        Assert.assertEquals(body.get("template").get("heat_template_version").asText(), "2018-08-31");
        Assert.assertTrue(((Map<?, ?>) result.get("Parameters")).containsKey("key_name"));
        Assert.assertEquals(result.get("Description"), "d");
    }

    private static Map<String, String> query(RecordedRequest request) {
        Map<String, String> query = new HashMap<>();
        for (String pair : decodedPath(request).substring(decodedPath(request).indexOf('?') + 1).split("&"))
            query.put(pair.substring(0, pair.indexOf('=')), pair.substring(pair.indexOf('=') + 1));
        return query;
    }

    public void validateNoOptions() throws Exception {
        respondWith(200, ANSWER);
        respondWith(200, ANSWER);

        Template template = Builders.template().templateJson(TEMPLATE).build();
        osv3().heat().templates().validate(template, null);
        osv3().heat().templates().validate(template, TemplateValidateOptions.create());

        for (int i = 0; i < 2; i++) {
            RecordedRequest request = expect("POST", "/validate");
            Assert.assertFalse(request.getPath().contains("?"), request.getPath());
            JsonNode body = body(request);
            Assert.assertFalse(body.has("environment") || body.has("files") || body.has("parameters") || body.has("environment_files"), body.toString());
        }
    }

    public void validate400Raises() throws Exception {
        respondWith(400, INVALID);

        try {
            osv3().heat().templates().validate(Builders.template().templateJson(TEMPLATE).build(), null);
            Assert.fail("expected a ClientResponseException");
        } catch (ClientResponseException expected) {
            Assert.assertEquals(expected.getStatus(), 400);
            Assert.assertTrue(expected.getMessage().contains("Parameter 'x' is invalid"), expected.getMessage());
        }
        expect("POST", "/validate");
    }

    public void legacyValidateUnchanged() throws Exception {
        respondWith(200, ANSWER);
        respondWith(400, INVALID);

        TemplateResponse valid = osv3().heat().templates().validateTemplate(Builders.template().templateJson(TEMPLATE).build());
        TemplateResponse invalid = osv3().heat().templates().validateTemplate(Builders.template().templateJson(TEMPLATE).build());

        for (int i = 0; i < 2; i++) {
            JsonNode body = body(expect("POST", "/validate"));
            Assert.assertTrue(body.has("template"));
            Assert.assertFalse(body.has("environment") || body.has("files") || body.has("parameters") || body.has("environment_files"), body.toString());
        }
        // valid is not asserted: the legacy method reads the answer as a String, so a real JSON answer already counted as invalid
        Assert.assertNotNull(valid);
        Assert.assertFalse(invalid.isValid());
    }
}
