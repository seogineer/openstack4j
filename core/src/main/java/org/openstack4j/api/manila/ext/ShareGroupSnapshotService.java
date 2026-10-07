package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareGroupSnapshot;
import org.openstack4j.model.manila.ext.options.ShareGroupSnapshotCreate;
import org.openstack4j.model.manila.ext.options.ShareGroupSnapshotUpdate;

/** Share group snapshots ({@code /v2/share-group-snapshots}, microversion 2.55). */
public interface ShareGroupSnapshotService extends RestService {

    /** @return the share group snapshots */
    List<? extends ShareGroupSnapshot> list();

    /** @param filters query parameters such as {@code name}, {@code status}, {@code share_group_id}, {@code limit}, {@code offset} */
    List<? extends ShareGroupSnapshot> list(Map<String, String> filters);

    /** @return the share group snapshot, or {@code null} when it does not exist */
    ShareGroupSnapshot get(String id);

    ShareGroupSnapshot create(ShareGroupSnapshotCreate create);

    ShareGroupSnapshot update(String id, ShareGroupSnapshotUpdate update);

    /** @return the share snapshots of a group snapshot ({@code share_id}, {@code size}, {@code status} ...); a missing one raises */
    List<Map<String, Object>> listMembers(String id);

    /** @param status e.g. {@code available}, {@code error} (admin) */
    ActionResponse resetStatus(String id, String status);

    ActionResponse forceDelete(String id);

    ActionResponse delete(String id);
}
