package org.openstack4j.api.storage.ext;

import java.util.List;

import org.openstack4j.model.storage.block.ext.Service;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.ServiceLogLevel;

/**
 * API which supports the "os-services" extension.
 *
 * @author Taemin
 */
public interface BlockStorageServiceService {

    /**
     * List services info
     * <p>
     * NOTE: This is an extension and not all deployments support os-services
     *
     * @return a list of block storage services
     */
    List<? extends Service> list();

    /** Enables a service ({@code PUT /os-services/enable}). */
    ActionResponse enable(String host, String binary);

    /** Disables a service ({@code PUT /os-services/disable}). */
    ActionResponse disable(String host, String binary);

    /** Disables a service with a reason ({@code PUT /os-services/disable-log-reason}). */
    ActionResponse disableWithReason(String host, String binary, String reason);

    /** Freezes a backend host ({@code PUT /os-services/freeze}). */
    ActionResponse freeze(String host);

    /** Thaws a backend host ({@code PUT /os-services/thaw}). */
    ActionResponse thaw(String host);

    /** Fails a replicating backend over ({@code PUT /os-services/failover_host}); {@code backendId} may be {@code null}. */
    ActionResponse failoverHost(String host, String backendId);

    /** Fails a host or cluster over ({@code PUT /os-services/failover}, 3.26+); give {@code host} or {@code cluster}. */
    ActionResponse failover(String host, String cluster, String backendId);

    /** Reads log levels ({@code PUT /os-services/get-log}, 3.32+); every argument may be {@code null}. */
    List<? extends ServiceLogLevel> getLog(String binary, String server, String prefix);

    /** Sets a log level ({@code PUT /os-services/set-log}, 3.32+); binary, server and prefix may be {@code null}. */
    ActionResponse setLog(String level, String binary, String server, String prefix);
}
