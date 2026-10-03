package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** An access rule of an application credential: which API calls the credential may make. */
public interface AccessRule extends ModelEntity {
    String getId();
    String getService();
    String getPath();
    String getMethod();
}
