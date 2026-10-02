package org.openstack4j.api.storage;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeGroupType;

/** Group types and their specs ({@code /group_types}, block storage microversion 3.11+). */
public interface BlockGroupTypeService extends RestService {

    List<? extends VolumeGroupType> list();

    VolumeGroupType get(String groupTypeId);

    VolumeGroupType getDefault();

    /** @param isPublic {@code null} for the default (public); @param groupSpecs optional specs */
    VolumeGroupType create(String name, String description, Boolean isPublic, Map<String, String> groupSpecs);

    /** Updates name, description and/or is_public; {@code null} leaves a field unchanged. */
    VolumeGroupType update(String groupTypeId, String name, String description, Boolean isPublic);

    ActionResponse delete(String groupTypeId);

    Map<String, String> groupSpecs(String groupTypeId);

    /** Adds or updates group specs. */
    Map<String, String> setGroupSpecs(String groupTypeId, Map<String, String> groupSpecs);

    String groupSpec(String groupTypeId, String key);

    String updateGroupSpec(String groupTypeId, String key, String value);

    ActionResponse deleteGroupSpec(String groupTypeId, String key);
}
