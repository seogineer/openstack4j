package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ExportLocation;
import org.openstack4j.model.manila.ext.ShareReplica;
import org.openstack4j.model.manila.ext.options.ShareReplicaCreate;

/** Share replicas ({@code /v2/share-replicas}, microversion 2.56). */
public interface ShareReplicaService extends RestService {

    /** @return the replicas of a share, or of all shares when {@code shareId} is {@code null} */
    List<? extends ShareReplica> list(String shareId);

    /** @return the replicas with all fields, of one share or all when {@code shareId} is {@code null} */
    List<? extends ShareReplica> listDetail(String shareId);

    /** @return the replica, or {@code null} when it does not exist */
    ShareReplica get(String replicaId);

    ShareReplica create(ShareReplicaCreate create);

    ActionResponse delete(String replicaId);

    /** Makes the replica active; {@code quiesceWaitTime} seconds (2.75) may be {@code null}. */
    ActionResponse promote(String replicaId, Integer quiesceWaitTime);

    ActionResponse resync(String replicaId);

    /** @param status e.g. {@code available}, {@code error} (admin) */
    ActionResponse resetStatus(String replicaId, String status);

    /** @param replicaState {@code active}, {@code in_sync}, {@code out_of_sync} or {@code error} (admin) */
    ActionResponse resetReplicaState(String replicaId, String replicaState);

    ActionResponse forceDelete(String replicaId);

    /** @return the export locations of a replica; a missing replica raises */
    List<? extends ExportLocation> listExportLocations(String replicaId);

    /** @return the export location, or {@code null} when it does not exist */
    ExportLocation getExportLocation(String replicaId, String exportLocationId);

    /** @return the replica's metadata (2.95); a missing replica raises */
    Map<String, String> getMetadata(String replicaId);

    /** @return one metadata value, or {@code null} (2.95) */
    String getMetadataItem(String replicaId, String key);

    /** Adds or changes metadata items (2.95) and returns all of them. */
    Map<String, String> setMetadata(String replicaId, Map<String, String> metadata);

    /** Replaces all metadata (2.95) and returns it. */
    Map<String, String> replaceMetadata(String replicaId, Map<String, String> metadata);

    ActionResponse deleteMetadataItem(String replicaId, String key);
}
