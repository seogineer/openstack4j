package org.openstack4j.model.compute;

import org.openstack4j.common.Buildable;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.compute.builder.ServerActionEventBuilder;

/**
 * Model for the generic events
 *
 * @author sujit sah
 */
public interface ServerActionEvent extends ModelEntity, Buildable<ServerActionEventBuilder> {
    /**
     * @return event
     */
    public String getEvent();

    /**
     * @return finish_time or null
     */
    public String getFinishTime();

    /**
     * @return result
     */
    public String getResult();

    /**
     * @return start_time or null
     */
    public String getStartTime();

    /**
     * @return traceback or null
     */
    public String getTraceback();

    /** @return host name, if policy allows (2.62+) */
    default String getHost() { return null; }

    /** @return obfuscated host id (2.62+) */
    default String getHostId() { return null; }

    /** @return failure details (2.84+) */
    default String getDetails() { return null; }
}
