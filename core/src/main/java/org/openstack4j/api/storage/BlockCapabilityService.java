package org.openstack4j.api.storage;

import org.openstack4j.model.storage.block.BackendCapabilities;

import org.openstack4j.common.RestService;

/** Backend capabilities ({@code GET /capabilities/{host}}; admin). */
public interface BlockCapabilityService extends RestService {

    BackendCapabilities get(String hostName);
}
