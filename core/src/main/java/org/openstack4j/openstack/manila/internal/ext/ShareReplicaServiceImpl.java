package org.openstack4j.openstack.manila.internal.ext;

import static org.openstack4j.openstack.manila.internal.ManilaMicroVersions.V;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.manila.ext.ShareReplicaService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ExportLocation;
import org.openstack4j.model.manila.ext.ShareReplica;
import org.openstack4j.model.manila.ext.options.ShareReplicaCreate;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaExportLocation;
import org.openstack4j.openstack.manila.domain.ext.ManilaExportLocation.ManilaExportLocationList;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareReplica;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareReplica.ManilaShareReplicaList;

public class ShareReplicaServiceImpl extends BaseManilaExtService implements ShareReplicaService {

    private static final MicroVersion FLOOR = V(56);
    private static final MicroVersion METADATA = V(95);
    private static final String PATH = "/share-replicas";

    private static String replica(String replicaId) {
        return PATH + "/" + id(replicaId);
    }

    @Override
    public List<? extends ShareReplica> list(String shareId) {
        return listOf(FLOOR, ManilaShareReplicaList.class, PATH, shareId == null ? null : Map.of("share_id", shareId));
    }

    @Override
    public List<? extends ShareReplica> listDetail(String shareId) {
        return listOf(FLOOR, ManilaShareReplicaList.class, PATH + "/detail", shareId == null ? null : Map.of("share_id", shareId));
    }

    @Override
    public ShareReplica get(String replicaId) {
        return show(FLOOR, ManilaShareReplica.class, replica(replicaId));
    }

    @Override
    public ShareReplica create(ShareReplicaCreate create) {
        return create(FLOOR, ManilaShareReplica.class, PATH, "share_replica", create);
    }

    @Override
    public ActionResponse delete(String replicaId) {
        return remove(FLOOR, replica(replicaId));
    }

    @Override
    public ActionResponse promote(String replicaId, Integer quiesceWaitTime) {
        if (quiesceWaitTime == null)
            return action(FLOOR, replica(replicaId), "promote", null);
        return action(V(75), replica(replicaId), "promote", Map.of("quiesce_wait_time", quiesceWaitTime));
    }

    @Override
    public ActionResponse resync(String replicaId) {
        return action(FLOOR, replica(replicaId), "resync", null);
    }

    @Override
    public ActionResponse resetStatus(String replicaId, String status) {
        return action(FLOOR, replica(replicaId), "reset_status", Map.of("status", Objects.requireNonNull(status, "status")));
    }

    @Override
    public ActionResponse resetReplicaState(String replicaId, String replicaState) {
        return action(FLOOR, replica(replicaId), "reset_replica_state", Map.of("replica_state", Objects.requireNonNull(replicaState, "replicaState")));
    }

    @Override
    public ActionResponse forceDelete(String replicaId) {
        return action(FLOOR, replica(replicaId), "force_delete", null);
    }

    @Override
    public List<? extends ExportLocation> listExportLocations(String replicaId) {
        return listOf(FLOOR, ManilaExportLocationList.class, replica(replicaId) + "/export-locations", null);
    }

    @Override
    public ExportLocation getExportLocation(String replicaId, String exportLocationId) {
        return show(FLOOR, ManilaExportLocation.class, replica(replicaId) + "/export-locations/" + id(exportLocationId));
    }

    @Override
    public Map<String, String> getMetadata(String replicaId) {
        return metadataOf(METADATA, replica(replicaId) + "/metadata");
    }

    @Override
    public String getMetadataItem(String replicaId, String key) {
        return metadataItem(METADATA, replica(replicaId) + "/metadata", key);
    }

    @Override
    public Map<String, String> setMetadata(String replicaId, Map<String, String> metadata) {
        return writeMetadata(METADATA, replica(replicaId) + "/metadata", metadata, false);
    }

    @Override
    public Map<String, String> replaceMetadata(String replicaId, Map<String, String> metadata) {
        return writeMetadata(METADATA, replica(replicaId) + "/metadata", metadata, true);
    }

    @Override
    public ActionResponse deleteMetadataItem(String replicaId, String key) {
        return remove(METADATA, replica(replicaId) + "/metadata/" + id(key));
    }
}
