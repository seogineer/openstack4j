package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.VolumeTarget;
import org.openstack4j.model.baremetal.options.VolumeTargetCreate;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal volume targets ({@code /v1/volume/targets}) (microversion 1.32). */
public interface VolumeTargetService extends RestService {

    /** @return the volume targets with all fields */
    List<? extends VolumeTarget> list();

    /** @param filters query parameters such as {@code node}, {@code limit}, {@code marker} ({@code detail=true} is sent unless given) */
    List<? extends VolumeTarget> list(Map<String, String> filters);

    /** @return the volume target, or {@code null} when it does not exist */
    VolumeTarget get(String ident);

    VolumeTarget create(VolumeTargetCreate create);

    /** Updates with JSON Patch operations. */
    VolumeTarget update(String ident, List<BaremetalPatch> patches);

    /** @return the volume targets of a node with all fields; a missing node raises */
    List<? extends VolumeTarget> listByNode(String nodeIdent);

    ActionResponse delete(String ident);
}
