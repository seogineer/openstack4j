package org.openstack4j.openstack.octavia.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.octavia.ext.FlavorProfileService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.FlavorProfile;
import org.openstack4j.model.octavia.options.FlavorProfileOptions;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaFlavorProfile;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaFlavorProfile.FlavorProfiles;

public class FlavorProfileServiceImpl extends BaseOctaviaExtService implements FlavorProfileService {

    private static final String PATH = "/lbaas/flavorprofiles";
    private static final String ROOT = "flavorprofile";

    @Override public List<? extends FlavorProfile> list() { return listOf(FlavorProfiles.class, PATH, null); }
    @Override public List<? extends FlavorProfile> list(Map<String, String> filters) { return listOf(FlavorProfiles.class, PATH, filters); }
    @Override public FlavorProfile get(String id) { return show(OctaviaFlavorProfile.class, PATH + "/" + id(id)); }
    @Override public FlavorProfile create(FlavorProfileOptions options) { return create(OctaviaFlavorProfile.class, PATH, ROOT, options); }
    @Override public FlavorProfile update(String id, FlavorProfileOptions options) { return update(OctaviaFlavorProfile.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
