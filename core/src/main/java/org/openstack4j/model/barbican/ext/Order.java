package org.openstack4j.model.barbican.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A Barbican order (asynchronous generation of a key, an asymmetric key pair or a certificate). */
public interface Order extends ModelEntity {
    String getOrderRef();
    /** @return {@code key}, {@code asymmetric} or {@code certificate} */
    String getType();
    /** @return {@code PENDING}, {@code ACTIVE} or {@code ERROR} */
    String getStatus();
    Map<String, Object> getMeta();
    /** @return the generated secret, once the order is active */
    String getSecretRef();
    /** @return the generated container (asymmetric orders), once the order is active */
    String getContainerRef();
    String getErrorStatusCode();
    String getErrorReason();
    String getCreatorId();
    String getCreated();
    String getUpdated();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
