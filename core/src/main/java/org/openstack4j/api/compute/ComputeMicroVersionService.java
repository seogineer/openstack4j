package org.openstack4j.api.compute;

import org.openstack4j.common.RestService;
import org.openstack4j.model.compute.ComputeVersion;

/**
 * Opt-in compute microversions. Off by default: requests carry no microversion header (Nova treats them as 2.1).
 * Once turned on, methods whose API Nova removed in later microversions keep working because they are sent at the
 * highest microversion they support.
 */
public interface ComputeMicroVersionService extends RestService {

    /** Turns microversions on at min(library latest, server max). */
    ComputeVersion negotiate();

    /** Turns microversions on at a fixed version within the library and server range. */
    ComputeVersion use(String version);

    /** Turns microversions off again (no header). */
    void clear();

    ComputeVersion get();
}
