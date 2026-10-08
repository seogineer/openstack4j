package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.TapServiceService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.TapService;
import org.openstack4j.model.network.options.TapServiceOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronTapService;
import org.openstack4j.openstack.networking.domain.ext.NeutronTapService.NeutronTapServiceList;

public class TapServiceServiceImpl extends BaseNeutronExtService implements TapServiceService {

    private static final String PATH = "/taas/tap_services";
    private static final String ROOT = "tap_service";

    @Override
    public List<? extends TapService> list() {
        return list(null);
    }

    @Override
    public List<? extends TapService> list(Map<String, String> filters) {
        return listOf(NeutronTapServiceList.class, PATH, filters);
    }

    @Override
    public TapService get(String id) {
        return show(NeutronTapService.class, PATH + "/" + id(id));
    }

    @Override
    public TapService create(TapServiceOptions options) {
        return create(NeutronTapService.class, PATH, ROOT, options);
    }

    @Override
    public TapService update(String id, TapServiceOptions options) {
        return update(NeutronTapService.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
