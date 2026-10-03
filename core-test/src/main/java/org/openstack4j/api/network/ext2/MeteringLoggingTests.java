package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.MeteringLabel;
import org.openstack4j.model.network.ext.MeteringLabelRule;
import org.openstack4j.model.network.ext.NetworkLog;
import org.openstack4j.model.network.options.MeteringLabelOptions;
import org.openstack4j.model.network.options.MeteringLabelRuleOptions;
import org.openstack4j.model.network.options.NetworkLogOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/MeteringLogging")
public class MeteringLoggingTests extends AbstractNetworkingExtTest {

    public void metering() throws Exception {
        String label = "{\"project_id\": \"" + PROJECT + "\", \"description\": \"d\", \"name\": \"label1\", \"id\": \"ml1\", \"shared\": false}";
        String rule = "{\"remote_ip_prefix\": \"10.0.1.0/24\", \"direction\": \"ingress\", \"metering_label_id\": \"ml1\", \"id\": \"mr1\", \"excluded\": false}";
        respondWith(201, "{\"metering_label\": " + label + "}");
        respondWith(200, "{\"metering_labels\": [" + label + "]}");
        respondWith(200, "{\"metering_label\": " + label + "}");
        respondWith(201, "{\"metering_label_rule\": " + rule + "}");
        respondWith(200, "{\"metering_label_rules\": [" + rule + "]}");
        respondWith(200, "{\"metering_label_rule\": " + rule + "}");
        respondWith(204);
        respondWith(204);

        var metering = osv3().networking().metering();
        MeteringLabel created = metering.createLabel(MeteringLabelOptions.create("label1").description("d"));
        metering.listLabels();
        metering.getLabel("ml1");
        MeteringLabelRule createdRule = metering.createRule(MeteringLabelRuleOptions.create("ml1", "ingress").remoteIpPrefix("10.0.1.0/24"));
        List<? extends MeteringLabelRule> rules = metering.listRules();
        metering.getRule("mr1");
        metering.deleteRule("mr1");
        metering.deleteLabel("ml1");

        RecordedRequest createLabel = expect("POST", "/v2.0/metering/metering-labels");
        Assert.assertEquals(body(createLabel).get("metering_label").get("name").asText(), "label1");
        expect("GET", "/v2.0/metering/metering-labels");
        expect("GET", "/v2.0/metering/metering-labels/ml1");
        RecordedRequest createRule = expect("POST", "/v2.0/metering/metering-label-rules");
        Assert.assertEquals(body(createRule).get("metering_label_rule").get("metering_label_id").asText(), "ml1");
        Assert.assertFalse(body(createRule).get("metering_label_rule").has("excluded"));
        expect("GET", "/v2.0/metering/metering-label-rules");
        expect("GET", "/v2.0/metering/metering-label-rules/mr1");
        expect("DELETE", "/v2.0/metering/metering-label-rules/mr1");
        expect("DELETE", "/v2.0/metering/metering-labels/ml1");
        Assert.assertFalse(created.isShared());
        Assert.assertEquals(createdRule.getDirection(), "ingress");
        Assert.assertFalse(rules.get(0).isExcluded());
    }

    public void logging() throws Exception {
        String log = "{\"name\": \"security group log\", \"description\": \"\", \"id\": \"lg1\", \"project_id\": \"" + PROJECT + "\", \"enabled\": true, \"revision_number\": 1,"
                + " \"resource_type\": \"security_group\", \"resource_id\": null, \"target_id\": null, \"event\": \"ALL\"}";
        respondWith(201, "{\"log\": " + log + "}");
        respondWith(200, "{\"logs\": [" + log + "]}");
        respondWith(200, "{\"log\": " + log + "}");
        respondWith(200, "{\"log\": " + log + "}");
        respondWith(204);
        respondWith(200, "{\"loggable_resources\": [{\"type\": \"security_group\"}, {\"type\": \"firewall_group\"}]}");

        var logging = osv3().networking().logging();
        NetworkLog created = logging.create(NetworkLogOptions.create("security_group").name("security group log").event("ALL"));
        logging.list();
        logging.get("lg1");
        logging.update("lg1", NetworkLogOptions.update().enabled(false));
        logging.delete("lg1");
        List<String> types = logging.loggableResources();

        RecordedRequest create = expect("POST", "/v2.0/log/logs");
        Assert.assertEquals(body(create).get("log").get("resource_type").asText(), "security_group");
        expect("GET", "/v2.0/log/logs");
        expect("GET", "/v2.0/log/logs/lg1");
        Assert.assertFalse(body(expect("PUT", "/v2.0/log/logs/lg1")).get("log").get("enabled").asBoolean());
        expect("DELETE", "/v2.0/log/logs/lg1");
        expect("GET", "/v2.0/log/loggable-resources");
        Assert.assertEquals(created.getEvent(), "ALL");
        Assert.assertNull(created.getResourceId());
        Assert.assertEquals(types, List.of("security_group", "firewall_group"));
    }
}
