package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareGroup;
import org.openstack4j.model.manila.ext.options.ShareGroupCreate;
import org.openstack4j.model.manila.ext.options.ShareGroupUpdate;

/** Share groups ({@code /v2/share-groups}, microversion 2.55). */
public interface ShareGroupService extends RestService {

    /** @return the share groups */
    List<? extends ShareGroup> list();

    /** @param filters query parameters such as {@code name}, {@code status}, {@code share_group_type_id}, {@code share_network_id}, {@code host}, {@code limit}, {@code offset} */
    List<? extends ShareGroup> list(Map<String, String> filters);

    /** @return the share group, or {@code null} when it does not exist */
    ShareGroup get(String id);

    ShareGroup create(ShareGroupCreate create);

    ShareGroup update(String id, ShareGroupUpdate update);

    /** @param status e.g. {@code available}, {@code error} (admin) */
    ActionResponse resetStatus(String id, String status);

    ActionResponse forceDelete(String id);

    ActionResponse delete(String id);
}
