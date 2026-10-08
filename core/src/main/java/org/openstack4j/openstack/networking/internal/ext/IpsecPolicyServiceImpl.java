package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.IpsecPolicyService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.IpsecPolicy;
import org.openstack4j.model.network.options.IpsecPolicyOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronIpsecPolicy;
import org.openstack4j.openstack.networking.domain.ext.NeutronIpsecPolicy.NeutronIpsecPolicyList;

public class IpsecPolicyServiceImpl extends BaseNeutronExtService implements IpsecPolicyService {

    private static final String PATH = "/vpn/ipsecpolicies";
    private static final String ROOT = "ipsecpolicy";

    @Override
    public List<? extends IpsecPolicy> list() {
        return list(null);
    }

    @Override
    public List<? extends IpsecPolicy> list(Map<String, String> filters) {
        return listOf(NeutronIpsecPolicyList.class, PATH, filters);
    }

    @Override
    public IpsecPolicy get(String id) {
        return show(NeutronIpsecPolicy.class, PATH + "/" + id(id));
    }

    @Override
    public IpsecPolicy create(IpsecPolicyOptions options) {
        return create(NeutronIpsecPolicy.class, PATH, ROOT, options);
    }

    @Override
    public IpsecPolicy update(String id, IpsecPolicyOptions options) {
        return update(NeutronIpsecPolicy.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
