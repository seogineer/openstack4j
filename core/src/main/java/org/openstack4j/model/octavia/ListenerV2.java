package org.openstack4j.model.octavia;

import java.util.Map;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.openstack4j.common.Buildable;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.octavia.builder.ListenerV2Builder;
import org.openstack4j.openstack.octavia.domain.ListItem;
import org.openstack4j.openstack.octavia.domain.OctaviaListenerV2;

/**
 * A listener for v2 loadbalancer
 *
 * @author wei
 */
@JsonDeserialize(as = OctaviaListenerV2.class)
public interface ListenerV2 extends ModelEntity, Buildable<ListenerV2Builder> {

    /**
     * @return id. The unique ID for the listener.
     */
    String getId();

    /**
     * @return the ID of the project/tenant that owns the resource
     */
    String getProjectId();

    /**
     * @return listener name. Does not have to be unique.
     */
    String getName();

    /**
     * @return Description for the listener.
     */
    String getDescription();

    /**
     * @return The protocol of the listener, which is TCP, HTTP, or HTTPS.
     */
    ListenerProtocol getProtocol();

    /**
     * @return The protocol of the listener.
     */
    Integer getProtocolPort();

    /**
     * @return The connection limit of the listener.
     * -1 indicates an infinite limit.
     */
    Integer getConnectionLimit();

    /**
     * @return The default pool id of the listener.
     */
    String getDefaultPoolId();

    /**
     * @return the default tls container ref
     */
    String getDefaultTlsContainerRef();

    /**
     * @return the sni container refs
     */
    List<String> getSniContainerRefs();

    /**
     * @return The administrative state of the listener, which is up (true) or
     * down (false).
     */
    boolean isAdminStateUp();

    /**
     * @return The loadbalancers of the listener.
     */
    List<ListItem> getLoadBalancers();

    /** @return the provisioning status of the listener, or null */
    default LbProvisioningStatus getProvisioningStatus() {
        return null;
    }

    /** @return the operating status of the listener, or null */
    default LbOperatingStatus getOperatingStatus() {
        return null;
    }

    /** @return the creation time (response only), or null */
    default Date getCreatedAt() {
        return null;
    }

    /** @return the last update time (response only), or null */
    default Date getUpdatedAt() {
        return null;
    }

    /** @return the headers inserted into requests to the members (for example X-Forwarded-For) (response only), or null */
    default Map<String, String> getInsertHeaders() {
        return null;
    }

    /** @return the client inactivity timeout in milliseconds (response only), or null */
    default Integer getTimeoutClientData() {
        return null;
    }

    /** @return the member connection timeout in milliseconds (response only), or null */
    default Integer getTimeoutMemberConnect() {
        return null;
    }

    /** @return the member inactivity timeout in milliseconds (response only), or null */
    default Integer getTimeoutMemberData() {
        return null;
    }

    /** @return the time to wait for additional TCP packets for content inspection in milliseconds (response only), or null */
    default Integer getTimeoutTcpInspect() {
        return null;
    }
}
