package org.openstack4j.model.barbican.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** The {@code read} ACL of a secret or container. */
public interface BarbicanAcl extends ModelEntity {
    /** @return the users allowed to read, or {@code null} when none were set */
    List<String> getUsers();
    /** @return whether every user of the project may read; {@code false} limits reading to the listed users */
    Boolean isProjectAccess();
    String getCreated();
    String getUpdated();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
