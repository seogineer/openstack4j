package org.openstack4j.test.heat;

import java.net.URL;

import org.openstack4j.openstack.heat.utils.Environment;
import org.openstack4j.openstack.heat.utils.Template;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HeatYamlParsingTest {

    private static URL resource(String name) {
        return HeatYamlParsingTest.class.getResource("/heat/" + name);
    }

    @Test
    public void templateResolvesGetFileAndNestedTemplate() throws Exception {
        Template template = new Template(resource("template.yaml"));

        Assert.assertTrue(template.getFiles().get("script.sh").contains("echo hello"));
        Assert.assertTrue(template.getFiles().get("nested.yaml").contains("heat_template_version"));
    }

    @Test
    public void environmentResolvesResourceRegistryTemplates() throws Exception {
        Environment environment = new Environment(resource("env.yaml"));

        Assert.assertEquals(environment.getFiles().size(), 1);
        Assert.assertTrue(environment.getFiles().values().iterator().next().contains("heat_template_version"));
    }
}
