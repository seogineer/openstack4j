package org.openstack4j.model.heat.ext;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** A template format version Heat understands. */
public interface TemplateVersion extends ModelEntity {
    String getVersion();
    String getType();
    List<String> getAliases();
}
