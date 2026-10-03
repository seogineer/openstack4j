package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** The limit enforcement model of the deployment, for example "flat" or "strict-two-level". */
public interface LimitModel extends ModelEntity {
    String getName();
    String getDescription();
}
