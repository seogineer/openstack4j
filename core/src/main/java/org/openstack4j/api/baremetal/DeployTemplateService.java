package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.DeployTemplate;
import org.openstack4j.model.baremetal.options.DeployTemplateCreate;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal deploy templates ({@code /v1/deploy_templates}) (microversion 1.55). */
public interface DeployTemplateService extends RestService {

    /** @return the deploy templates with all fields */
    List<? extends DeployTemplate> list();

    /** @param filters query parameters such as {@code limit}, {@code marker}, {@code sort_key} ({@code detail=true} is sent unless {@code detail} or {@code fields} is given) */
    List<? extends DeployTemplate> list(Map<String, String> filters);

    /** @return the deploy template, or {@code null} when it does not exist */
    DeployTemplate get(String ident);

    DeployTemplate create(DeployTemplateCreate create);

    /** Updates with JSON Patch operations. */
    DeployTemplate update(String ident, List<BaremetalPatch> patches);

    ActionResponse delete(String ident);
}
