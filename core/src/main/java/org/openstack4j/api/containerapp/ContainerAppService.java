package org.openstack4j.api.containerapp;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/**
 * Application containers (Zun v1): containers and their actions, images, hosts, services, capsules, quotas, quota
 * classes, availability zones, networks and registries. Results are {@code Map}s; a list returns its page (the items
 * under the resource's key and {@code next}, the following page's URL, if any).
 * <p>
 * Without a version header Zun answers at {@code container 1.1}. Methods and create fields that need a newer API send
 * {@code OpenStack-API-Version: container <version>} themselves (e.g. hosts 1.4, network detach 1.6, network attach
 * 1.8, network list 1.18, resize 1.19, a {@code command} list 1.20, quotas 1.26, registries 1.30, capsules 1.32,
 * {@code host} 1.39, {@code entrypoint} 1.40); {@link #useApiVersion} sends a version on every request.
 */
public interface ContainerAppService extends RestService {

    /**
     * Sends {@code OpenStack-API-Version: container <version>} on the current client's requests (a method needing a
     * newer version still sends its own).
     *
     * @param version e.g. {@code 1.40} or {@code latest}, or {@code null} to send none again
     */
    void useApiVersion(String version);

    /** @return the API versions ({@code GET /}) */
    Map<String, Object> versions();

    /** @return the v1 document with its links */
    Map<String, Object> version();

    /** @param filters e.g. {@code name}, {@code image}, {@code status}, {@code host}, {@code all_projects}, {@code limit}, {@code marker} */
    Map<String, Object> listContainers(Map<String, String> filters);

    /**
     * Creates a container.
     *
     * @param container {@code image} and e.g. {@code name}, {@code command} (a list), {@code cpu}, {@code memory},
     *                  {@code environment}, {@code nets}, {@code mounts}, {@code security_groups}, {@code restart_policy}
     * @param run       whether to start it once created
     */
    Map<String, Object> createContainer(Map<String, ?> container, boolean run);

    /** @return the container (by UUID or name), or {@code null} when it does not exist */
    Map<String, Object> getContainer(String containerIdent);

    /** @param fields {@code cpu}, {@code memory}, {@code name}, {@code auto_heal} */
    Map<String, Object> updateContainer(String containerIdent, Map<String, ?> fields);

    /** @param force delete a running container (admin, 1.7) @param stop stop it first (1.12) */
    ActionResponse deleteContainer(String containerIdent, boolean force, boolean stop);

    ActionResponse startContainer(String containerIdent);

    /** @param timeout seconds to wait before killing it, or {@code null} */
    ActionResponse stopContainer(String containerIdent, Integer timeout);

    /** @param timeout seconds to wait before killing it, or {@code null} */
    ActionResponse rebootContainer(String containerIdent, Integer timeout);

    ActionResponse pauseContainer(String containerIdent);

    ActionResponse unpauseContainer(String containerIdent);

    /** @param signal e.g. {@code SIGKILL}, or {@code null} for the default */
    ActionResponse killContainer(String containerIdent, String signal);

    /** @param options {@code image}, {@code image_driver}, or {@code null} */
    ActionResponse rebuildContainer(String containerIdent, Map<String, ?> options);

    /** Renames the container with the API before 1.14 (newer servers rename with {@link #updateContainer}). */
    Map<String, Object> renameContainer(String containerIdent, String name);

    /** @param fields {@code cpu} and/or {@code memory} (1.19) */
    Map<String, Object> resizeContainer(String containerIdent, Map<String, ?> fields);

    /** Resizes the container's TTY. */
    ActionResponse resizeContainerTty(String containerIdent, int height, int width);

    /**
     * Runs a command in the container.
     *
     * @return {@code output} and {@code exit_code} when {@code run}, else {@code exec_id} and {@code url} for an
     *         interactive session
     */
    Map<String, Object> executeContainer(String containerIdent, String command, boolean run, boolean interactive);

    /** Resizes the TTY of an exec session. @return {@code exec_id} and {@code url} */
    Map<String, Object> resizeExec(String containerIdent, String execId, int height, int width);

    /** @param params e.g. {@code stdout}, {@code stderr}, {@code timestamps}, {@code tail}, {@code since}, or {@code null} */
    String containerLogs(String containerIdent, Map<String, String> params);

    /** @param psArgs e.g. {@code aux}, or {@code null} @return {@code Titles} and {@code Processes} */
    Map<String, Object> containerTop(String containerIdent, String psArgs);

    Map<String, Object> containerStats(String containerIdent);

    /** @return the websocket URL to attach to an interactive container */
    String attachContainer(String containerIdent);

    /** @return {@code data} (a Base64 tar, 1.25) and {@code stat} of {@code path} */
    Map<String, Object> getArchive(String containerIdent, String path);

    /** Extracts a Base64 tar into {@code path} (1.25). */
    ActionResponse putArchive(String containerIdent, String path, String base64Tar);

    /** @param tag or {@code null} @return {@code uuid} of the new image */
    Map<String, Object> commitContainer(String containerIdent, String repository, String tag);

    /** @param options {@code network} or {@code port}, optional {@code fixed_ip} (1.8) */
    ActionResponse attachNetwork(String containerIdent, Map<String, ?> options);

    /** @param options {@code network} or {@code port} (1.6) */
    ActionResponse detachNetwork(String containerIdent, Map<String, ?> options);

    /** @return the container's networks: {@code net_id}, {@code port_id}, {@code fixed_ips} (1.18) */
    List<Map<String, Object>> listContainerNetworks(String containerIdent);

    /** Adds a security group with the API before 1.15. */
    Map<String, Object> addSecurityGroup(String containerIdent, String securityGroup);

    /** Removes a security group with the API before 1.15. */
    Map<String, Object> removeSecurityGroup(String containerIdent, String securityGroup);

    /** @return the actions run on the container */
    List<Map<String, Object>> listContainerActions(String containerIdent);

    /** @return the action with its {@code events}, or {@code null} when it does not exist */
    Map<String, Object> getContainerAction(String containerIdent, String requestId);

    Map<String, Object> listImages(Map<String, String> filters);

    /** @return the image, or {@code null} when it does not exist */
    Map<String, Object> getImage(String imageId);

    /** @param image {@code repo}, {@code host}, optional {@code tag}, {@code image_driver} */
    Map<String, Object> pullImage(Map<String, ?> image);

    ActionResponse deleteImage(String imageId);

    /** @param params e.g. {@code image_driver}, {@code exact_match}, or {@code null} @return the matching images */
    List<Map<String, Object>> searchImages(String image, Map<String, String> params);

    /** @return the compute hosts (1.4) */
    Map<String, Object> listHosts(Map<String, String> filters);

    /** @return the host (by UUID or name), or {@code null} when it does not exist (1.4) */
    Map<String, Object> getHost(String hostIdent);

    /** @return the Zun services */
    List<Map<String, Object>> listServices();

    ActionResponse deleteService(String host, String binary);

    Map<String, Object> enableService(String host, String binary);

    /** @param reason or {@code null} */
    Map<String, Object> disableService(String host, String binary, String reason);

    Map<String, Object> forceDownService(String host, String binary, boolean forcedDown);

    /** @return the capsules (1.32) */
    Map<String, Object> listCapsules(Map<String, String> filters);

    /** @return the capsule, or {@code null} when it does not exist (1.32) */
    Map<String, Object> getCapsule(String capsuleIdent);

    /** @param template {@code kind}, {@code metadata}, {@code spec} (1.32) */
    Map<String, Object> createCapsule(Map<String, ?> template);

    ActionResponse deleteCapsule(String capsuleIdent);

    /** @param usages include {@code in_use} with each limit @return the project's limits (1.26) */
    Map<String, Object> getQuotas(String projectId, boolean usages);

    Map<String, Object> getDefaultQuotas(String projectId);

    /** @param limits any of {@code containers}, {@code memory}, {@code cpu}, {@code disk} (-1 for unlimited) */
    Map<String, Object> updateQuotas(String projectId, Map<String, ?> limits);

    /** Reverts the project's quotas to the defaults. */
    ActionResponse deleteQuotas(String projectId);

    Map<String, Object> getQuotaClass(String quotaClassName);

    Map<String, Object> updateQuotaClass(String quotaClassName, Map<String, ?> limits);

    /** @return the availability zones (admin) */
    List<Map<String, Object>> listAvailabilityZones();

    /** @param network {@code name}, optional {@code neutron_net_id} (admin) */
    Map<String, Object> createNetwork(Map<String, ?> network);

    /** Deletes a Zun network (admin, 1.27). */
    ActionResponse deleteNetwork(String networkId);

    /** @return the image registries (1.30) */
    Map<String, Object> listRegistries(Map<String, String> filters);

    /** @return the registry, or {@code null} when it does not exist */
    Map<String, Object> getRegistry(String registryIdent);

    /** @param registry {@code domain}, optional {@code name}, {@code username}, {@code password} */
    Map<String, Object> createRegistry(Map<String, ?> registry);

    Map<String, Object> updateRegistry(String registryIdent, Map<String, ?> fields);

    ActionResponse deleteRegistry(String registryIdent);
}
