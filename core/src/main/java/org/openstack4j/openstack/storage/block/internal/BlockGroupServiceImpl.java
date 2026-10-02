package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.*;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;
import org.openstack4j.api.storage.BlockGroupService;
import org.openstack4j.model.storage.block.ReplicationTarget;
import org.openstack4j.model.storage.block.VolumeGroup;
import org.openstack4j.model.storage.block.options.GroupCreate;
import org.openstack4j.model.storage.block.options.GroupListOptions;
import org.openstack4j.openstack.storage.block.domain.CinderGroup.Groups;

public class BlockGroupServiceImpl extends BaseBlockStorageServices implements BlockGroupService {

    @Override public List<? extends VolumeGroup> list() { return list(GroupListOptions.create()); }
    @Override public List<? extends VolumeGroup> listDetail() { return listDetail(GroupListOptions.create()); }
    @Override public List<? extends VolumeGroup> list(GroupListOptions o) { requireMicroVersion("Groups", V(13)); return get(Groups.class, uri("/groups")).params(o.toQueryParams()).execute().getList(); }
    @Override public List<? extends VolumeGroup> listDetail(GroupListOptions o) { requireMicroVersion("Groups", V(13)); return get(Groups.class, uri("/groups/detail")).params(o.toQueryParams()).execute().getList(); }
    @Override public VolumeGroup get(String id) { requireMicroVersion("Groups", V(13)); return get(CinderGroup.class, uri("/groups/%s", Objects.requireNonNull(id))).execute(); }

    @Override
    public VolumeGroup create(GroupCreate request) {
        Objects.requireNonNull(request);
        requireMicroVersion("Groups", V(13));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", request.getName());
        body.put("group_type", request.getGroupType());
        body.put("volume_types", request.getVolumeTypes());
        if (request.getDescription() != null) body.put("description", request.getDescription());
        if (request.getAvailabilityZone() != null) body.put("availability_zone", request.getAvailabilityZone());
        return post(CinderGroup.class, uri("/groups")).entity(JsonBody.of("group", body)).execute();
    }

    @Override
    public VolumeGroup createFromSource(String name, String description, String groupSnapshotId, String sourceGroupId) {
        requireMicroVersion("Group from source", V(14));
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (groupSnapshotId != null) body.put("group_snapshot_id", groupSnapshotId);
        if (sourceGroupId != null) body.put("source_group_id", sourceGroupId);
        return post(CinderGroup.class, uri("/groups/action")).entity(JsonBody.of("create-from-src", body)).execute();
    }

    @Override
    public VolumeGroup update(String id, String name, String description, List<String> addVolumes, List<String> removeVolumes) {
        Objects.requireNonNull(id);
        requireMicroVersion("Groups", V(13));
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (addVolumes != null) body.put("add_volumes", String.join(",", addVolumes));
        if (removeVolumes != null) body.put("remove_volumes", String.join(",", removeVolumes));
        return put(CinderGroup.class, uri("/groups/%s", id)).entity(JsonBody.of("group", body)).execute();
    }

    @Override public ActionResponse delete(String id, boolean deleteVolumes) { requireMicroVersion("Groups", V(13)); return action(id, "delete", Collections.singletonMap("delete-volumes", deleteVolumes)); }
    @Override public ActionResponse resetStatus(String id, String status) { requireMicroVersion("Group reset status", V(20)); return action(id, "reset_status", Collections.singletonMap("status", Objects.requireNonNull(status))); }
    @Override public ActionResponse enableReplication(String id) { requireMicroVersion("Group replication", V(38)); return action(id, "enable_replication", Collections.emptyMap()); }
    @Override public ActionResponse disableReplication(String id) { requireMicroVersion("Group replication", V(38)); return action(id, "disable_replication", Collections.emptyMap()); }

    @Override
    public ActionResponse failoverReplication(String id, boolean allowAttachedVolume, String secondaryBackendId) {
        requireMicroVersion("Group replication", V(38));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("allow_attached_volume", allowAttachedVolume);
        if (secondaryBackendId != null) body.put("secondary_backend_id", secondaryBackendId);
        return action(id, "failover_replication", body);
    }

    @Override
    public List<? extends ReplicationTarget> listReplicationTargets(String id) {
        requireMicroVersion("Group replication", V(38));
        CinderReplicationTargets targets = post(CinderReplicationTargets.class, uri("/groups/%s/action", Objects.requireNonNull(id)))
                .entity(JsonBody.of("list_replication_targets", Collections.emptyMap())).execute();
        return targets == null ? Collections.emptyList() : targets.getTargets();
    }

    private ActionResponse action(String id, String action, Map<String, ?> body) {
        return post(ActionResponse.class, uri("/groups/%s/action", Objects.requireNonNull(id))).entity(JsonBody.of(action, body)).execute();
    }
}
