package org.openstack4j.common;

import org.openstack4j.model.common.MicroVersionInfo;

/**
 * Opt-in microversions of one OpenStack service. Off by default: requests carry no microversion header. Once turned
 * on, methods whose API the service removed in later microversions keep working because they are sent at the highest
 * microversion they support, and new features check their minimum microversion before sending.
 *
 * @param <V> the service's version information type
 */
public interface MicroVersionService<V extends MicroVersionInfo> extends RestService {

    /** Turns microversions on at min(library latest, server max). */
    V negotiate();

    /** Turns microversions on at a fixed version within the library and server range. */
    V use(String version);

    /** Turns microversions off again (no header). */
    void clear();

    V get();
}
