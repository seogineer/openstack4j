package org.openstack4j.openstack.octavia.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.octavia.ext.OctaviaAvailabilityZoneService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.OctaviaAvailabilityZone;
import org.openstack4j.model.octavia.options.OctaviaAvailabilityZoneOptions;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaAvailabilityZoneEntity;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaAvailabilityZoneEntity.AvailabilityZones;

public class OctaviaAvailabilityZoneServiceImpl extends BaseOctaviaExtService implements OctaviaAvailabilityZoneService {

    private static final String PATH = "/lbaas/availabilityzones";
    private static final String ROOT = "availability_zone";

    @Override public List<? extends OctaviaAvailabilityZone> list() { return listOf(AvailabilityZones.class, PATH, null); }
    @Override public List<? extends OctaviaAvailabilityZone> list(Map<String, String> filters) { return listOf(AvailabilityZones.class, PATH, filters); }
    @Override public OctaviaAvailabilityZone get(String name) { return show(OctaviaAvailabilityZoneEntity.class, PATH + "/" + id(name)); }
    @Override public OctaviaAvailabilityZone create(OctaviaAvailabilityZoneOptions options) { return create(OctaviaAvailabilityZoneEntity.class, PATH, ROOT, options); }
    @Override public OctaviaAvailabilityZone update(String name, OctaviaAvailabilityZoneOptions options) { return update(OctaviaAvailabilityZoneEntity.class, PATH + "/" + id(name), ROOT, options); }
    @Override public ActionResponse delete(String name) { return remove(PATH + "/" + id(name)); }
}
