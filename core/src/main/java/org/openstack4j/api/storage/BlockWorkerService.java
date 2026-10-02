package org.openstack4j.api.storage;

import org.openstack4j.model.storage.block.WorkerCleanup;
import org.openstack4j.model.storage.block.options.WorkerCleanupRequest;

import org.openstack4j.common.RestService;

/** Worker cleanup ({@code POST /workers/cleanup}, block storage microversion 3.24+; admin). */
public interface BlockWorkerService extends RestService {

    WorkerCleanup cleanup(WorkerCleanupRequest request);
}
