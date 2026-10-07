package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Allocation;
import org.openstack4j.model.baremetal.options.AllocationCreate;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal allocations ({@code /v1/allocations}) (microversion 1.52). */
public interface AllocationService extends RestService {

    /** @return the allocations with all fields */
    List<? extends Allocation> list();

    /** @param filters query parameters such as {@code node}, {@code resource_class}, {@code state}, {@code owner}, {@code limit}, {@code marker} */
    List<? extends Allocation> list(Map<String, String> filters);

    /** @return the allocation, or {@code null} when it does not exist */
    Allocation get(String ident);

    Allocation create(AllocationCreate create);

    /** Updates with JSON Patch operations (microversion 1.57). */
    Allocation update(String ident, List<BaremetalPatch> patches);

    /** @return the allocation of a node, or {@code null} when it has none */
    Allocation getForNode(String nodeIdent);

    ActionResponse deleteForNode(String nodeIdent);

    ActionResponse delete(String ident);
}
