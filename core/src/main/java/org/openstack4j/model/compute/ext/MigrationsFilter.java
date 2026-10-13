package org.openstack4j.model.compute.ext;

import org.openstack4j.model.common.BaseFilter;
import org.openstack4j.model.compute.ext.Migration.Status;

/**
 * Filter options used for Migration results
 *
 * @author Jeremy Unruh
 */
public class MigrationsFilter extends BaseFilter {

    private MigrationsFilter() {
    }

    public static MigrationsFilter create() {
        return new MigrationsFilter();
    }

    /**
     * Filters the response by host name
     *
     * @param host the host name
     * @return MigrationsFilter
     */
    public MigrationsFilter host(String host) {
        filter("host", host);
        return this;
    }

    /**
     * Filters the response by status.
     *
     * @param status the status to filter by; note the constant name is sent as is, so the hyphenated nova values
     *               ({@code PRE_MIGRATING}, {@code POST_MIGRATING}) cannot be filtered with this method
     * @return MigrationsFilter
     */
    public MigrationsFilter status(Status status) {
        filter("status", status.name());
        return this;
    }

    /**
     * Filters the response by cell name
     *
     * @param cellName the cell name
     * @return MigrationsFilter
     */
    public MigrationsFilter cellName(String cellName) {
        filter("cell_name", cellName);
        return this;
    }
    
    /**
     * Filters the instance UUID.
     * 
     * @param instanceUuid
     *            Instance UUID
     * @return MigrationsFilter
     */
    public MigrationsFilter instanceUuid(String instanceUuid) {
        filter("instance_uuid", instanceUuid);
        return this;
    }
}
