package org.openstack4j.api.storage;

import org.openstack4j.common.MicroVersionService;
import org.openstack4j.model.storage.block.BlockStorageVersion;

/**
 * Opt-in block storage microversions (3.0 - 3.71). Off by default: requests carry no microversion header and Cinder
 * answers as 3.0. Turn them on with {@link #negotiate()} to read fields and use APIs added after 3.0.
 */
public interface BlockStorageMicroVersionService extends MicroVersionService<BlockStorageVersion> {
}
