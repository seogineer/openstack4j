package org.openstack4j.openstack.baremetal.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.VolumeConnectorService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.VolumeConnector;
import org.openstack4j.model.baremetal.options.VolumeConnectorCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.baremetal.domain.IronicVolumeConnector;
import org.openstack4j.openstack.baremetal.domain.IronicVolumeConnector.IronicVolumeConnectorList;

public class VolumeConnectorServiceImpl extends BaseBaremetalServices implements VolumeConnectorService {

    @Override
    public List<? extends VolumeConnector> list() {
        return list(null);
    }

    @Override
    public List<? extends VolumeConnector> list(Map<String, String> filters) {
        return listOf(IronicVolumeConnectorList.class, "/volume/connectors", withDetail(filters));
    }

    @Override
    public VolumeConnector get(String ident) {
        return show(IronicVolumeConnector.class, "/volume/connectors/" + id(ident));
    }

    @Override
    public VolumeConnector create(VolumeConnectorCreate create) {
        return create(IronicVolumeConnector.class, "/volume/connectors", create);
    }

    @Override
    public VolumeConnector update(String ident, List<BaremetalPatch> patches) {
        return patchWith(IronicVolumeConnector.class, "/volume/connectors/" + id(ident), patches);
    }

    @Override
    public List<? extends VolumeConnector> listByNode(String nodeIdent) {
        return listOf(IronicVolumeConnectorList.class, "/nodes/" + id(nodeIdent) + "/volume/connectors", Map.of("detail", "true"));
    }

    @Override
    public ActionResponse delete(String ident) {
        return remove("/volume/connectors/" + id(ident));
    }
}
