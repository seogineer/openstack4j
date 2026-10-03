package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.NeutronServiceProfileService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.ServiceProfile;
import org.openstack4j.model.network.options.ServiceProfileOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronServiceProfile;
import org.openstack4j.openstack.networking.domain.ext.NeutronServiceProfile.ServiceProfiles;

public class NeutronServiceProfileServiceImpl extends BaseNeutronExtService implements NeutronServiceProfileService {

    private static final String PATH = "/service_profiles";
    private static final String ROOT = "service_profile";

    @Override public List<? extends ServiceProfile> list() { return listOf(ServiceProfiles.class, PATH, null); }
    @Override public List<? extends ServiceProfile> list(Map<String, String> filters) { return listOf(ServiceProfiles.class, PATH, filters); }
    @Override public ServiceProfile get(String id) { return show(NeutronServiceProfile.class, PATH + "/" + id(id)); }
    @Override public ServiceProfile create(ServiceProfileOptions options) { return create(NeutronServiceProfile.class, PATH, ROOT, options); }
    @Override public ServiceProfile update(String id, ServiceProfileOptions options) { return update(NeutronServiceProfile.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
