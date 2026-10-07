package org.openstack4j.api.manila;

import org.openstack4j.common.MicroVersionService;
import org.openstack4j.model.manila.ShareApiVersion;

/**
 * Opt-in Manila microversions for this session. They apply to the methods added in 4.6 (the accessors in
 * {@code org.openstack4j.api.manila.ext}): {@code negotiate()} sends min(library latest, server max), {@code use("2.x")}
 * pins a version and {@code clear()} goes back to each method's own minimum. The older methods always send 2.6.
 */
public interface ShareMicroVersionService extends MicroVersionService<ShareApiVersion> {
}
