package org.openstack4j.openstack.networking.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.networking.ext.AddressGroupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.AddressGroup;
import org.openstack4j.model.network.options.AddressGroupOptions;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.networking.domain.ext.NeutronAddressGroup;
import org.openstack4j.openstack.networking.domain.ext.NeutronAddressGroup.AddressGroups;

public class AddressGroupServiceImpl extends BaseNeutronExtService implements AddressGroupService {

    private static final String GROUPS = "/address-groups";
    private static final String ROOT = "address_group";

    @Override public List<? extends AddressGroup> list() { return listOf(AddressGroups.class, GROUPS, null); }
    @Override public List<? extends AddressGroup> list(Map<String, String> filters) { return listOf(AddressGroups.class, GROUPS, filters); }
    @Override public AddressGroup get(String id) { return show(NeutronAddressGroup.class, GROUPS + "/" + id(id)); }
    @Override public AddressGroup create(AddressGroupOptions options) { return create(NeutronAddressGroup.class, GROUPS, ROOT, options); }
    @Override public AddressGroup update(String id, AddressGroupOptions options) { return update(NeutronAddressGroup.class, GROUPS + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(GROUPS + "/" + id(id)); }

    private AddressGroup addresses(String id, String action, List<String> addresses) {
        return put(NeutronAddressGroup.class, GROUPS + "/" + id(id) + "/" + action)
                .entity(JsonBody.of(Collections.singletonMap("addresses", Objects.requireNonNull(addresses)))).execute();
    }

    @Override public AddressGroup addAddresses(String id, List<String> addresses) { return addresses(id, "add_addresses", addresses); }
    @Override public AddressGroup removeAddresses(String id, List<String> addresses) { return addresses(id, "remove_addresses", addresses); }
}
