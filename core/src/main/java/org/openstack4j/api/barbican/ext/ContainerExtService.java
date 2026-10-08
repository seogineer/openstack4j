package org.openstack4j.api.barbican.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Secrets and consumers of a container. */
public interface ContainerExtService extends RestService {

    /** Adds a secret to a generic container under {@code name}. */
    ActionResponse addSecret(String containerId, String name, String secretRef);

    ActionResponse removeSecret(String containerId, String name, String secretRef);

    /** @return the consumers of a container ({@code name}, {@code URL}, {@code status} …); a missing container raises */
    List<Map<String, Object>> listConsumers(String containerId);

    /** Registers a consumer (e.g. a load balancer) of a container. */
    ActionResponse registerConsumer(String containerId, String name, String url);

    ActionResponse removeConsumer(String containerId, String name, String url);
}
