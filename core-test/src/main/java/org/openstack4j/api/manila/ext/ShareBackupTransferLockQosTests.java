package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.manila.ext.QosType;
import org.openstack4j.model.manila.ext.ResourceLock;
import org.openstack4j.model.manila.ext.ShareBackup;
import org.openstack4j.model.manila.ext.ShareTransfer;
import org.openstack4j.model.manila.ext.options.QosTypeCreate;
import org.openstack4j.model.manila.ext.options.QosTypeUpdate;
import org.openstack4j.model.manila.ext.options.ResourceLockCreate;
import org.openstack4j.model.manila.ext.options.ResourceLockUpdate;
import org.openstack4j.model.manila.ext.options.ShareBackupCreate;
import org.openstack4j.model.manila.ext.options.ShareBackupUpdate;
import org.openstack4j.model.manila.ext.options.ShareTransferCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/BackupsTransfersLocksQos")
public class ShareBackupTransferLockQosTests extends AbstractManilaExtTest {

    private static final String P = "/v2/b80f8d4e28b74188858b654cb1fccf7d";
    private static final String BACKUP = "{\"id\": \"b1\", \"share_id\": \"s1\", \"status\": \"creating\", \"name\": \"backup1\", \"size\": 1, \"progress\": \"0\"}";

    public void backupsAreExperimental() throws Exception {
        respondWith(202, "{\"share_backup\": " + BACKUP + "}");
        respondWith(200, "{\"share_backups\": [" + BACKUP + "]}");
        respondWith(200, "{\"share_backup\": " + BACKUP + "}");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);

        var backups = osv3().share().shareBackups();
        ShareBackup created = backups.create(ShareBackupCreate.create("s1").name("backup1"));
        List<? extends ShareBackup> all = backups.list(Map.of("share_id", "s1"));
        backups.update("b1", ShareBackupUpdate.create().name("backup2"));
        Assert.assertTrue(backups.restore("b1").isSuccess());
        Assert.assertTrue(backups.restore("b1", "s9").isSuccess());
        Assert.assertTrue(backups.resetStatus("b1", "error").isSuccess());
        Assert.assertTrue(backups.delete("b1").isSuccess());

        RecordedRequest create = expect("POST", P + "/share-backups");
        Assert.assertEquals(create.getHeader("X-OpenStack-Manila-API-Version"), "2.80");
        Assert.assertEquals(create.getHeader("X-OpenStack-Manila-API-Experimental"), "True");
        Assert.assertEquals(body(create).toString(), "{\"share_backup\":{\"share_id\":\"s1\",\"name\":\"backup1\"}}");
        Assert.assertEquals(expect("GET", P + "/share-backups/detail?share_id=s1").getHeader("X-OpenStack-Manila-API-Experimental"), "True");
        Assert.assertEquals(body(expect("PUT", P + "/share-backups/b1")).toString(), "{\"share_backup\":{\"name\":\"backup2\"}}");
        Assert.assertEquals(body(expect("POST", P + "/share-backups/b1/action")).toString(), "{\"restore\":null}");
        RecordedRequest targeted = expect("POST", P + "/share-backups/b1/action");
        Assert.assertEquals(body(targeted).toString(), "{\"restore\":\"s9\"}");
        Assert.assertEquals(targeted.getHeader("X-OpenStack-Manila-API-Version"), "2.91");
        Assert.assertEquals(targeted.getHeader("X-OpenStack-Manila-API-Experimental"), "True");
        Assert.assertEquals(body(expect("POST", P + "/share-backups/b1/action")).toString(), "{\"reset_status\":{\"status\":\"error\"}}");
        Assert.assertEquals(expect("DELETE", P + "/share-backups/b1").getHeader("X-OpenStack-Manila-API-Experimental"), "True");
        Assert.assertEquals(created.getProgress(), "0");
        Assert.assertEquals(all.get(0).getShareId(), "s1");
    }

    public void transfers() throws Exception {
        String transfer = "{\"id\": \"t1\", \"name\": \"test_transfer\", \"resource_type\": \"share\", \"resource_id\": \"s1\", \"auth_key\": \"406a2d67cdb09afe\","
                + " \"accepted\": false, \"expires_at\": \"2022-09-06T08:22:43.629495\"}";
        respondWith(202, "{\"transfer\": " + transfer + "}");
        respondWith(200, "{\"transfers\": [" + transfer + "]}");
        respondWith(202);
        respondWith(200);

        ShareTransfer created = osv3().share().shareTransfers().create(ShareTransferCreate.create("s1").name("test_transfer"));
        osv3().share().shareTransfers().list();
        Assert.assertTrue(osv3().share().shareTransfers().accept("t1", created.getAuthKey(), true).isSuccess());
        Assert.assertTrue(osv3().share().shareTransfers().delete("t1").isSuccess());

        RecordedRequest create = expect("POST", P + "/share-transfers");
        Assert.assertEquals(body(create).toString(), "{\"transfer\":{\"share_id\":\"s1\",\"name\":\"test_transfer\"}}");
        Assert.assertEquals(create.getHeader("X-OpenStack-Manila-API-Version"), "2.77");
        expect("GET", P + "/share-transfers/detail");
        Assert.assertEquals(body(expect("POST", P + "/share-transfers/t1/accept")).toString(), "{\"accept\":{\"auth_key\":\"406a2d67cdb09afe\",\"clear_access_rules\":true}}");
        expect("DELETE", P + "/share-transfers/t1");
        Assert.assertEquals(created.getResourceId(), "s1");
    }

    public void resourceLocksAndQosTypes() throws Exception {
        String lock = "{\"id\": \"l1\", \"resource_type\": \"share\", \"resource_id\": \"s1\", \"resource_action\": \"delete\", \"lock_reason\": \"audit\", \"lock_context\": \"user\"}";
        String qos = "{\"id\": \"q1\", \"name\": \"testing\", \"description\": \"d\", \"specs\": {\"peak_iops\": 5000}}";
        respondWith(200, "{\"resource_lock\": " + lock + "}");
        respondWith(200, "{\"resource_lock\": " + lock + "}");
        respondWith(200, "{\"resource_locks\": [" + lock + "]}");
        respondWith(202, "{\"qos_type\": " + qos + "}");
        respondWith(200, "{\"qos_type\": " + qos + "}");
        respondWith(200, "{\"specs\": {\"peak_iops\": \"5000\"}}");
        respondWith(200, "{\"specs\": {\"k\": \"v\"}}");
        respondWith(202);
        respondWith(202);

        ResourceLock created = osv3().share().resourceLocks().create(ResourceLockCreate.create("s1").resourceAction("delete").lockReason("audit"));
        osv3().share().resourceLocks().update("l1", ResourceLockUpdate.create().lockReason("audit"));
        osv3().share().resourceLocks().list(Map.of("resource_id", "s1"));
        QosType type = osv3().share().qosTypes().create(QosTypeCreate.create("testing").specs(Map.of("peak_iops", 5000)));
        osv3().share().qosTypes().update("q1", QosTypeUpdate.create().description("d"));
        Map<String, String> specs = osv3().share().qosTypes().getSpecs("q1");
        osv3().share().qosTypes().setSpecs("q1", Map.of("k", "v"));
        Assert.assertTrue(osv3().share().qosTypes().unsetSpec("q1", "k").isSuccess());
        Assert.assertTrue(osv3().share().qosTypes().delete("q1").isSuccess());

        RecordedRequest lockCreate = expect("POST", P + "/resource-locks");
        Assert.assertEquals(lockCreate.getHeader("X-OpenStack-Manila-API-Version"), "2.81");
        Assert.assertEquals(body(lockCreate).toString(), "{\"resource_lock\":{\"resource_id\":\"s1\",\"resource_action\":\"delete\",\"lock_reason\":\"audit\"}}");
        Assert.assertEquals(body(expect("PUT", P + "/resource-locks/l1")).toString(), "{\"resource_lock\":{\"lock_reason\":\"audit\"}}");
        expect("GET", P + "/resource-locks?resource_id=s1");
        RecordedRequest qosCreate = expect("POST", P + "/qos-types");
        Assert.assertEquals(qosCreate.getHeader("X-OpenStack-Manila-API-Version"), "2.94");
        Assert.assertEquals(body(qosCreate).toString(), "{\"qos_type\":{\"name\":\"testing\",\"specs\":{\"peak_iops\":5000}}}");
        expect("PUT", P + "/qos-types/q1");
        expect("GET", P + "/qos-types/q1/specs");
        Assert.assertEquals(body(expect("POST", P + "/qos-types/q1/specs")).toString(), "{\"specs\":{\"k\":\"v\"}}");
        expect("DELETE", P + "/qos-types/q1/specs/k");
        expect("DELETE", P + "/qos-types/q1");
        Assert.assertEquals(created.getLockContext(), "user");
        Assert.assertEquals(type.getSpecs().get("peak_iops"), 5000);
        Assert.assertEquals(specs.get("peak_iops"), "5000");
    }
}
