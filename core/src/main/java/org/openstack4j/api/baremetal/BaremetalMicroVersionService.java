package org.openstack4j.api.baremetal;

import org.openstack4j.common.MicroVersionService;
import org.openstack4j.model.baremetal.BaremetalVersion;

/**
 * Opt-in Ironic microversions for this session: {@code negotiate()} sends min(library latest, server max),
 * {@code use("1.x")} pins a version, {@code clear()} goes back to no header (the server then answers as 1.1).
 */
public interface BaremetalMicroVersionService extends MicroVersionService<BaremetalVersion> {
}
