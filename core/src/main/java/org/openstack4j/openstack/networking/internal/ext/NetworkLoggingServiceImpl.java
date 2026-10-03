package org.openstack4j.openstack.networking.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.NetworkLoggingService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.NetworkLog;
import org.openstack4j.model.network.options.NetworkLogOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronLoggableResources;
import org.openstack4j.openstack.networking.domain.ext.NeutronNetworkLog;
import org.openstack4j.openstack.networking.domain.ext.NeutronNetworkLog.Logs;

public class NetworkLoggingServiceImpl extends BaseNeutronExtService implements NetworkLoggingService {

    private static final String PATH = "/log/logs";
    private static final String ROOT = "log";

    @Override public List<? extends NetworkLog> list() { return listOf(Logs.class, PATH, null); }
    @Override public List<? extends NetworkLog> list(Map<String, String> filters) { return listOf(Logs.class, PATH, filters); }
    @Override public NetworkLog get(String id) { return show(NeutronNetworkLog.class, PATH + "/" + id(id)); }
    @Override public NetworkLog create(NetworkLogOptions options) { return create(NeutronNetworkLog.class, PATH, ROOT, options); }
    @Override public NetworkLog update(String id, NetworkLogOptions options) { return update(NeutronNetworkLog.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }

    @Override
    public List<String> loggableResources() {
        NeutronLoggableResources resources = showStrict(NeutronLoggableResources.class, "/log/loggable-resources");
        return resources == null ? Collections.emptyList() : resources.getTypes();
    }
}
