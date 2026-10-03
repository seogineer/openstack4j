package org.openstack4j.model.identity.v3;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** A prior role and the roles it implies. */
public interface RoleInference extends ModelEntity {
    Role getPriorRole();
    List<? extends Role> getImplies();
}
