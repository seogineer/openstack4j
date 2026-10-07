package org.openstack4j.openstack.octavia.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.octavia.ext.AvailabilityZoneProfileService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.AvailabilityZoneProfile;
import org.openstack4j.model.octavia.options.AvailabilityZoneProfileOptions;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaAvailabilityZoneProfile;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaAvailabilityZoneProfile.AvailabilityZoneProfiles;

public class AvailabilityZoneProfileServiceImpl extends BaseOctaviaExtService implements AvailabilityZoneProfileService {

    private static final String PATH = "/lbaas/availabilityzoneprofiles";
    private static final String ROOT = "availability_zone_profile";

    @Override public List<? extends AvailabilityZoneProfile> list() { return listOf(AvailabilityZoneProfiles.class, PATH, null); }
    @Override public List<? extends AvailabilityZoneProfile> list(Map<String, String> filters) { return listOf(AvailabilityZoneProfiles.class, PATH, filters); }
    @Override public AvailabilityZoneProfile get(String id) { return show(OctaviaAvailabilityZoneProfile.class, PATH + "/" + id(id)); }
    @Override public AvailabilityZoneProfile create(AvailabilityZoneProfileOptions options) { return create(OctaviaAvailabilityZoneProfile.class, PATH, ROOT, options); }
    @Override public AvailabilityZoneProfile update(String id, AvailabilityZoneProfileOptions options) { return update(OctaviaAvailabilityZoneProfile.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
