package org.openstack4j.openstack.containerapp.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import org.openstack4j.api.containerapp.ContainerAppService;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.OSClientSession;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class ContainerAppServiceImpl extends BaseOpenStackService implements ContainerAppService {

    private static final String VERSION_HEADER = "OpenStack-API-Version";

    /** API versions chosen with {@link #useApiVersion} per session (client). */
    private static final Map<Object, String> VERSIONS = Collections.synchronizedMap(new WeakHashMap<>());

    /** Container create fields and the version that introduced them. */
    private static final Map<String, Integer> CREATE_FIELDS = Map.ofEntries(Map.entry("auto_remove", 3), Map.entry("runtime", 5),
            Map.entry("hostname", 9), Map.entry("mounts", 11), Map.entry("privileged", 21), Map.entry("healthcheck", 22),
            Map.entry("exposed_ports", 24), Map.entry("registry", 31), Map.entry("tty", 36), Map.entry("host", 39),
            Map.entry("entrypoint", 40));

    public ContainerAppServiceImpl() {
        // catalogs register http://host:9517/v1 or http://host/container/v1; paths carry /v1 themselves
        super(ServiceType.CONTAINER_APP, url -> url.replaceAll("/+$", "").replaceAll("/v1(/.*)?$", ""));
    }

    @Override
    public void useApiVersion(String version) {
        if (version == null) {
            VERSIONS.remove(OSClientSession.getCurrent());
            return;
        }
        if (!version.equals("latest") && !version.matches("1\\.\\d+"))
            throw new IllegalArgumentException("Not a container API version: '" + version + "'");
        VERSIONS.put(OSClientSession.getCurrent(), version);
    }

    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        String version = VERSIONS.get(OSClientSession.getCurrent());
        return version == null ? invocation : invocation.header(VERSION_HEADER, "container " + version);
    }

    /** Sends at least {@code 1.<floor>}: the session's version when it is newer, else the floor. */
    private static <R> Invocation<R> at(Invocation<R> invocation, int floor) {
        String chosen = VERSIONS.get(OSClientSession.getCurrent());
        boolean newer = chosen != null && (chosen.equals("latest") || minor(chosen) >= floor);
        return invocation.header(VERSION_HEADER, "container " + (newer ? chosen : "1." + floor));
    }

    /** Sends exactly {@code 1.<version>}, for routes that newer versions removed. */
    private static <R> Invocation<R> exactly(Invocation<R> invocation, int version) {
        return invocation.header(VERSION_HEADER, "container 1." + version);
    }

    private static int minor(String version) {
        return Integer.parseInt(version.substring(version.indexOf('.') + 1));
    }

    private static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }

    private static String id(String value) {
        Objects.requireNonNull(value, "id");
        if (value.isBlank() || value.indexOf('/') >= 0 || value.indexOf('?') >= 0 || value.indexOf('#') >= 0)
            throw new IllegalArgumentException("Not a valid identifier: '" + value + "'");
        return value;
    }

    private static String container(String containerIdent) {
        return "/v1/containers/" + id(containerIdent);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> strict(Invocation<Map> invocation) {
        Map<String, Object> body = invocation.execute(propagate404());
        return body == null ? new HashMap<>() : body;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> body, String key) {
        Object list = body == null ? null : body.get(key);
        return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
    }

    private static <R> Invocation<R> params(Invocation<R> invocation, Map<String, String> params) {
        return params == null ? invocation : invocation.params(params);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> show(Invocation<Map> invocation) {
        return invocation.execute();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> body, String key) {
        Object inner = body == null ? null : body.get(key);
        return inner instanceof Map ? (Map<String, Object>) inner : body;
    }

    private ActionResponse action(String containerIdent, String action) {
        return postWithResponse(container(containerIdent) + "/" + action).execute();
    }

    @Override
    public Map<String, Object> versions() {
        return strict(get(Map.class, "/"));
    }

    @Override
    public Map<String, Object> version() {
        return strict(get(Map.class, "/v1"));
    }

    @Override
    public Map<String, Object> listContainers(Map<String, String> filters) {
        return strict(params(get(Map.class, "/v1/containers"), filters));
    }

    @Override
    public Map<String, Object> createContainer(Map<String, ?> container, boolean run) {
        Objects.requireNonNull(container, "container");
        int floor = container.get("command") instanceof List ? 20 : 1;
        for (Map.Entry<String, Integer> field : CREATE_FIELDS.entrySet())
            if (container.containsKey(field.getKey()))
                floor = Math.max(floor, field.getValue());
        Invocation<Map> invocation = post(Map.class, "/v1/containers").param("run", run).entity(JsonBody.of(container));
        if (floor > 1)
            at(invocation, floor);
        return strict(invocation);
    }

    @Override
    public Map<String, Object> getContainer(String containerIdent) {
        return show(get(Map.class, container(containerIdent)));
    }

    @Override
    public Map<String, Object> updateContainer(String containerIdent, Map<String, ?> fields) {
        return strict(patch(Map.class, container(containerIdent)).entity(JsonBody.of(Objects.requireNonNull(fields, "fields"))));
    }

    @Override
    public ActionResponse deleteContainer(String containerIdent, boolean force, boolean stop) {
        Invocation<ActionResponse> invocation = deleteWithResponse(container(containerIdent));
        if (force)
            at(invocation.param("force", true), 7);
        if (stop)
            at(invocation.param("stop", true), 12);
        return invocation.execute();
    }

    @Override
    public ActionResponse startContainer(String containerIdent) {
        return action(containerIdent, "start");
    }

    @Override
    public ActionResponse stopContainer(String containerIdent, Integer timeout) {
        Invocation<ActionResponse> invocation = postWithResponse(container(containerIdent) + "/stop");
        if (timeout != null)
            invocation.param("timeout", timeout);
        return invocation.execute();
    }

    @Override
    public ActionResponse rebootContainer(String containerIdent, Integer timeout) {
        Invocation<ActionResponse> invocation = postWithResponse(container(containerIdent) + "/reboot");
        if (timeout != null)
            invocation.param("timeout", timeout);
        return invocation.execute();
    }

    @Override
    public ActionResponse pauseContainer(String containerIdent) {
        return action(containerIdent, "pause");
    }

    @Override
    public ActionResponse unpauseContainer(String containerIdent) {
        return action(containerIdent, "unpause");
    }

    @Override
    public ActionResponse killContainer(String containerIdent, String signal) {
        Invocation<ActionResponse> invocation = postWithResponse(container(containerIdent) + "/kill");
        if (signal != null)
            invocation.param("signal", signal);
        return invocation.execute();
    }

    @Override
    public ActionResponse rebuildContainer(String containerIdent, Map<String, ?> options) {
        Invocation<ActionResponse> invocation = postWithResponse(container(containerIdent) + "/rebuild");
        if (options != null)
            options.forEach((k, v) -> invocation.param(k, v));
        return invocation.execute();
    }

    @Override
    public Map<String, Object> renameContainer(String containerIdent, String name) {
        return strict(exactly(post(Map.class, container(containerIdent) + "/rename").param("name", Objects.requireNonNull(name, "name")), 13));
    }

    @Override
    public Map<String, Object> resizeContainer(String containerIdent, Map<String, ?> fields) {
        return strict(at(post(Map.class, container(containerIdent) + "/resize_container"), 19).entity(JsonBody.of(Objects.requireNonNull(fields, "fields"))));
    }

    @Override
    public ActionResponse resizeContainerTty(String containerIdent, int height, int width) {
        return postWithResponse(container(containerIdent) + "/resize").param("h", height).param("w", width).execute();
    }

    @Override
    public Map<String, Object> executeContainer(String containerIdent, String command, boolean run, boolean interactive) {
        return strict(post(Map.class, container(containerIdent) + "/execute").param("command", Objects.requireNonNull(command, "command"))
                .param("run", run).param("interactive", interactive));
    }

    @Override
    public Map<String, Object> resizeExec(String containerIdent, String execId, int height, int width) {
        return strict(post(Map.class, container(containerIdent) + "/execute_resize").param("exec_id", Objects.requireNonNull(execId, "execId"))
                .param("h", height).param("w", width));
    }

    @Override
    public String containerLogs(String containerIdent, Map<String, String> params) {
        Object logs = params(get(Object.class, container(containerIdent) + "/logs"), params).execute(propagate404());
        return logs == null ? "" : logs.toString();
    }

    @Override
    public Map<String, Object> containerTop(String containerIdent, String psArgs) {
        Invocation<Map> invocation = get(Map.class, container(containerIdent) + "/top");
        if (psArgs != null)
            invocation.param("ps_args", psArgs);
        return strict(invocation);
    }

    @Override
    public Map<String, Object> containerStats(String containerIdent) {
        return strict(get(Map.class, container(containerIdent) + "/stats"));
    }

    @Override
    public String attachContainer(String containerIdent) {
        Object url = get(Object.class, container(containerIdent) + "/attach").execute(propagate404());
        return url == null ? null : url.toString();
    }

    @Override
    public Map<String, Object> getArchive(String containerIdent, String path) {
        return strict(at(get(Map.class, container(containerIdent) + "/get_archive"), 25).param("path", Objects.requireNonNull(path, "path")));
    }

    @Override
    public ActionResponse putArchive(String containerIdent, String path, String base64Tar) {
        return at(postWithResponse(container(containerIdent) + "/put_archive"), 25).param("path", Objects.requireNonNull(path, "path"))
                .entity(JsonBody.of(Map.of("data", Objects.requireNonNull(base64Tar, "base64Tar")))).execute();
    }

    @Override
    public Map<String, Object> commitContainer(String containerIdent, String repository, String tag) {
        Invocation<Map> invocation = post(Map.class, container(containerIdent) + "/commit").param("repository", Objects.requireNonNull(repository, "repository"));
        if (tag != null)
            invocation.param("tag", tag);
        return strict(invocation);
    }

    @Override
    public ActionResponse attachNetwork(String containerIdent, Map<String, ?> options) {
        Invocation<ActionResponse> invocation = at(postWithResponse(container(containerIdent) + "/network_attach"), 8);
        Objects.requireNonNull(options, "options").forEach((k, v) -> invocation.param(k, v));
        return invocation.execute();
    }

    @Override
    public ActionResponse detachNetwork(String containerIdent, Map<String, ?> options) {
        Invocation<ActionResponse> invocation = at(postWithResponse(container(containerIdent) + "/network_detach"), 6);
        Objects.requireNonNull(options, "options").forEach((k, v) -> invocation.param(k, v));
        return invocation.execute();
    }

    @Override
    public List<Map<String, Object>> listContainerNetworks(String containerIdent) {
        return list(strict(at(get(Map.class, container(containerIdent) + "/network_list"), 18)), "networks");
    }

    @Override
    public Map<String, Object> addSecurityGroup(String containerIdent, String securityGroup) {
        return strict(exactly(post(Map.class, container(containerIdent) + "/add_security_group"), 14)
                .entity(JsonBody.of(Map.of("name", Objects.requireNonNull(securityGroup, "securityGroup")))));
    }

    @Override
    public Map<String, Object> removeSecurityGroup(String containerIdent, String securityGroup) {
        return strict(exactly(post(Map.class, container(containerIdent) + "/remove_security_group"), 14)
                .entity(JsonBody.of(Map.of("name", Objects.requireNonNull(securityGroup, "securityGroup")))));
    }

    @Override
    public List<Map<String, Object>> listContainerActions(String containerIdent) {
        return list(strict(get(Map.class, container(containerIdent) + "/container_actions")), "containerActions");
    }

    @Override
    public Map<String, Object> getContainerAction(String containerIdent, String requestId) {
        return show(get(Map.class, container(containerIdent) + "/container_actions/" + id(requestId)));
    }

    @Override
    public Map<String, Object> listImages(Map<String, String> filters) {
        return strict(params(get(Map.class, "/v1/images"), filters));
    }

    @Override
    public Map<String, Object> getImage(String imageId) {
        return show(get(Map.class, "/v1/images/" + id(imageId)));
    }

    @Override
    public Map<String, Object> pullImage(Map<String, ?> image) {
        return strict(post(Map.class, "/v1/images").entity(JsonBody.of(Objects.requireNonNull(image, "image"))));
    }

    @Override
    public ActionResponse deleteImage(String imageId) {
        return deleteWithResponse("/v1/images/" + id(imageId)).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> searchImages(String image, Map<String, String> params) {
        List<Map<String, Object>> found = params(get(List.class, "/v1/images/search"), params).param("image", Objects.requireNonNull(image, "image"))
                .execute(propagate404());
        return found == null ? Collections.emptyList() : found;
    }

    @Override
    public Map<String, Object> listHosts(Map<String, String> filters) {
        return strict(at(params(get(Map.class, "/v1/hosts"), filters), 4));
    }

    @Override
    public Map<String, Object> getHost(String hostIdent) {
        return show(at(get(Map.class, "/v1/hosts/" + id(hostIdent)), 4));
    }

    @Override
    public List<Map<String, Object>> listServices() {
        return list(strict(get(Map.class, "/v1/services")), "services");
    }

    private static Map<String, Object> service(String host, String binary) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("host", Objects.requireNonNull(host, "host"));
        body.put("binary", Objects.requireNonNull(binary, "binary"));
        return body;
    }

    /** Zun reads these parameters from the query string; the body repeats them. */
    private Map<String, Object> updateService(String action, Map<String, Object> fields) {
        Invocation<Map> invocation = put(Map.class, "/v1/services/" + action).entity(JsonBody.of(fields));
        fields.forEach((k, v) -> invocation.param(k, v));
        return unwrap(strict(invocation), "service");
    }

    @Override
    public ActionResponse deleteService(String host, String binary) {
        return deleteWithResponse("/v1/services").param("host", Objects.requireNonNull(host, "host"))
                .param("binary", Objects.requireNonNull(binary, "binary")).execute();
    }

    @Override
    public Map<String, Object> enableService(String host, String binary) {
        return updateService("enable", service(host, binary));
    }

    @Override
    public Map<String, Object> disableService(String host, String binary, String reason) {
        Map<String, Object> fields = service(host, binary);
        if (reason != null)
            fields.put("disabled_reason", reason);
        return updateService("disable", fields);
    }

    @Override
    public Map<String, Object> forceDownService(String host, String binary, boolean forcedDown) {
        Map<String, Object> fields = service(host, binary);
        fields.put("forced_down", forcedDown);
        return updateService("force_down", fields);
    }

    @Override
    public Map<String, Object> listCapsules(Map<String, String> filters) {
        return strict(at(params(get(Map.class, "/v1/capsules"), filters), 32));
    }

    @Override
    public Map<String, Object> getCapsule(String capsuleIdent) {
        return show(at(get(Map.class, "/v1/capsules/" + id(capsuleIdent)), 32));
    }

    @Override
    public Map<String, Object> createCapsule(Map<String, ?> template) {
        return strict(at(post(Map.class, "/v1/capsules"), 32).entity(JsonBody.of(Map.of("template", Objects.requireNonNull(template, "template")))));
    }

    @Override
    public ActionResponse deleteCapsule(String capsuleIdent) {
        return at(deleteWithResponse("/v1/capsules/" + id(capsuleIdent)), 32).execute();
    }

    @Override
    public Map<String, Object> getQuotas(String projectId, boolean usages) {
        Invocation<Map> invocation = at(get(Map.class, "/v1/quotas/" + id(projectId)), 26);
        if (usages)
            invocation.param("usages", true);
        return strict(invocation);
    }

    @Override
    public Map<String, Object> getDefaultQuotas(String projectId) {
        return strict(at(get(Map.class, "/v1/quotas/" + id(projectId) + "/defaults"), 26));
    }

    @Override
    public Map<String, Object> updateQuotas(String projectId, Map<String, ?> limits) {
        return strict(at(put(Map.class, "/v1/quotas/" + id(projectId)), 26).entity(JsonBody.of(Objects.requireNonNull(limits, "limits"))));
    }

    @Override
    public ActionResponse deleteQuotas(String projectId) {
        return at(deleteWithResponse("/v1/quotas/" + id(projectId)), 26).execute();
    }

    @Override
    public Map<String, Object> getQuotaClass(String quotaClassName) {
        return strict(at(get(Map.class, "/v1/quota_classes/" + id(quotaClassName)), 26));
    }

    @Override
    public Map<String, Object> updateQuotaClass(String quotaClassName, Map<String, ?> limits) {
        return strict(at(put(Map.class, "/v1/quota_classes/" + id(quotaClassName)), 26).entity(JsonBody.of(Objects.requireNonNull(limits, "limits"))));
    }

    @Override
    public List<Map<String, Object>> listAvailabilityZones() {
        return list(strict(get(Map.class, "/v1/availability_zones")), "availability_zones");
    }

    @Override
    public Map<String, Object> createNetwork(Map<String, ?> network) {
        return strict(post(Map.class, "/v1/networks").entity(JsonBody.of(Objects.requireNonNull(network, "network"))));
    }

    @Override
    public ActionResponse deleteNetwork(String networkId) {
        return at(deleteWithResponse("/v1/networks/" + id(networkId)), 27).execute();
    }

    @Override
    public Map<String, Object> listRegistries(Map<String, String> filters) {
        return strict(at(params(get(Map.class, "/v1/registries"), filters), 30));
    }

    @Override
    public Map<String, Object> getRegistry(String registryIdent) {
        Map<String, Object> body = show(at(get(Map.class, "/v1/registries/" + id(registryIdent)), 30));
        return body == null ? null : unwrap(body, "registry");
    }

    @Override
    public Map<String, Object> createRegistry(Map<String, ?> registry) {
        return unwrap(strict(at(post(Map.class, "/v1/registries"), 30).entity(JsonBody.of(Map.of("registry", Objects.requireNonNull(registry, "registry"))))), "registry");
    }

    @Override
    public Map<String, Object> updateRegistry(String registryIdent, Map<String, ?> fields) {
        return unwrap(strict(at(patch(Map.class, "/v1/registries/" + id(registryIdent)), 30)
                .entity(JsonBody.of(Map.of("registry", Objects.requireNonNull(fields, "fields"))))), "registry");
    }

    @Override
    public ActionResponse deleteRegistry(String registryIdent) {
        return at(deleteWithResponse("/v1/registries/" + id(registryIdent)), 30).execute();
    }
}
