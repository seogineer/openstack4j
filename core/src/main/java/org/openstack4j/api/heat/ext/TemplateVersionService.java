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
     * @return the template versions
     */
    List<? extends TemplateVersion> list();

    /**
     * Lists the intrinsic functions of a template version.
     *
     * @throws org.openstack4j.api.exceptions.ResponseException if the version is unknown (404)
     * @param version the version
     * @return the functions of the version
     */
    List<? extends TemplateFunction> functions(String version);
}
