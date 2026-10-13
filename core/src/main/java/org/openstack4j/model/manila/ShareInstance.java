package org.openstack4j.model.manila;

import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openstack4j.model.ModelEntity;

/**
 * Representation of a share instance.
 *
 * @author Daniel Gonzalez Nothnagel
 */
public interface ShareInstance extends ModelEntity {
    /**
     * @return the share instance status
     */
    Status getStatus();

    /**
     * @return the UUID of the share from which the share instance was created
     */
    String getShareId();

    /**
     * @return the availability zone
     */
    String getAvailabilityZone();

    /**
     * @return the date and time stamp when the share instance was created
     */
    String getCreatedAt();

    /**
     * @return the share instance export location
     */
    String getExportLocation();

    /**
     * @return a list of share instance export locations
     */
    List<String> getExportLocations();

    /**
     * @return the share network ID
     */
    String getShareNetworkId();

    /**
     * @return the share server ID
     */
    String getShareServerId();

    /**
     * @return the share instance host name
     */
    String getHost();

    /**
     * @return the share instance ID
     */
    String getId();

    enum Status {
        AVAILABLE,
        ERROR,
        CREATING,
        DELETING,
        ERROR_DELETING,
        MANAGE_STARTING,
        UNMANAGE_STARTING,
        UNMANAGE_ERROR,
        UNMANAGED,
        EXTENDING,
        EXTENDING_ERROR,
        SHRINKING,
        SHRINKING_ERROR,
        SHRINKING_POSSIBLE_DATA_LOSS_ERROR,
        CREATING_FROM_SNAPSHOT,
        DELETED,
        /** (manila server constants, not in api-ref table) */
        DEFERRED_DELETING,
        /** (manila server constants, not in api-ref table) */
        ERROR_DEFERRED_DELETING,
        INACTIVE,
        MANAGE_ERROR,
        /** (manila server constants, not in api-ref table) */
        MANAGE_ERROR_UNMANAGE_STARTING,
        MIGRATING,
        MIGRATING_TO,
        /** (manila server constants, not in api-ref table) */
        SERVER_MIGRATING,
        /** (manila server constants, not in api-ref table) */
        SERVER_MIGRATING_TO,
        REPLICATION_CHANGE,
        /** (manila server constants, not in api-ref table) */
        RESTORING,
        REVERTING,
        REVERTING_ERROR,
        /** (manila server constants, not in api-ref table) */
        REVERTING_TO_SNAPSHOT,
        AWAITING_TRANSFER,
        BACKUP_CREATING,
        BACKUP_RESTORING,
        BACKUP_RESTORING_ERROR,
        /** (manila server constants, not in api-ref table) */
        ENSURING,
        /** Value not known to this client; check the raw API */
        UNRECOGNIZED;

        @JsonCreator
        public static Status value(String v) {
            if (v == null || v.isEmpty()) return UNRECOGNIZED;
            try {
                return valueOf(v.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return UNRECOGNIZED;
            }
        }

        @JsonValue
        public String value() {
            return name().toLowerCase();
        }
    }
}
