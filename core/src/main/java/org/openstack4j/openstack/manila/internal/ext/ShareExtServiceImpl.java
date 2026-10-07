package org.openstack4j.openstack.manila.internal.ext;

import static org.openstack4j.openstack.manila.internal.ManilaMicroVersions.V;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.manila.ext.ShareExtService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.Access;
import org.openstack4j.model.manila.Share;
import org.openstack4j.model.manila.ShareInstance;
import org.openstack4j.model.manila.ext.ExportLocation;
import org.openstack4j.model.manila.ext.ShareAccessRule;
import org.openstack4j.model.manila.ext.options.ShareAccessCreate;
import org.openstack4j.model.manila.ext.options.ShareMigration;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.manila.domain.ManilaAccess;
import org.openstack4j.openstack.manila.domain.ManilaShare;
import org.openstack4j.openstack.manila.domain.ManilaShareInstance;
import org.openstack4j.openstack.manila.domain.ext.ManilaExportLocation;
import org.openstack4j.openstack.manila.domain.ext.ManilaExportLocation.ManilaExportLocationList;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareAccessRule;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareAccessRule.ManilaShareAccessRuleList;

public class ShareExtServiceImpl extends BaseManilaExtService implements ShareExtService {

    private static final MicroVersion MIGRATION_STABLE = V(96);

    private static String share(String shareId) {
        return "/shares/" + id(shareId);
    }

    private static String location(String shareId, String exportLocationId) {
        return share(shareId) + "/export_locations/" + id(exportLocationId);
    }

    @Override
    public List<? extends ExportLocation> listExportLocations(String shareId) {
        return listOf(V(9), ManilaExportLocationList.class, share(shareId) + "/export_locations", null);
    }

    @Override
    public ExportLocation getExportLocation(String shareId, String exportLocationId) {
        return show(V(9), ManilaExportLocation.class, location(shareId, exportLocationId));
    }

    @Override
    public Map<String, String> getExportLocationMetadata(String shareId, String exportLocationId) {
        return metadataOf(V(87), location(shareId, exportLocationId) + "/metadata");
    }

    @Override
    public String getExportLocationMetadataItem(String shareId, String exportLocationId, String key) {
        return metadataItem(V(87), location(shareId, exportLocationId) + "/metadata", key);
    }

    @Override
    public Map<String, String> setExportLocationMetadata(String shareId, String exportLocationId, Map<String, String> metadata) {
        return writeMetadata(V(87), location(shareId, exportLocationId) + "/metadata", metadata, false);
    }

    @Override
    public Map<String, String> replaceExportLocationMetadata(String shareId, String exportLocationId, Map<String, String> metadata) {
        return writeMetadata(V(87), location(shareId, exportLocationId) + "/metadata", metadata, true);
    }

    @Override
    public ActionResponse deleteExportLocationMetadataItem(String shareId, String exportLocationId, String key) {
        return remove(V(87), location(shareId, exportLocationId) + "/metadata/" + id(key));
    }

    @Override
    public List<? extends ExportLocation> listInstanceExportLocations(String shareInstanceId) {
        return listOf(V(9), ManilaExportLocationList.class, "/share_instances/" + id(shareInstanceId) + "/export_locations", null);
    }

    @Override
    public List<? extends ShareInstance> listInstances(String shareId) {
        return listOf(V(7), ManilaShareInstance.ShareInstances.class, share(shareId) + "/instances", null);
    }

    @Override
    public Share manage(Map<String, ?> share) {
        return at(V(7), post(ManilaShare.class, "/shares/manage"), "/shares/manage").entity(JsonBody.of("share", Objects.requireNonNull(share, "share")))
                .execute(propagate404());
    }

    @Override
    public ActionResponse unmanage(String shareId) {
        return action(V(7), share(shareId), "unmanage", null);
    }

    @Override
    public ActionResponse revertToSnapshot(String shareId, String snapshotId) {
        return action(V(27), share(shareId), "revert", Map.of("snapshot_id", Objects.requireNonNull(snapshotId, "snapshotId")));
    }

    @Override
    public ActionResponse softDelete(String shareId) {
        return action(V(69), share(shareId), "soft_delete", null);
    }

    @Override
    public ActionResponse restore(String shareId) {
        return action(V(69), share(shareId), "restore", null);
    }

    @Override
    public Access grantAccess(String shareId, ShareAccessCreate access) {
        Map<String, Object> fields = Objects.requireNonNull(access, "access").toMap();
        MicroVersion floor = fields.keySet().stream().anyMatch(k -> k.startsWith("lock_")) ? V(82) : fields.containsKey("metadata") ? V(45) : V(7);
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("allow_access", fields);
        String path = share(shareId) + "/action";
        return at(floor, post(ManilaAccess.class, path), path + " allow_access").entity(JsonBody.of(wrapper)).execute(propagate404());
    }

    @Override
    public ActionResponse revokeAccess(String shareId, String accessId) {
        return action(V(7), share(shareId), "deny_access", Map.of("access_id", id(accessId)));
    }

    @Override
    public List<? extends ShareAccessRule> listAccessRules(String shareId, Map<String, String> filters) {
        Map<String, String> query = filters == null ? new HashMap<>() : new HashMap<>(filters);
        query.put("share_id", id(shareId));
        return listOf(V(45), ManilaShareAccessRuleList.class, "/share-access-rules", query);
    }

    @Override
    public ShareAccessRule getAccessRule(String accessId) {
        return show(V(45), ManilaShareAccessRule.class, "/share-access-rules/" + id(accessId));
    }

    @Override
    public ShareAccessRule updateAccessRuleLevel(String accessId, String accessLevel) {
        String path = "/share-access-rules/" + id(accessId);
        return at(V(88), put(ManilaShareAccessRule.class, path), path)
                .entity(JsonBody.of("update_access", Map.of("access_level", Objects.requireNonNull(accessLevel, "accessLevel")))).execute(propagate404());
    }

    @Override
    public Map<String, String> updateAccessRuleMetadata(String accessId, Map<String, String> metadata) {
        String path = "/share-access-rules/" + id(accessId) + "/metadata";
        return strings(at(V(45), put(Map.class, path), path).entity(JsonBody.of("metadata", Objects.requireNonNull(metadata, "metadata"))).execute(propagate404()), "metadata");
    }

    @Override
    public ActionResponse deleteAccessRuleMetadata(String accessId, String key) {
        return remove(V(45), "/share-access-rules/" + id(accessId) + "/metadata/" + id(key));
    }

    @Override
    public ActionResponse migrationStart(String shareId, ShareMigration migration) {
        return experimental(V(29), MIGRATION_STABLE, share(shareId), "migration_start", Objects.requireNonNull(migration, "migration").toMap());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> migrationProgress(String shareId) {
        String path = share(shareId) + "/action";
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("migration_get_progress", null);
        Map<String, Object> body = experimentalHeader(V(29), MIGRATION_STABLE, at(V(29), post(Map.class, path), path + " migration_get_progress"), path)
                .entity(JsonBody.of(wrapper)).execute(propagate404());
        return body == null ? new LinkedHashMap<>() : body;
    }

    @Override
    public ActionResponse migrationComplete(String shareId) {
        return experimental(V(29), MIGRATION_STABLE, share(shareId), "migration_complete", null);
    }

    @Override
    public ActionResponse migrationCancel(String shareId) {
        return experimental(V(29), MIGRATION_STABLE, share(shareId), "migration_cancel", null);
    }
}
