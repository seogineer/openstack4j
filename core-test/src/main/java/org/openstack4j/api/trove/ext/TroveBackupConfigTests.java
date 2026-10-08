package org.openstack4j.api.trove.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.trove.ext.Backup;
import org.openstack4j.model.trove.ext.Configuration;
import org.openstack4j.model.trove.ext.options.BackupOptions;
import org.openstack4j.model.trove.ext.options.ConfigurationOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Trove/BackupsConfigurations")
public class TroveBackupConfigTests extends AbstractTroveExtTest {

    private static final String P = "/v1.0/26decac97b67478f9f64ff2c2c1b778e";
    private static final String BACKUP = "{\"created\": \"2014-10-30T12:30:00\", \"datastore\": {\"type\": \"mysql\", \"version\": \"5.5\"}, \"description\": \"My Backup\","
            + " \"id\": \"b1\", \"instance_id\": \"i1\", \"locationRef\": null, \"name\": \"snapshot\", \"parent_id\": null, \"size\": 0.14, \"status\": \"NEW\", \"storage_driver\": \"cinder\"}";
    private static final String CONFIG = "{\"id\": \"cfg1\", \"name\": \"group1\", \"datastore_name\": \"mysql\", \"datastore_version_name\": \"mysql-5.7\","
            + " \"instance_count\": 0, \"values\": {\"connect_timeout\": 200}}";

    public void backupsAndStrategies() throws Exception {
        respondWith(202, "{\"backup\": " + BACKUP + "}");
        respondWith(200, "{\"backups\": [" + BACKUP + "]}");
        respondWith(200, "{\"backup\": " + BACKUP + "}");
        respondWith(202);
        respondWith(202, "{\"backup_strategy\": {\"project_id\": \"p1\", \"instance_id\": \"i1\", \"backend\": \"swift\", \"swift_container\": \"my_trove_backups\"}}");
        respondWith(200, "{\"backup_strategies\": [{\"backend\": \"swift\", \"instance_id\": \"i1\", \"swift_container\": \"my_trove_backups\"}]}");
        respondWith(202);

        var trove = osv3().trove();
        Backup created = trove.backups().create(BackupOptions.create("snapshot").instance("i1").description("My Backup").storageDriver("cinder"));
        List<? extends Backup> all = trove.backups().list(Map.of("instance_id", "i1"));
        trove.backups().get("b1");
        Assert.assertTrue(trove.backups().delete("b1").isSuccess());
        Map<String, Object> strategy = trove.backupStrategies().create("i1", "my_trove_backups");
        trove.backupStrategies().list(Map.of("instance_id", "i1"));
        Assert.assertTrue(trove.backupStrategies().delete("i1").isSuccess());

        Assert.assertEquals(body(expect("POST", P + "/backups")).toString(), "{\"backup\":{\"name\":\"snapshot\",\"instance\":\"i1\",\"description\":\"My Backup\",\"storage_driver\":\"cinder\"}}");
        expect("GET", P + "/backups?instance_id=i1");
        expect("GET", P + "/backups/b1");
        expect("DELETE", P + "/backups/b1");
        Assert.assertEquals(body(expect("POST", P + "/backup_strategies")).toString(), "{\"backup_strategy\":{\"instance_id\":\"i1\",\"swift_container\":\"my_trove_backups\"}}");
        expect("GET", P + "/backup_strategies?instance_id=i1");
        expect("DELETE", P + "/backup_strategies?instance_id=i1");
        Assert.assertEquals(created.getStatus(), "NEW");
        Assert.assertEquals(all.get(0).getSize(), Double.valueOf(0.14));
        Assert.assertEquals(created.getDatastore().get("type"), "mysql");
        Assert.assertEquals(strategy.get("backend"), "swift");
    }

    public void configurationGroups() throws Exception {
        respondWith(200, "{\"configuration\": " + CONFIG + "}");
        respondWith(200, "{\"configurations\": [" + CONFIG + "]}");
        respondWith(200, "{\"configuration\": " + CONFIG + "}");
        respondWith(200);
        respondWith(202);
        respondWith(200, "{\"instances\": [{\"id\": \"i1\", \"name\": \"master_1\"}]}");
        respondWith(202);

        var groups = osv3().trove().configurations();
        Configuration created = groups.create(ConfigurationOptions.create("group1", Map.of("connect_timeout", 200)).datastore(Map.of("type", "mysql", "version", "mysql-5.7")));
        groups.list();
        groups.get("cfg1");
        Assert.assertTrue(groups.update("cfg1", ConfigurationOptions.update().name("new_name").values(Map.of("connect_timeout", 18))).isSuccess());
        try {
            groups.update("cfg1", ConfigurationOptions.update().name("only_name"));
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // PUT without values would empty the group
        }
        Assert.assertTrue(groups.patchValues("cfg1", Map.of("connect_timeout", 17)).isSuccess());
        List<Map<String, Object>> instances = groups.listInstances("cfg1");
        Assert.assertTrue(groups.delete("cfg1").isSuccess());

        var create = body(expect("POST", P + "/configurations")).get("configuration");
        Assert.assertEquals(create.get("values").get("connect_timeout").asInt(), 200);
        Assert.assertEquals(create.get("datastore").get("type").asText(), "mysql");
        expect("GET", P + "/configurations");
        expect("GET", P + "/configurations/cfg1");
        Assert.assertEquals(body(expect("PUT", P + "/configurations/cfg1")).toString(), "{\"configuration\":{\"name\":\"new_name\",\"values\":{\"connect_timeout\":18}}}");
        Assert.assertEquals(body(expect("PATCH", P + "/configurations/cfg1")).toString(), "{\"configuration\":{\"values\":{\"connect_timeout\":17}}}");
        expect("GET", P + "/configurations/cfg1/instances");
        expect("DELETE", P + "/configurations/cfg1");
        Assert.assertEquals(created.getDatastoreVersionName(), "mysql-5.7");
        Assert.assertEquals(created.getValues().get("connect_timeout"), 200);
        Assert.assertEquals(instances.get(0).get("name"), "master_1");
    }
}
