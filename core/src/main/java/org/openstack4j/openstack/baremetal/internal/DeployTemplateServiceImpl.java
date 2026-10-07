package org.openstack4j.openstack.baremetal.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.DeployTemplateService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.DeployTemplate;
import org.openstack4j.model.baremetal.options.DeployTemplateCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.baremetal.domain.IronicDeployTemplate;
import org.openstack4j.openstack.baremetal.domain.IronicDeployTemplate.IronicDeployTemplateList;

public class DeployTemplateServiceImpl extends BaseBaremetalServices implements DeployTemplateService {

    @Override
    public List<? extends DeployTemplate> list() {
        return list(null);
    }

    @Override
    public List<? extends DeployTemplate> list(Map<String, String> filters) {
        Map<String, String> query = filters == null ? new HashMap<>() : new HashMap<>(filters);
        query.putIfAbsent("detail", "true");
        return listOf(IronicDeployTemplateList.class, "/deploy_templates", query);
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
