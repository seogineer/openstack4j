package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.AddressScopeService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.AddressScope;
import org.openstack4j.model.network.options.AddressScopeOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronAddressScope;
import org.openstack4j.openstack.networking.domain.ext.NeutronAddressScope.AddressScopes;

public class AddressScopeServiceImpl extends BaseNeutronExtService implements AddressScopeService {

    private static final String PATH = "/address-scopes";
    private static final String ROOT = "address_scope";

    @Override public List<? extends AddressScope> list() { return listOf(AddressScopes.class, PATH, null); }
    @Override public List<? extends AddressScope> list(Map<String, String> filters) { return listOf(AddressScopes.class, PATH, filters); }
    @Override public AddressScope get(String id) { return show(NeutronAddressScope.class, PATH + "/" + id(id)); }
    @Override public AddressScope create(AddressScopeOptions options) { return create(NeutronAddressScope.class, PATH, ROOT, options); }
    @Override public AddressScope update(String id, AddressScopeOptions options) { return update(NeutronAddressScope.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
