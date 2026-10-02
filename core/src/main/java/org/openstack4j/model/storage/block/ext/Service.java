package org.openstack4j.model.storage.block.ext;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openstack4j.model.ModelEntity;

/**
 * A Service represents a Block storage service
 *
 * @author Stephan Latour
 */
public interface Service extends ModelEntity {
    /**
     * @return the binary for this service
     */
    String getBinary();

    /**
     * @return the reason for disabled status of this service
     */
    String getDisabledReason();

    /**
     * @return the host for this service
     */
    String getHost();

    /**
     * @return the id for this service
     */
    String getId();

    /**
     * @return the status of the service
     */
    State getState();

    /**
     * @return the status of the service
     */
    Status getStatus();

    /**
     * @return last updated time
     */
    Date getUpdatedAt();

    /**
     * @return the zone for this service
     */
    String getZone();

    /**
     * The status of a Block storage service entity
     */
    public enum Status {
        DISABLED, ENABLED, UNRECOGNIZED;

        @JsonCreator
        public static Status forValue(String value) {
            if (value != null) {
                for (Status s : Status.values()) {
                    if (s.name().equalsIgnoreCase(value)) {
                        return s;
                    }
                }
            }
            return Status.UNRECOGNIZED;
        }
    }

    /**
     * The state of a Block storage service entity
     */
    public enum State {
        DOWN, UNRECOGNIZED, UP;

        @JsonCreator
        public static State forValue(String value) {
            if (value != null) {
                for (State s : State.values()) {
                    if (s.name().equalsIgnoreCase(value)) {
                        return s;
                    }
                }
            }
            return State.UNRECOGNIZED;
        }
    }

    /** @return cluster (3.7+) */
    default String getCluster() { return null; }
    /** @return replication_status */
    default String getReplicationStatus() { return null; }
    /** @return active_backend_id */
    default String getActiveBackendId() { return null; }
    /** @return frozen (3.26+) */
    default Boolean getFrozen() { return null; }
    /** @return backend_state (3.49+) */
    default String getBackendState() { return null; }
}
