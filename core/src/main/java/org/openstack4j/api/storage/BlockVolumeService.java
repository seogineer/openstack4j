package org.openstack4j.api.storage;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.VolumeTypeEncryption;
import org.openstack4j.model.storage.block.VolumeUploadImage;
import org.openstack4j.model.storage.block.options.UploadImageData;
import org.openstack4j.model.storage.block.VolumeSummary;
import org.openstack4j.model.storage.block.options.VolumeListOptions;
import org.openstack4j.model.storage.block.options.VolumeUpdateOptions;
import org.openstack4j.model.storage.block.options.VolumeMigrateRequest;

/**
 * Manages Volumes and Volume Type based operations against Block Storage (Cinder)
 *
 * @author Jeremy Unruh
 */
public interface BlockVolumeService extends RestService {

    /**
     * The volume type defines the characteristics of a volume
     *
     * @return List of VolumeType entities
     */
    List<? extends VolumeType> listVolumeTypes();

    /**
     * Deletes the specified VolumeType
     *
     * @param volumeTypeId the volume type identifier
     */
    void deleteVolumeType(String volumeTypeId);

    /**
     * Creates a new volume type with the specified name
     *
     * @param volumeType the volumeType for create
     * @return the created volume type
     */
    VolumeType createVolumeType(VolumeType volumeType);

    /**
     * Creates a new encryption with the specified instance for the specified volume type
     *
     * @param volumeTypeId the volume type identifier
     * @param volumeTypeEncryption the encryption to create
     * @return the created volume type encryption
     */
    VolumeTypeEncryption createVolumeTypeEncryption(String volumeTypeId, VolumeTypeEncryption volumeTypeEncryption);

    /**
     * Retrieves the encryption for the specified volume type
     *
     * @param volumeTypeId the volume type identifier
     * @return the encryption
     */
    VolumeTypeEncryption getVolumeTypeEncryption(String volumeTypeId);

    /**
     * Deletes the specified volume type encryption for the specified volume type
     *
     * @param volumeTypeId the volume type identifier
     * @param encryptionId the encryption identifier
     */
    void deleteVolumeTypeEncryption(String volumeTypeId, String encryptionId);

    /**
     * Lists summary information for all Block Storage volumes that the tenant who submits the request can access.
     *
     * @return List of Volumes
     */
    List<? extends Volume> list();

    /**
     * Returns list of Block Storage volumes filtered by parameters.
     *
     * @param filteringParams map (name, value) of filtering parameters
     */
    List<? extends Volume> list(Map<String, String> filteringParams);

    /**
     * Lists all Block Storage volumes for all tenants.
     *
     * @return List of all Volumes
     */
    List<? extends Volume> listAll();

    /**
     * Gets a Block Storage volume by ID
     *
     * @param volumeId the volume identifier
     * @return the volume or null if not found
     */
    Volume get(String volumeId);

    /**
     * Deletes the specified volume
     *
     * @param volumeId the volume identifier
     * @return the action response
     */
    ActionResponse delete(String volumeId);

    /**
     * Attempt forced removal of volume, regardless of the state.
     * It's dangerous but useful. It's not 100% success.
     *
     * @param volumeId the volume id
     * @return the action response
     */
    ActionResponse forceDelete(String volumeId);

    /**
     * Resets the specified volume status.
     *
     * @param volumeId the volume id
     * @param status new volume status
     * @return the action response
     */
    ActionResponse resetState(String volumeId, Volume.Status status);

    /**
     * Extends the specified volume size.
     *
     * @param volumeId the volume id
     * @param newSize new volume size
     * @return the action response
     */
    ActionResponse extend(String volumeId, Integer newSize);

    /**
     * Creates a new Block Storage Volume
     *
     * @param volume the volume for create
     * @return the created volume
     */
    Volume create(Volume volume);

    /**
     * Uploads a volume to the image service
     *
     * @param volumeId the volume identifier to upload
     * @param data the data about the volume being uploaded (required)
     * @return the volume upload image containing the current status
     */
    VolumeUploadImage uploadToImage(String volumeId, UploadImageData data);

    /**
     * OpenStack only allows name or description to be updated. This call enforces that based on the API docs.
     *
     * @param volumeId the volume id
     * @param name the name to update (null indicates no name update)
     * @param description the description to update (null indicates no description update)
     * @return the action response
     */
    ActionResponse update(String volumeId, String name, String description);

    /**
     * migrate a volume to another host and service
     *
     * @param volumeId the volume id
     * @param hostService the destination host and service ,like kvmnode002021.cnsuning.com@lvmdriver
     * @return the action response
     */
    ActionResponse migrate(String volumeId, String hostService, boolean forceHostCopy);

    /**
     * Returns the API used to transfer a Volume from one tenant/project to another
     *
     * @return the volume transfer service
     */
    BlockVolumeTransferService transfer();

    /**
     * Updates volume read-only access-mode flag
     *
     * @param volumeId ID of volume to update
     * @param readonly enables or disables update of volume to read-only access mode
     * @return the action response
     */
    ActionResponse readOnlyModeUpdate(String volumeId, boolean readonly);

    /**
     * <br/>Description:Attaches a volume to a server.
     * You should set instance_uuid or host_name.
     * Volume status must be available.
     * <p>Author:Wang Ting/王婷</p>
     */
    ActionResponse attach(String volumeId, String instanceId, String mountpoint, String hostName);

    /**
     * <br/>Description:Forces a volume to detach.
     * <p>Author:Wang Ting/王婷</p>
     */
    ActionResponse forceDetach(String volumeId, String initiator, String attachmentId);

    /**
     * Detach volume from server
     *
     * @author capitek-xuning（首信科技-徐宁）
     */
    ActionResponse detach(String volumeId, String attachmentId);

    /**
     * Update volume bootable status.
     *
     * @param volumeId the volume id
     * @param bootable Enables or disables the bootable attribute
     * @return the action response
     */
    ActionResponse bootable(String volumeId, Boolean bootable);

    /**
     * Lists volumes with details using typed filters. Filters that need a newer block storage microversion fail before
     * the request when the session does not send it.
     */
    List<? extends Volume> list(VolumeListOptions options);

    /**
     * Volume summary of the project ({@code GET /volumes/summary}, 3.12+; metadata 3.36+).
     */
    VolumeSummary summary();

    /**
     * Volume summary with filters (3.12+).
     */
    VolumeSummary summary(VolumeListOptions options);

    /**
     * Updates name, description and/or metadata ({@code PUT /volumes/{id}}) and returns the updated volume.
     */
    Volume update(String volumeId, VolumeUpdateOptions options);

    // --- metadata -------------------------------------------------------------------------------------------------

    /** All metadata of a volume ({@code GET /volumes/{id}/metadata}). */
    Map<String, String> metadata(String volumeId);

    /** Adds or updates metadata keys ({@code POST /volumes/{id}/metadata}); other keys are kept. */
    Map<String, String> setMetadata(String volumeId, Map<String, String> metadata);

    /** Replaces all metadata ({@code PUT /volumes/{id}/metadata}). */
    Map<String, String> replaceMetadata(String volumeId, Map<String, String> metadata);

    /** One metadata value ({@code GET /volumes/{id}/metadata/{key}}), or {@code null}. */
    String metadataItem(String volumeId, String key);

    /** Sets one metadata value ({@code PUT /volumes/{id}/metadata/{key}}). */
    String updateMetadataItem(String volumeId, String key, String value);

    /** Deletes one metadata key ({@code DELETE /volumes/{id}/metadata/{key}}). */
    ActionResponse deleteMetadataItem(String volumeId, String key);

    /** Image metadata of a volume ({@code os-show_image_metadata}). */
    Map<String, String> imageMetadata(String volumeId);

    /** Sets image metadata ({@code os-set_image_metadata}) and returns the resulting metadata. */
    Map<String, String> setImageMetadata(String volumeId, Map<String, String> metadata);

    /** Removes one image metadata key ({@code os-unset_image_metadata}). */
    ActionResponse unsetImageMetadata(String volumeId, String key);

    // --- actions (3.x) ------------------------------------------------------------------------------------------------

    /** Reverts the volume to its latest snapshot ({@code revert}, 3.40+). */
    ActionResponse revertToSnapshot(String volumeId, String snapshotId);

    /** Re-images the volume ({@code os-reimage}, 3.68+). */
    ActionResponse reimage(String volumeId, String imageId, boolean reimageReserved);

    /** Tells Cinder that Nova finished (or failed) handling a volume extension ({@code os-extend_volume_completion}, 3.71+). */
    ActionResponse completeExtend(String volumeId, boolean error);

    /**
     * Changes the volume type ({@code os-retype}).
     *
     * @param migrationPolicy {@code never} or {@code on-demand}, or {@code null} for the default
     */
    ActionResponse retype(String volumeId, String newType, String migrationPolicy);

    /** Migrates the volume to a host or, from 3.16, a cluster ({@code os-migrate_volume}; admin). */
    ActionResponse migrate(String volumeId, VolumeMigrateRequest request);

    /** Completes a migration ({@code os-migrate_volume_completion}; admin). */
    ActionResponse completeMigration(String volumeId, String newVolumeId, boolean error);

    /** Removes the volume from Cinder without deleting it on the backend ({@code os-unmanage}; admin). */
    ActionResponse unmanage(String volumeId);

    /** {@code os-reserve} */
    ActionResponse reserve(String volumeId);

    /** {@code os-unreserve} */
    ActionResponse unreserve(String volumeId);

    /** {@code os-begin_detaching} */
    ActionResponse beginDetaching(String volumeId);

    /** {@code os-roll_detaching} */
    ActionResponse rollDetaching(String volumeId);

    /** Initialises a connection for the given connector ({@code os-initialize_connection}) and returns the connection info. */
    Map<String, Object> initializeConnection(String volumeId, Map<String, Object> connector);

    /** {@code os-terminate_connection} */
    ActionResponse terminateConnection(String volumeId, Map<String, Object> connector);

    /**
     * Resets status fields ({@code os-reset_status}; admin). Each argument may be {@code null} to leave it unchanged.
     */
    ActionResponse setStatus(String volumeId, String status, String attachStatus, String migrationStatus);
}
