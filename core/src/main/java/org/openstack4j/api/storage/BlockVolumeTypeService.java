package org.openstack4j.api.storage;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.VolumeTypeAccess;
import org.openstack4j.model.storage.block.VolumeTypeEncryption;
import org.openstack4j.model.storage.block.options.VolumeTypeListOptions;

/** Volume types ({@code /types}): CRUD, extra specs, project access and encryption. */
public interface BlockVolumeTypeService extends RestService {

    List<? extends VolumeType> list();

    /** Lists with filters; {@code extra_specs} needs 3.52. */
    List<? extends VolumeType> list(VolumeTypeListOptions options);

    VolumeType get(String volumeTypeId);

    /** The default type ({@code GET /types/default}; the project's own default from 3.62). */
    VolumeType getDefault();

    VolumeType create(VolumeType volumeType);

    /** Updates name, description and/or is_public; {@code null} leaves a field unchanged. */
    VolumeType update(String volumeTypeId, String name, String description, Boolean isPublic);

    ActionResponse delete(String volumeTypeId);

    Map<String, String> extraSpecs(String volumeTypeId);

    /** Adds or updates extra specs ({@code POST /types/{id}/extra_specs}). */
    Map<String, String> setExtraSpecs(String volumeTypeId, Map<String, String> extraSpecs);

    String extraSpec(String volumeTypeId, String key);

    String updateExtraSpec(String volumeTypeId, String key, String value);

    ActionResponse deleteExtraSpec(String volumeTypeId, String key);

    /** Allows a project to use a private type ({@code addProjectAccess}). */
    ActionResponse addProjectAccess(String volumeTypeId, String projectId);

    ActionResponse removeProjectAccess(String volumeTypeId, String projectId);

    List<? extends VolumeTypeAccess> listProjectAccess(String volumeTypeId);

    /** Encryption of the type ({@code GET /types/{id}/encryption}). */
    VolumeTypeEncryption encryption(String volumeTypeId);

    /** One encryption field ({@code GET /types/{id}/encryption/{key}}), such as {@code cipher}. */
    String encryptionSpec(String volumeTypeId, String key);

    VolumeTypeEncryption createEncryption(String volumeTypeId, VolumeTypeEncryption encryption);

    VolumeTypeEncryption updateEncryption(String volumeTypeId, String encryptionId, VolumeTypeEncryption encryption);

    ActionResponse deleteEncryption(String volumeTypeId, String encryptionId);
}
