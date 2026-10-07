package org.openstack4j.openstack.octavia.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.octavia.ext.AmphoraService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.Amphora;
import org.openstack4j.model.octavia.ext.AmphoraStats;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaAmphora;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaAmphora.Amphorae;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaAmphoraStats.Stats;

public class AmphoraServiceImpl extends BaseOctaviaExtService implements AmphoraService {

    private static final String PATH = "/octavia/amphorae";

    @Override public List<? extends Amphora> list() { return listOf(Amphorae.class, PATH, null); }
    @Override public List<? extends Amphora> list(Map<String, String> filters) { return listOf(Amphorae.class, PATH, filters); }
    @Override public Amphora get(String id) { return show(OctaviaAmphora.class, PATH + "/" + id(id)); }
    @Override public List<? extends AmphoraStats> stats(String id) { return listOf(Stats.class, PATH + "/" + id(id) + "/stats", null); }
    @Override public ActionResponse configure(String id) { return putWithResponse(PATH + "/" + id(id) + "/config").execute(); }
    @Override public ActionResponse failover(String id) { return putWithResponse(PATH + "/" + id(id) + "/failover").execute(); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
