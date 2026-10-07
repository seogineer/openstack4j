package org.openstack4j.model.baremetal.options;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v1/allocations}. */
public final class AllocationCreate extends BaremetalAttributes<AllocationCreate> {

    private AllocationCreate() {
    }

    public static AllocationCreate create(String resourceClass) {
        return new AllocationCreate().put("resource_class", Objects.requireNonNull(resourceClass, "resourceClass"));
    }

    @Override
    protected AllocationCreate self() {
        return this;
    }

    public AllocationCreate name(String name) {
        return put("name", name);
    }

    public AllocationCreate uuid(String uuid) {
        return put("uuid", uuid);
    }

    /** Node names or UUIDs to pick from. */
    public AllocationCreate candidateNodes(List<String> candidateNodes) {
        return put("candidate_nodes", candidateNodes);
    }

    /** Traits the node must have. */
    public AllocationCreate traits(List<String> traits) {
        return put("traits", traits);
    }

    public AllocationCreate extra(Map<String, ?> extra) {
        return put("extra", extra);
    }

    /** Backfills an allocation for a node that is already deployed (microversion 1.58). */
    public AllocationCreate node(String node) {
        return put("node", node);
    }

    /** Needs microversion 1.60. */
    public AllocationCreate owner(String owner) {
        return put("owner", owner);
    }
}
