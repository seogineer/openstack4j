package org.openstack4j.openstack.compute.internal.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.compute.ext.InterfaceService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.InterfaceAttachment;
import org.openstack4j.openstack.compute.domain.JsonBody;
import org.openstack4j.openstack.compute.domain.NovaInterfaceAttachment;
import org.openstack4j.openstack.compute.domain.NovaInterfaceAttachment.NovaInterfaceAttachments;
import org.openstack4j.openstack.compute.internal.BaseComputeServices;

/**
 * API to Create, list, get details for, and delete port interfaces on a Server Instance
 *
 * @author Jeremy Unruh
 */
public class InterfaceServiceImpl extends BaseComputeServices implements InterfaceService {

    @Override
    public InterfaceAttachment create(String serverId, String portId) {
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(portId, "portId");
        return post(NovaInterfaceAttachment.class, uri("/servers/%s/os-interface", serverId))
                .entity(new NovaInterfaceAttachment(portId))
                .execute();
    }

    @Override
    public InterfaceAttachment attachToNetwork(String serverId, String netId, String ipAddress) {
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(netId, "netId");
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("net_id", netId);
        if (ipAddress != null) {
            attachment.put("fixed_ips", List.of(Map.of("ip_address", ipAddress)));
        }
        return post(NovaInterfaceAttachment.class, uri("/servers/%s/os-interface", serverId))
                .entity(JsonBody.of("interfaceAttachment", attachment))
                .execute();
    }

    @Override
    public List<? extends InterfaceAttachment> list(String serverId) {
        Objects.requireNonNull(serverId, "serverId");
        return get(NovaInterfaceAttachments.class, uri("/servers/%s/os-interface", serverId))
                .execute().getList();
    }

    @Override
    public InterfaceAttachment get(String serverId, String attachmentId) {
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(attachmentId, "attachmentId");
        return get(NovaInterfaceAttachment.class, uri("/servers/%s/os-interface/%s", serverId, attachmentId)).execute();
    }

    @Override
    public ActionResponse detach(String serverId, String attachmentId) {
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(attachmentId, "attachmentId");
        return delete(ActionResponse.class, uri("/servers/%s/os-interface/%s", serverId, attachmentId)).execute();
    }

}
