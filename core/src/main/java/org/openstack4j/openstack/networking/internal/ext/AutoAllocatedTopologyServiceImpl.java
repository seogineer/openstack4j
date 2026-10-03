package org.openstack4j.openstack.networking.internal.ext;

import org.openstack4j.api.networking.ext.AutoAllocatedTopologyService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.AutoAllocatedTopology;
import org.openstack4j.openstack.networking.domain.ext.NeutronAutoAllocatedTopology;

public class AutoAllocatedTopologyServiceImpl extends BaseNeutronExtService implements AutoAllocatedTopologyService {

    private static String path(String projectId) {
        return "/auto-allocated-topology/" + id(projectId);
    }

    @Override public AutoAllocatedTopology get(String projectId) { return show(NeutronAutoAllocatedTopology.class, path(projectId)); }
    @Override public AutoAllocatedTopology validate(String projectId) { return get(NeutronAutoAllocatedTopology.class, path(projectId)).param("fields", "dry-run").execute(); }
    @Override public ActionResponse delete(String projectId) { return remove(path(projectId)); }
}
