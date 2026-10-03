package org.openstack4j.openstack.networking.internal;

import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.networking.domain.ext.NeutronPortBinding.PortBindings;
import org.openstack4j.openstack.networking.domain.ext.NeutronPortBinding;
import org.openstack4j.model.network.options.PortBindingOptions;
import org.openstack4j.model.network.ext.PortBinding;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.networking.PortService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.options.PortListOptions;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronPort.Ports;
import org.openstack4j.openstack.networking.domain.NeutronPortCreate;

/**
 * OpenStack (Neutron) Port based Operations Implementation
 *
 * @author Jeremy Unruh
 */
public class PortServiceImpl extends BaseNetworkingServices implements PortService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Port> list() {
        return get(Ports.class, uri("/ports")).execute().getList();
    }

    @Override
    public List<? extends Port> list(PortListOptions options) {
        if (options == null)
            return list();

        return get(Ports.class, uri("/ports")).params(options.getOptions()).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Port> list(Map<String, ? extends Iterable<?>> params) {
        return get(Ports.class, uri("/ports")).paramLists(params).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Port get(String portId) {
        Objects.requireNonNull(portId);
        return get(NeutronPort.class, uri("/ports/%s", portId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse delete(String portId) {
        Objects.requireNonNull(portId);
        return deleteWithResponse(uri("/ports/%s", portId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Port create(Port port) {
        Objects.requireNonNull(port);
        Objects.requireNonNull(port.getNetworkId(), "NetworkId is a required field");
        return post(NeutronPort.class, uri("/ports")).entity(NeutronPortCreate.fromPort(port)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Port> create(List<? extends Port> ports) {
        Objects.requireNonNull(ports);
        for (Port port : ports) {
            Objects.requireNonNull(port.getNetworkId(), "NetworkId is a required field");
        }
        return post(Ports.class, uri("/ports")).entity(NeutronPortCreate.NeutronPortsCreate.fromPorts(ports))
                .execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Port update(Port port) {
        Objects.requireNonNull(port);
        Objects.requireNonNull(port.getId());
        Port p = port.toBuilder().networkId(null).state(null).tenantId(null).macAddress(null)
                .vifType(null).vifDetails(null)
                .build();
        return put(NeutronPort.class, uri("/ports/%s", getAndClearIdentifier(p))).entity(p).execute();
    }

    private String getAndClearIdentifier(Port port) {
        String portId = port.getId();
        port.setId(null);
        return portId;
    }

    @Override public List<? extends PortBinding> listBindings(String portId) { return get(PortBindings.class, uri("/ports/%s/bindings", Objects.requireNonNull(portId))).execute().getList(); }
    @Override public PortBinding createBinding(String portId, PortBindingOptions options) { return post(NeutronPortBinding.class, uri("/ports/%s/bindings/", Objects.requireNonNull(portId))).entity(JsonBody.of("binding", options.toMap())).execute(); }
    @Override public PortBinding activateBinding(String portId, String host) { return put(NeutronPortBinding.class, uri("/ports/%s/bindings/%s/activate", Objects.requireNonNull(portId), Objects.requireNonNull(host))).execute(); }
    @Override public ActionResponse deleteBinding(String portId, String host) { return deleteWithResponse(uri("/ports/%s/bindings/%s", Objects.requireNonNull(portId), Objects.requireNonNull(host))).execute(); }
}
