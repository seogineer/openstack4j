package org.openstack4j.model.octavia;

import java.util.List;

import org.openstack4j.common.Buildable;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.octavia.builder.LbPoolV2UpdateBuilder;

/**
 * An entity for updating a lbaas v2 lb pool
 *
 * @author wei
 */
public interface LbPoolV2Update extends ModelEntity, Buildable<LbPoolV2UpdateBuilder> {

    /**
     * @return Pool name. Does not have to be unique.
     */
    public String getName();

    /**
     * @return Description for the pool.
     */
    public String getDescription();

    /**
     * @return The administrative state of the lb pool, which is up (true) or
     * down (false).
     */
    public boolean isAdminStateUp();

    /**
     * @return The load-balancer algorithm, which is round-robin, least-connections, and so on. This value, which must be supported, is dependent on the load-balancer provider. Round-robin must be supported.
     */
    public LbMethod getLbMethod();

    /**
     * Optional
     *
     * @see Vip#getSessionPersistence()
     */
    public SessionPersistence getSessionPersistence();

    /**
     * @return the OpenSSL cipher string set through the builder (Octavia API 2.15+), or null
     */
    default String getTlsCiphers() {
        return null;
    }

    /**
     * @return the TLS versions set through the builder (Octavia API 2.17+), or null
     */
    default List<String> getTlsVersions() {
        return null;
    }

    /**
     * @return the CA certificate container ref set through the builder (Octavia API 2.8+), or null
     */
    default String getCaTlsContainerRef() {
        return null;
    }

}
