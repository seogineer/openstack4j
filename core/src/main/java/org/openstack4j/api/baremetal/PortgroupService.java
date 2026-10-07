package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Portgroup;
import org.openstack4j.model.baremetal.options.PortgroupCreate;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal port groups ({@code /v1/portgroups}). */
public interface PortgroupService extends RestService {

    /** @return the port groups (summary fields; use {@code listDetail()} for all fields) */
    List<? extends Portgroup> list();

    /** @param filters query parameters such as {@code node}, {@code address}, {@code limit}, {@code marker}, {@code fields} */
    List<? extends Portgroup> list(Map<String, String> filters);

    /** @return the port groups with all fields */
    List<? extends Portgroup> listDetail();

    List<? extends Portgroup> listDetail(Map<String, String> filters);

    /** @return the port groups of a node with all fields ({@code GET /v1/nodes/{node}/portgroups/detail}, microversion 1.24); a missing node raises */
    List<? extends Portgroup> listByNode(String nodeIdent);

    /** @return the port group, or {@code null} when it does not exist */
    Portgroup get(String ident);

    Portgroup create(PortgroupCreate create);

    /** Updates with JSON Patch operations, e.g. {@code BaremetalPatch.replace("/extra/rack", "3")}. */
    Portgroup update(String ident, List<BaremetalPatch> patches);

    ActionResponse delete(String ident);
}
