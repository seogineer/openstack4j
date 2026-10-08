package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.SfcServiceGraphService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.SfcServiceGraph;
import org.openstack4j.model.network.options.SfcServiceGraphOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronSfcServiceGraph;
import org.openstack4j.openstack.networking.domain.ext.NeutronSfcServiceGraph.NeutronSfcServiceGraphList;

public class SfcServiceGraphServiceImpl extends BaseNeutronExtService implements SfcServiceGraphService {

    private static final String PATH = "/sfc/service_graphs";
    private static final String ROOT = "service_graph";

    @Override
    public List<? extends SfcServiceGraph> list() {
        return list(null);
    }

    @Override
    public List<? extends SfcServiceGraph> list(Map<String, String> filters) {
        return listOf(NeutronSfcServiceGraphList.class, PATH, filters);
    }

    @Override
    public SfcServiceGraph get(String id) {
        return show(NeutronSfcServiceGraph.class, PATH + "/" + id(id));
    }

    @Override
    public SfcServiceGraph create(SfcServiceGraphOptions options) {
        return create(NeutronSfcServiceGraph.class, PATH, ROOT, options);
    }

    @Override
    public SfcServiceGraph update(String id, SfcServiceGraphOptions options) {
        return update(NeutronSfcServiceGraph.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
