package org.openstack4j.openstack.networking.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.ServiceFlavorService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.ServiceFlavor;
import org.openstack4j.model.network.options.ServiceFlavorOptions;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.networking.domain.ext.NeutronServiceFlavor;
import org.openstack4j.openstack.networking.domain.ext.NeutronServiceFlavor.Flavors;

public class ServiceFlavorServiceImpl extends BaseNeutronExtService implements ServiceFlavorService {

    private static final String PATH = "/flavors";
    private static final String ROOT = "flavor";

    @Override public List<? extends ServiceFlavor> list() { return listOf(Flavors.class, PATH, null); }
    @Override public List<? extends ServiceFlavor> list(Map<String, String> filters) { return listOf(Flavors.class, PATH, filters); }
    @Override public ServiceFlavor get(String id) { return show(NeutronServiceFlavor.class, PATH + "/" + id(id)); }
    @Override public ServiceFlavor create(ServiceFlavorOptions options) { return create(NeutronServiceFlavor.class, PATH, ROOT, options); }
    @Override public ServiceFlavor update(String id, ServiceFlavorOptions options) { return update(NeutronServiceFlavor.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }

    @Override
    public ActionResponse associateProfile(String flavorId, String profileId) {
        return postWithResponse(PATH + "/" + id(flavorId) + "/service_profiles")
                .entity(JsonBody.of("service_profile", Collections.singletonMap("id", id(profileId)))).execute();
    }

    @Override
    public ActionResponse disassociateProfile(String flavorId, String profileId) {
        return remove(PATH + "/" + id(flavorId) + "/service_profiles/" + id(profileId));
    }
}
