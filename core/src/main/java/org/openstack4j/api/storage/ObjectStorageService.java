package org.openstack4j.api.storage;

import org.openstack4j.common.RestService;

/**
 * OpenStack Object Storage service
 *
 * @author Jeremy Unruh
 */
public interface ObjectStorageService extends RestService {

    /**
     * The Object Storage Account Service
     *
     * @return the account service
     */
    ObjectStorageAccountService account();

    /**
     * The Object Storage Container Service
     *
     * @return the container service
     */
    ObjectStorageContainerService containers();

    /**
     * The Object Storage file and directory service
     *
     * @return the object service
     */
    ObjectStorageObjectService objects();

    /** @return the cluster's capabilities and limits ({@code GET /info}, unauthenticated) */
    java.util.Map<String, Object> info();

    /**
     * @param container the container, or {@code null} for the account
     * @param object    the object in {@code container}, or {@code null}
     * @return the storage node URLs holding the account, container or object (list_endpoints middleware, if enabled)
     */
    java.util.List<String> listEndpoints(String container, String object);
}
