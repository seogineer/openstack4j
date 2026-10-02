package org.openstack4j.api.placement.v1;

import org.openstack4j.common.RestService;
import org.openstack4j.model.placement.v1.AllocationCandidates;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery;

/** Scheduling candidates ({@code GET /allocation_candidates}). */
public interface AllocationCandidateService extends RestService {

    AllocationCandidates list(AllocationCandidatesQuery query);
}
