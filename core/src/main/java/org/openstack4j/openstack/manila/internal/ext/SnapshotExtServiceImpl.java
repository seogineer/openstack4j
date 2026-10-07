package org.openstack4j.openstack.manila.internal.ext;

import static org.openstack4j.openstack.manila.internal.ManilaMicroVersions.V;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.manila.ext.SnapshotExtService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareSnapshotInfo;
import org.openstack4j.model.manila.ext.SnapshotInstance;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareSnapshotInfo;
import org.openstack4j.openstack.manila.domain.ext.ManilaSnapshotInstance;
import org.openstack4j.openstack.manila.domain.ext.ManilaSnapshotInstance.ManilaSnapshotInstanceList;

public class SnapshotExtServiceImpl extends BaseManilaExtService implements SnapshotExtService {

    private static final MicroVersion METADATA = V(73);
    private static final MicroVersion MANAGE = V(12);
    private static final MicroVersion INSTANCES = V(19);

    private static String metadata(String snapshotId) {
        return "/snapshots/" + id(snapshotId) + "/metadata";
    }

    @Override
    public Map<String, String> getMetadata(String snapshotId) {
        return metadataOf(METADATA, metadata(snapshotId));
    }

    @Override
    public String getMetadataItem(String snapshotId, String key) {
        return metadataItem(METADATA, metadata(snapshotId), key);
    }

    @Override
    public Map<String, String> setMetadata(String snapshotId, Map<String, String> metadata) {
        return writeMetadata(METADATA, metadata(snapshotId), metadata, false);
    }

    @Override
    public Map<String, String> replaceMetadata(String snapshotId, Map<String, String> metadata) {
        return writeMetadata(METADATA, metadata(snapshotId), metadata, true);
    }

    @Override
    public ActionResponse deleteMetadataItem(String snapshotId, String key) {
        return remove(METADATA, metadata(snapshotId) + "/" + id(key));
    }

    @Override
    public ShareSnapshotInfo manage(Map<String, ?> snapshot) {
        return at(MANAGE, post(ManilaShareSnapshotInfo.class, "/snapshots/manage"), "/snapshots/manage")
                .entity(JsonBody.of("snapshot", Objects.requireNonNull(snapshot, "snapshot"))).execute(propagate404());
    }

    @Override
    public ActionResponse unmanage(String snapshotId) {
        return action(MANAGE, "/snapshots/" + id(snapshotId), "unmanage", null);
    }

    @Override
    public List<? extends SnapshotInstance> listInstances(Map<String, String> filters) {
        return listOf(INSTANCES, ManilaSnapshotInstanceList.class, "/snapshot-instances", filters);
    }

    @Override
    public List<? extends SnapshotInstance> listInstancesDetail(Map<String, String> filters) {
        return listOf(INSTANCES, ManilaSnapshotInstanceList.class, "/snapshot-instances/detail", filters);
    }

    @Override
    public SnapshotInstance getInstance(String snapshotInstanceId) {
        return show(INSTANCES, ManilaSnapshotInstance.class, "/snapshot-instances/" + id(snapshotInstanceId));
    }

    @Override
    public ActionResponse resetInstanceStatus(String snapshotInstanceId, String status) {
        return action(INSTANCES, "/snapshot-instances/" + id(snapshotInstanceId), "reset_status", Map.of("status", Objects.requireNonNull(status, "status")));
    }
}
