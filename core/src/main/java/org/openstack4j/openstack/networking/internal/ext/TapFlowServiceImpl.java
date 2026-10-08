package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.TapFlowService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.TapFlow;
import org.openstack4j.model.network.options.TapFlowOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronTapFlow;
import org.openstack4j.openstack.networking.domain.ext.NeutronTapFlow.NeutronTapFlowList;

public class TapFlowServiceImpl extends BaseNeutronExtService implements TapFlowService {

    private static final String PATH = "/taas/tap_flows";
    private static final String ROOT = "tap_flow";

    @Override
    public List<? extends TapFlow> list() {
        return list(null);
    }

    @Override
    public List<? extends TapFlow> list(Map<String, String> filters) {
        return listOf(NeutronTapFlowList.class, PATH, filters);
    }

    @Override
    public TapFlow get(String id) {
        return show(NeutronTapFlow.class, PATH + "/" + id(id));
    }

    @Override
    public TapFlow create(TapFlowOptions options) {
        return create(NeutronTapFlow.class, PATH, ROOT, options);
    }

    @Override
    public TapFlow update(String id, TapFlowOptions options) {
        return update(NeutronTapFlow.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
