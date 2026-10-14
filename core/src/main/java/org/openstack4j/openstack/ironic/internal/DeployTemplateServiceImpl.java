package org.openstack4j.openstack.ironic.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.ironic.DeployTemplateService;
import org.openstack4j.model.ironic.BaremetalPatch;
import org.openstack4j.model.ironic.DeployTemplate;
import org.openstack4j.model.ironic.options.DeployTemplateCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.ironic.domain.IronicDeployTemplate;
import org.openstack4j.openstack.ironic.domain.IronicDeployTemplate.IronicDeployTemplateList;

public class DeployTemplateServiceImpl extends BaseIronicServices implements DeployTemplateService {

    @Override
    public List<? extends DeployTemplate> list() {
        return list(null);
    }

    @Override
    public List<? extends DeployTemplate> list(Map<String, String> filters) {
        return listOf(IronicDeployTemplateList.class, "/deploy_templates", withDetail(filters));
    }

    @Override
    public DeployTemplate get(String ident) {
        return show(IronicDeployTemplate.class, "/deploy_templates/" + id(ident));
    }

    @Override
    public DeployTemplate create(DeployTemplateCreate create) {
        return create(IronicDeployTemplate.class, "/deploy_templates", create);
    }

    @Override
    public DeployTemplate update(String ident, List<BaremetalPatch> patches) {
        return patchWith(IronicDeployTemplate.class, "/deploy_templates/" + id(ident), patches);
    }

    @Override
    public ActionResponse delete(String ident) {
        return remove("/deploy_templates/" + id(ident));
    }
}
