package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.*;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;
import org.openstack4j.api.storage.BlockWorkerService;
import org.openstack4j.model.storage.block.WorkerCleanup;
import org.openstack4j.model.storage.block.options.WorkerCleanupRequest;

public class BlockWorkerServiceImpl extends BaseBlockStorageServices implements BlockWorkerService {

    @Override
    public WorkerCleanup cleanup(WorkerCleanupRequest request) {
        Objects.requireNonNull(request);
        requireMicroVersion("Worker cleanup", V(24));
        return post(CinderWorkerCleanup.class, uri("/workers/cleanup")).entity(JsonBody.of(request.toMap())).execute();
    }
}
