package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/** Connection details behind a console token (admin only; all console types from 2.31). */
public interface ConsoleConnectionInfo extends ModelEntity {
    String getInstanceUuid();
    String getHost();
    Integer getPort();
    /** 2.99+ */
    Integer getTlsPort();
    String getInternalAccessPath();
}
