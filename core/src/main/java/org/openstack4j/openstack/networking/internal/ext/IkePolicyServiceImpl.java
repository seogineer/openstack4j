package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.IkePolicyService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.IkePolicy;
import org.openstack4j.model.network.options.IkePolicyOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronIkePolicy;
import org.openstack4j.openstack.networking.domain.ext.NeutronIkePolicy.NeutronIkePolicyList;

public class IkePolicyServiceImpl extends BaseNeutronExtService implements IkePolicyService {

    private static final String PATH = "/vpn/ikepolicies";
    private static final String ROOT = "ikepolicy";

    @Override
    public List<? extends IkePolicy> list() {
        return list(null);
    }

    @Override
    public List<? extends IkePolicy> list(Map<String, String> filters) {
        return listOf(NeutronIkePolicyList.class, PATH, filters);
    }

    @Override
    public IkePolicy get(String id) {
        return show(NeutronIkePolicy.class, PATH + "/" + id(id));
    }

    @Override
    public IkePolicy create(IkePolicyOptions options) {
        return create(NeutronIkePolicy.class, PATH, ROOT, options);
    }

    @Override
    public IkePolicy update(String id, IkePolicyOptions options) {
        return update(NeutronIkePolicy.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
