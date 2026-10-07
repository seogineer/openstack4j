package org.openstack4j.openstack.heat.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.heat.ext.SoftwareDeploymentService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.heat.ext.SoftwareDeployment;
import org.openstack4j.model.heat.options.SoftwareDeploymentOptions;
import org.openstack4j.openstack.heat.domain.ext.HeatDeploymentMetadata;
import org.openstack4j.openstack.heat.domain.ext.HeatSoftwareDeployment;
import org.openstack4j.openstack.heat.domain.ext.HeatSoftwareDeployment.SoftwareDeployments;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class SoftwareDeploymentServiceImpl extends BaseHeatExtService implements SoftwareDeploymentService {

    private static final String PATH = "/software_deployments";

    @Override public List<? extends SoftwareDeployment> list() { return listOf(SoftwareDeployments.class, PATH, null); }
    @Override public List<? extends SoftwareDeployment> list(Map<String, String> filters) { return listOf(SoftwareDeployments.class, PATH, filters); }
    @Override public SoftwareDeployment get(String id) { return show(HeatSoftwareDeployment.class, PATH + "/" + id(id)); }

    /** The request body has no root key; the response is wrapped in software_deployment. */
    @Override
    public SoftwareDeployment create(SoftwareDeploymentOptions options) {
        return post(HeatSoftwareDeployment.class, PATH).entity(JsonBody.of(Objects.requireNonNull(options).toMap())).execute(propagate404());
    }

    @Override
    public SoftwareDeployment update(String id, SoftwareDeploymentOptions options) {
        return put(HeatSoftwareDeployment.class, PATH + "/" + id(id)).entity(JsonBody.of(Objects.requireNonNull(options).toMap())).execute(propagate404());
    }

    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }

    @Override
    public List<Map<String, Object>> metadata(String serverId) {
        HeatDeploymentMetadata metadata = showStrict(HeatDeploymentMetadata.class, PATH + "/metadata/" + id(serverId));
        return metadata == null || metadata.getMetadata() == null ? Collections.emptyList() : metadata.getMetadata();
    }
}
