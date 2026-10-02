package org.openstack4j.model.storage.block;

import java.util.Date;
import java.util.List;

import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.common.Link;

/**
 * Represents a Volume Transfer Entity which is used for creating a volume transfer
 *
 * @author Jeremy Unruh
 */
public interface VolumeTransfer extends ModelEntity {

    /**
     * @return the identifier assigned to the transfer
     */
    String getId();

    /**
     * @return indicates the volume identifier of the volume being transfer
     */
    String getVolumeId();

    /**
     * @return the name of the transfer
     */
    String getName();

    /**
     * @return the authorization key of the transfer
     */
    String getAuthKey();

    /**
     * @return the date/time the transfer was created
     */
    Date getCreatedAt();

    /**
     * @return List of External Links
     **/
    List<? extends Link> getLinks();

    /** @return source_project_id (3.57+) */
    default String getSourceProjectId() { return null; }
    /** @return destination_project_id (3.57+) */
    default String getDestinationProjectId() { return null; }
    /** @return accepted (3.57+) */
    default Boolean getAccepted() { return null; }
    /** @return no_snapshots (3.55+) */
    default Boolean getNoSnapshots() { return null; }
}
