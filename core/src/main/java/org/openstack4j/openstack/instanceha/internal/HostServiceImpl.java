package org.openstack4j.openstack.instanceha.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.instanceha.HostService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.instanceha.Host;
import org.openstack4j.model.instanceha.options.HostOptions;
import org.openstack4j.openstack.instanceha.domain.MasakariHost;
import org.openstack4j.openstack.instanceha.domain.MasakariHost.MasakariHostList;

public class HostServiceImpl extends BaseMasakariService implements HostService {

    private static String hosts(String segmentId) {
        return "/segments/" + id(segmentId) + "/hosts";
    }

    @Override
    public List<? extends Host> list(String segmentId, Map<String, String> filters) {
        return listOf(MasakariHostList.class, hosts(segmentId), filters);
    }

    @Override
    public Host get(String segmentId, String hostId) {
        return show(MasakariHost.class, hosts(segmentId) + "/" + id(hostId));
    }

    @Override
    public Host create(String segmentId, HostOptions options) {
        return create(MasakariHost.class, hosts(segmentId), "host", options);
    }

    @Override
    public Host update(String segmentId, String hostId, HostOptions options) {
        return update(MasakariHost.class, hosts(segmentId) + "/" + id(hostId), "host", options);
    }

    @Override
    public ActionResponse delete(String segmentId, String hostId) {
        return remove(hosts(segmentId) + "/" + id(hostId));
    }
}
