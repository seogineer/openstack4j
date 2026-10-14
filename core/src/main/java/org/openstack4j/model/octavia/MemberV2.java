package org.openstack4j.model.octavia;

import java.util.Date;
import org.openstack4j.common.Buildable;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.octavia.builder.MemberV2Builder;

/**
 * A member of a Lbaas V2 pool
 *
 * @author wei
 */
public interface MemberV2 extends ModelEntity, Buildable<MemberV2Builder> {
    /**
     * @return the member identifier
     */
    String getId();

    /**
     * @return the ID of the project/tenant that owns the resource
     */
    String getProjectId();

    /**
     * @return address the IP address of a member
     */
    String getAddress();

    /**
     * @return The port on which the application is hosted.such as 80
     */
    Integer getProtocolPort();

    /**
     * @return Weight of member.1~256
     */
    Integer getWeight();

    /**
     * @return The subnet in which to access the member
     */
    String getSubnetId();

    /**
     * @return The administrative state of the member, which is up (true) or
     * down (false).
     */
    boolean isAdminStateUp();

    /** @return the name of the member, or null */
    default String getName() {
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

    /** @return the operating status (response only), or null */
    default LbOperatingStatus getOperatingStatus() {
        return null;
    }

    /** @return the provisioning status (response only), or null */
    default LbProvisioningStatus getProvisioningStatus() {
        return null;
    }

    /** @return whether this is a backup member (response only), or null */
    default Boolean getBackup() {
        return null;
    }

    /** @return the address used for health monitoring (response only), or null */
    default String getMonitorAddress() {
        return null;
    }

    /** @return the port used for health monitoring (response only), or null */
    default Integer getMonitorPort() {
        return null;
    }
}
