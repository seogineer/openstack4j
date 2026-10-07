package org.openstack4j.model.heat.ext;

import org.openstack4j.model.ModelEntity;

/** An intrinsic function of a template version. */
public interface TemplateFunction extends ModelEntity {
    String getFunctions();
    String getDescription();
}
