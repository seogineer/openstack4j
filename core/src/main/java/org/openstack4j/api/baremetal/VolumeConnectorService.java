package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.VolumeConnector;
import org.openstack4j.model.baremetal.options.VolumeConnectorCreate;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal volume connectors ({@code /v1/volume/connectors}) (microversion 1.32). */
public interface VolumeConnectorService extends RestService {

    /** @return the volume connectors with all fields */
    List<? extends VolumeConnector> list();

    /** @param filters query parameters such as {@code node}, {@code limit}, {@code marker} ({@code detail=true} is sent unless {@code detail} or {@code fields} is given) */
    List<? extends VolumeConnector> list(Map<String, String> filters);

    /** @return the volume connector, or {@code null} when it does not exist */
    VolumeConnector get(String ident);

    VolumeConnector create(VolumeConnectorCreate create);

    /** Updates with JSON Patch operations. */
    VolumeConnector update(String ident, List<BaremetalPatch> patches);

    /** @return the volume connectors of a node with all fields; a missing node raises */
    List<? extends VolumeConnector> listByNode(String nodeIdent);

    ActionResponse delete(String ident);
}
