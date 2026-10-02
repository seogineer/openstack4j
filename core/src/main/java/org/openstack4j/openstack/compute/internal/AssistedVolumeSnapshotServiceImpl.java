package org.openstack4j.openstack.compute.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.api.compute.AssistedVolumeSnapshotService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.AssistedVolumeSnapshot;
import org.openstack4j.openstack.compute.domain.JsonBody;
import org.openstack4j.openstack.compute.domain.NovaAssistedVolumeSnapshot;
import org.openstack4j.openstack.compute.functions.ToActionResponseFunction;

/** {@code /os-assisted-volume-snapshots} */
public class AssistedVolumeSnapshotServiceImpl extends BaseComputeServices implements AssistedVolumeSnapshotService {

    private static final ObjectMapper PLAIN = new ObjectMapper();

    @Override
    public AssistedVolumeSnapshot create(String volumeId, Map<String, Object> createInfo) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(createInfo);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("volume_id", volumeId);
        snapshot.put("create_info", createInfo);
        return post(NovaAssistedVolumeSnapshot.class, uri("/os-assisted-volume-snapshots"))
                .entity(JsonBody.of("snapshot", snapshot))
                .execute();
    }

    @Override
    public ActionResponse delete(String snapshotId, Map<String, Object> deleteInfo) {
        Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(deleteInfo);
        String json;
        try {
            json = PLAIN.writeValueAsString(deleteInfo);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("delete_info is not serializable", e);
        }
        return ToActionResponseFunction.INSTANCE.apply(
                delete(Void.class, uri("/os-assisted-volume-snapshots/%s", snapshotId)).param("delete_info", json).executeWithResponse());
    }
}
