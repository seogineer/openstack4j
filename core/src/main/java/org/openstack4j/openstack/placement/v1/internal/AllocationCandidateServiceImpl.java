package org.openstack4j.openstack.placement.v1.internal;

import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.placement.v1.AllocationCandidateService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.placement.v1.AllocationCandidates;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery.RequestGroup;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.openstack.placement.v1.domain.PlacementAllocationCandidates;

public class AllocationCandidateServiceImpl extends BasePlacementV1Service implements AllocationCandidateService {

    @Override
    public AllocationCandidates list(AllocationCandidatesQuery query) {
        Objects.requireNonNull(query, "query");
        checkVersions(query);
        Invocation<PlacementAllocationCandidates> invocation = placement(HttpMethod.GET, PlacementAllocationCandidates.class, "/allocation_candidates");
        for (Map.Entry<String, RequestGroup> entry : query.getGroups().entrySet()) {
            String suffix = entry.getKey();
            RequestGroup group = entry.getValue();
            invocation.param("resources" + suffix, group.resourcesParameter());
            invocation.param("required" + suffix, group.requiredParameter());
            invocation.param("in_tree" + suffix, group.getInTree());
            for (String aggregate : group.getMemberOf())
                invocation.param("member_of" + suffix, aggregate);
        }
        if (query.getGroupPolicy() != null) invocation.param("group_policy", query.getGroupPolicy().parameterValue());
        invocation.param("limit", query.getLimit());
        if (!query.getRootRequired().isEmpty()) invocation.param("root_required", String.join(",", query.getRootRequired()));
        if (!query.getSameSubtree().isEmpty()) invocation.param("same_subtree", String.join(",", query.getSameSubtree()));
        return executeOrThrow(invocation);
    }

    private void checkVersions(AllocationCandidatesQuery query) {
        for (Map.Entry<String, RequestGroup> entry : query.getGroups().entrySet()) {
            String suffix = entry.getKey();
            RequestGroup group = entry.getValue();
            if (!suffix.isEmpty() && !suffix.matches("\\d+"))
                requireMicroVersion("non-numeric request group suffixes such as '" + suffix + "'", PlacementMicroVersions.V1_33);
            if (group.getInTree() != null)
                requireMicroVersion("in_tree in allocation candidates", PlacementMicroVersions.V1_31);
            if (group.getMemberOf().stream().anyMatch(m -> m.startsWith("!")))
                requireMicroVersion("forbidden aggregates (member_of=!...)", PlacementMicroVersions.V1_32);
            if (group.getRequired().stream().anyMatch(t -> t.startsWith("in:")))
                requireMicroVersion("the trait 'in:' syntax", PlacementMicroVersions.V1_39);
        }
        if (!query.getRootRequired().isEmpty())
            requireMicroVersion("root_required", PlacementMicroVersions.V1_35);
        if (!query.getSameSubtree().isEmpty())
            requireMicroVersion("same_subtree", PlacementMicroVersions.V1_36);
    }
}
