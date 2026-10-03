package org.openstack4j.openstack.networking.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.networking.ext.SubnetPoolService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.SubnetPool;
import org.openstack4j.model.network.options.SubnetPoolOptions;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.networking.domain.ext.NeutronPrefixes;
import org.openstack4j.openstack.networking.domain.ext.NeutronSubnetPool;
import org.openstack4j.openstack.networking.domain.ext.NeutronSubnetPool.SubnetPools;

public class SubnetPoolServiceImpl extends BaseNeutronExtService implements SubnetPoolService {

    private static final String POOLS = "/subnetpools";
    private static final String ROOT = "subnetpool";

    @Override public List<? extends SubnetPool> list() { return listOf(SubnetPools.class, POOLS, null); }
    @Override public List<? extends SubnetPool> list(Map<String, String> filters) { return listOf(SubnetPools.class, POOLS, filters); }
    @Override public SubnetPool get(String id) { return show(NeutronSubnetPool.class, POOLS + "/" + id(id)); }
    @Override public SubnetPool create(SubnetPoolOptions options) { return create(NeutronSubnetPool.class, POOLS, ROOT, options); }
    @Override public SubnetPool update(String id, SubnetPoolOptions options) { return update(NeutronSubnetPool.class, POOLS + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(POOLS + "/" + id(id)); }

    private List<String> prefixes(String id, String action, List<String> prefixes) {
        NeutronPrefixes result = put(NeutronPrefixes.class, POOLS + "/" + id(id) + "/" + action)
                .entity(JsonBody.of(Collections.singletonMap("prefixes", Objects.requireNonNull(prefixes)))).execute(NeutronExecution.propagate404());
        return result == null || result.getPrefixes() == null ? Collections.emptyList() : result.getPrefixes();
    }

    @Override public List<String> addPrefixes(String id, List<String> prefixes) { return prefixes(id, "add_prefixes", prefixes); }
    @Override public List<String> removePrefixes(String id, List<String> prefixes) { return prefixes(id, "remove_prefixes", prefixes); }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> onboardNetworkSubnets(String id, String networkId) {
        List<Map<String, Object>> result = put(List.class, POOLS + "/" + id(id) + "/onboard_network_subnets")
                .entity(JsonBody.of(Collections.singletonMap("network_id", Objects.requireNonNull(networkId)))).execute(NeutronExecution.propagate404());
        return result == null ? Collections.emptyList() : result;
    }
}
