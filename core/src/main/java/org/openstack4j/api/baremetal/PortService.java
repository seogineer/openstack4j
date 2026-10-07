package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Port;
import org.openstack4j.model.baremetal.options.PortCreate;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal ports ({@code /v1/ports}). */
public interface PortService extends RestService {

    /** @return the ports (summary fields; use {@code listDetail()} for all fields) */
    List<? extends Port> list();

    /** @param filters query parameters such as {@code node}, {@code address}, {@code limit}, {@code marker}, {@code fields} */
    List<? extends Port> list(Map<String, String> filters);

    /** @return the ports with all fields */
    List<? extends Port> listDetail();

    List<? extends Port> listDetail(Map<String, String> filters);

    /** @return the ports of a node with all fields ({@code GET /v1/nodes/{node}/ports/detail}); a missing node raises */
    List<? extends Port> listByNode(String nodeIdent);

    /** @return the ports of a port group with all fields (microversion 1.24); a missing port group raises */
    List<? extends Port> listByPortgroup(String portgroupIdent);
    /** @return the port, or {@code null} when it does not exist */
    Port get(String ident);

    Port create(PortCreate create);

    /** Updates with JSON Patch operations, e.g. {@code BaremetalPatch.replace("/extra/rack", "3")}. */
    Port update(String ident, List<BaremetalPatch> patches);

    ActionResponse delete(String ident);
}
