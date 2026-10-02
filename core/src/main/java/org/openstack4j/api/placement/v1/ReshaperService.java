package org.openstack4j.api.placement.v1;

import org.openstack4j.common.RestService;
import org.openstack4j.model.placement.v1.ReshapeRequest;

/** Atomic inventory and allocation migration ({@code POST /reshaper}, placement 1.30). */
public interface ReshaperService extends RestService {

    void reshape(ReshapeRequest request);
}
