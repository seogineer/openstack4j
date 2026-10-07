package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareGroupType;
import org.openstack4j.model.manila.ext.options.ShareGroupTypeCreate;

/** Share group types ({@code /v2/share-group-types}, microversion 2.55). */
public interface ShareGroupTypeService extends RestService {

    /** @return the share group types */
    List<? extends ShareGroupType> list();

    /** @param filters query parameters such as {@code is_public} ({@code true}, {@code false}, {@code all}), {@code group_specs} */
    List<? extends ShareGroupType> list(Map<String, String> filters);

    /** @return the share group type, or {@code null} when it does not exist */
    ShareGroupType get(String id);

    ShareGroupType create(ShareGroupTypeCreate create);

    /** @return the default share group type, or {@code null} when none is set */
    ShareGroupType getDefault();

    /** @return the group specs; a missing type raises */
    Map<String, String> getGroupSpecs(String id);

    /** Adds or changes group specs and returns them. */
    Map<String, String> setGroupSpecs(String id, Map<String, String> groupSpecs);

    ActionResponse unsetGroupSpec(String id, String key);

    /** @return the projects that can use a private group type ({@code share_group_type_id}, {@code project_id}); a missing type raises */
    List<Map<String, Object>> listAccess(String id);

    ActionResponse addAccess(String id, String projectId);

    ActionResponse removeAccess(String id, String projectId);

    ActionResponse delete(String id);
}
