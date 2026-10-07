package org.openstack4j.openstack.heat.internal.ext;

import java.util.List;

import org.openstack4j.api.heat.ext.TemplateVersionService;
import org.openstack4j.model.heat.ext.TemplateFunction;
import org.openstack4j.model.heat.ext.TemplateVersion;
import org.openstack4j.openstack.heat.domain.ext.HeatTemplateFunction.TemplateFunctions;
import org.openstack4j.openstack.heat.domain.ext.HeatTemplateVersion.TemplateVersions;

public class TemplateVersionServiceImpl extends BaseHeatExtService implements TemplateVersionService {

    @Override public List<? extends TemplateVersion> list() { return listOf(TemplateVersions.class, "/template_versions", null); }
    @Override public List<? extends TemplateFunction> functions(String version) { return listOf(TemplateFunctions.class, "/template_versions/" + id(version) + "/functions", null); }
}
