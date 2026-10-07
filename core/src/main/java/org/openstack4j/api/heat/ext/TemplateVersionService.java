package org.openstack4j.api.heat.ext;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.heat.ext.TemplateFunction;
import org.openstack4j.model.heat.ext.TemplateVersion;

/**
 * Heat template versions ({@code /template_versions}).
 */
public interface TemplateVersionService extends RestService {

    /**
     * Lists the template versions Heat understands.
     *
     * @return the result
     */
    List<? extends TemplateVersion> list();

    /**
     * Lists the intrinsic functions of a template version.
     *
     * @param version the version
     * @return the result
     */
    List<? extends TemplateFunction> functions(String version);
}
