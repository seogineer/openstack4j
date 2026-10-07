package org.openstack4j.api.heat.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.heat.ext.HeatEngineService;
import org.openstack4j.model.heat.ext.TemplateFunction;
import org.openstack4j.model.heat.ext.TemplateVersion;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Heat/Ext/Basics")
public class HeatBasicsTests extends AbstractHeatExtTest {

    public void buildInfoAndServices() throws Exception {
        respondWith(200, "{\"api\": {\"revision\": \"23.0.0\"}, \"engine\": {\"revision\": \"23.0.0\"}}");
        respondWith(200, "{\"services\": [{\"binary\": \"heat-engine\", \"created_at\": \"2015-02-03T05:55:59.000000\", \"deleted_at\": null, \"engine_id\": \"9d9242c3\","
                + " \"host\": \"engine-1\", \"hostname\": \"mrkanag\", \"id\": \"e1908f44\", \"report_interval\": 60, \"status\": \"up\", \"topic\": \"engine\", \"updated_at\": \"2015-02-03T05:57:59.000000\"}]}");

        Map<String, Object> info = osv3().heat().info().buildInfo();
        List<? extends HeatEngineService> services = osv3().heat().info().services();

        expect("GET", "/build_info");
        expect("GET", "/services");
        Assert.assertEquals(((Map<?, ?>) info.get("api")).get("revision"), "23.0.0");
        Assert.assertEquals(services.get(0).getBinary(), "heat-engine");
        Assert.assertEquals(services.get(0).getReportInterval(), Integer.valueOf(60));
    }

    public void templateVersions() throws Exception {
        respondWith(200, "{\"template_versions\": [{\"aliases\": [], \"version\": \"heat_template_version.2013-05-23\", \"type\": \"hot\"}]}");
        respondWith(200, "{\"template_functions\": [{\"functions\": \"get_param\", \"description\": \"A function for resolving parameter references.\"}]}");

        List<? extends TemplateVersion> versions = osv3().heat().templateVersions().list();
        List<? extends TemplateFunction> functions = osv3().heat().templateVersions().functions("heat_template_version.2013-05-23");

        expect("GET", "/template_versions");
        expect("GET", "/template_versions/heat_template_version.2013-05-23/functions");
        Assert.assertEquals(versions.get(0).getType(), "hot");
        Assert.assertTrue(versions.get(0).getAliases().isEmpty());
        Assert.assertEquals(functions.get(0).getFunctions(), "get_param");
    }

    public void resourceTypes() throws Exception {
        respondWith(200, "{\"resource_types\": [\"AWS::EC2::Instance\", \"OS::Heat::RandomString\"]}");
        respondWith(200, "{\"resource_types\": [\"OS::Heat::RandomString\"]}");
        respondWith(200, "{\"resource_type\": \"OS::Heat::RandomString\", \"properties\": {\"length\": {\"type\": \"integer\"}}, \"attributes\": {\"value\": {\"type\": \"string\"}}}");
        respondWith(200, "{\"heat_template_version\": \"2016-10-14\", \"description\": \"Initial template of RandomString\", \"parameters\": {}}");
        respondWith(200, "{\"HeatTemplateFormatVersion\": \"2012-12-12\"}");

        var types = osv3().heat().resourceTypes();
        Assert.assertEquals(types.list().size(), 2);
        types.list(Map.of("name", "OS::Heat::Random*"));
        Map<String, Object> schema = types.schema("OS::Heat::RandomString");
        Map<String, Object> hot = types.template("OS::Heat::RandomString", "hot");
        types.template("OS::Heat::RandomString", null);

        expect("GET", "/resource_types");
        expect("GET", "/resource_types?name=OS::Heat::Random*");
        expect("GET", "/resource_types/OS::Heat::RandomString");
        expect("GET", "/resource_types/OS::Heat::RandomString/template?template_type=hot");
        expect("GET", "/resource_types/OS::Heat::RandomString/template");
        Assert.assertTrue(((Map<?, ?>) schema.get("properties")).containsKey("length"));
        Assert.assertEquals(hot.get("heat_template_version"), "2016-10-14");
    }
}
