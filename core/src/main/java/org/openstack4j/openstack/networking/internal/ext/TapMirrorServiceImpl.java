package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.TapMirrorService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.TapMirror;
import org.openstack4j.model.network.options.TapMirrorOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronTapMirror;
import org.openstack4j.openstack.networking.domain.ext.NeutronTapMirror.NeutronTapMirrorList;

public class TapMirrorServiceImpl extends BaseNeutronExtService implements TapMirrorService {

    private static final String PATH = "/taas/tap_mirrors";
    private static final String ROOT = "tap_mirror";

    @Override
    public List<? extends TapMirror> list() {
        return list(null);
    }

    @Override
    public List<? extends TapMirror> list(Map<String, String> filters) {
        return listOf(NeutronTapMirrorList.class, PATH, filters);
    }

    @Override
    public TapMirror get(String id) {
        return show(NeutronTapMirror.class, PATH + "/" + id(id));
    }

    @Override
    public TapMirror create(TapMirrorOptions options) {
        return create(NeutronTapMirror.class, PATH, ROOT, options);
    }

    @Override
    public TapMirror update(String id, TapMirrorOptions options) {
        return update(NeutronTapMirror.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
