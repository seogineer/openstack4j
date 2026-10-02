package org.openstack4j.model.compute;

import java.util.Date;
import java.util.List;

/**
 * An action executed on an instance
 *
 * @author Christian Banse
 */
public interface InstanceAction {

    /**
     * @return the action
     */
    String getAction();

    /**
     * @return the instance uuid
     */
    String getInstanceUuid();

    /**
     * @return the message
     */
    String getMessage();

    /**
     * @return the project id
     */
    String getProjectId();

    /**
     * @return the request id
     */
    String getRequestId();

    /**
     * @return the start time
     */
    Date getStartTime();

    /**
     * @return the user id
     */
    String getUserId();

    /** @return updated_at (2.58+) */
    default Date getUpdatedAt() { return null; }

    /** @return events, present when the action is shown by request id */
    default List<? extends ServerActionEvent> getEvents() { return null; }
}
