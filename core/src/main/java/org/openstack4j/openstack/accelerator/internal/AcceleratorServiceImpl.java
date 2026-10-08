package org.openstack4j.openstack.accelerator.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;
import org.openstack4j.api.accelerator.AcceleratorService;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class AcceleratorServiceImpl extends BaseOpenStackService implements AcceleratorService {

    private static final String API_VERSION = "OpenStack-API-Version";
    private static final java.util.regex.Pattern UUID_LIKE = java.util.regex.Pattern.compile("[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}");

    public AcceleratorServiceImpl() {
        // catalogs register http://host/accelerator or .../accelerator/v2; paths carry /v2 themselves
        super(ServiceType.ACCELERATOR, url -> url.replaceAll("/+$", "").replaceAll("/v2(/.*)?$", ""));
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOf(Invocation<Map> invocation, String key, Map<String, String> filters) {
        Map<String, Object> body = invocation.params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        Object list = body == null ? null : body.get(key);
        return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> show(Invocation<Map> invocation) {
        return invocation.execute();
    }

    @Override
    public List<Map<String, Object>> listArqs(Map<String, String> filters) {
        return listOf(get(Map.class, "/v2/accelerator_requests"), "arqs", filters);
    }

    @Override
    public Map<String, Object> getArq(String arqId) {
        return show(get(Map.class, "/v2/accelerator_requests/" + id(arqId)));
    }

    @Override
    public List<Map<String, Object>> createArqs(String deviceProfileName) {
        return listOf(post(Map.class, "/v2/accelerator_requests").entity(JsonBody.of(Map.of("device_profile_name", Objects.requireNonNull(deviceProfileName, "deviceProfileName")))),
                "arqs", null);
    }

    @Override
    public ActionResponse patchArqs(Map<String, List<Map<String, Object>>> patches) {
        boolean projectId = Objects.requireNonNull(patches, "patches").values().stream().flatMap(List::stream).anyMatch(p -> "/project_id".equals(p.get("path")));
        Invocation<ActionResponse> invocation = patchWithResponse("/v2/accelerator_requests");
        if (projectId)
            invocation.header(API_VERSION, "accelerator 2.1");
        return invocation.entity(JsonBody.of(patches)).execute();
    }

    @Override
    public ActionResponse deleteArqs(List<String> arqIds) {
        return deleteWithResponse("/v2/accelerator_requests").param("arqs", String.join(",", Objects.requireNonNull(arqIds, "arqIds"))).execute();
    }

    @Override
    public ActionResponse deleteArqsOfInstance(String instanceId) {
        return deleteWithResponse("/v2/accelerator_requests").param("instance", id(instanceId)).execute();
    }

    @Override
    public List<Map<String, Object>> listDeviceProfiles(Map<String, String> filters) {
        return listOf(get(Map.class, "/v2/device_profiles"), "device_profiles", filters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDeviceProfile(String nameOrId) {
        Invocation<Map> invocation = get(Map.class, "/v2/device_profiles/" + id(nameOrId));
        if (!UUID_LIKE.matcher(nameOrId).matches())
            invocation.header(API_VERSION, "accelerator 2.2");
        Map<String, Object> body = show(invocation);
        // the only Cyborg single-object response that is wrapped
        Object profile = body == null ? null : body.get("device_profile");
        return profile instanceof Map ? (Map<String, Object>) profile : body;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> createDeviceProfile(Map<String, ?> profile) {
        // the API takes a list with one profile
        return post(Map.class, "/v2/device_profiles").entity(new ListBody(List.of(Objects.requireNonNull(profile, "profile")))).execute(propagate404());
    }

    @Override
    public ActionResponse deleteDeviceProfile(String deviceProfileId) {
        return deleteWithResponse("/v2/device_profiles/" + id(deviceProfileId)).execute();
    }

    @Override
    public ActionResponse deleteDeviceProfilesByName(List<String> names) {
        return deleteWithResponse("/v2/device_profiles").param("value", String.join(",", Objects.requireNonNull(names, "names"))).execute();
    }

    @Override
    public List<Map<String, Object>> listDevices(Map<String, String> filters) {
        return listOf(get(Map.class, "/v2/devices"), "devices", filters);
    }

    @Override
    public Map<String, Object> getDevice(String deviceId) {
        return show(get(Map.class, "/v2/devices/" + id(deviceId)));
    }

    @Override
    public ActionResponse enableDevice(String deviceId) {
        return postWithResponse("/v2/devices/" + id(deviceId) + "/enable").execute();
    }

    @Override
    public ActionResponse disableDevice(String deviceId) {
        return postWithResponse("/v2/devices/" + id(deviceId) + "/disable").execute();
    }

    @Override
    public List<Map<String, Object>> listDeployables(Map<String, String> filters) {
        return listOf(get(Map.class, "/v2/deployables"), "deployables", filters);
    }

    @Override
    public Map<String, Object> getDeployable(String deployableId) {
        return show(get(Map.class, "/v2/deployables/" + id(deployableId)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> programDeployable(String deployableId, String imageId) {
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("path", "/program");
        op.put("value", List.of(Map.of("image_uuid", id(imageId))));
        op.put("op", "replace");
        return patch(Map.class, "/v2/deployables/" + id(deployableId) + "/program").entity(new ListBody(List.of(op))).execute(propagate404());
    }

    @Override
    public List<Map<String, Object>> listAttributes(Map<String, String> filters) {
        return listOf(get(Map.class, "/v2/attributes"), "attributes", filters);
    }

    @Override
    public Map<String, Object> getAttribute(String attributeId) {
        return show(get(Map.class, "/v2/attributes/" + id(attributeId)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> createAttribute(String deployableId, String key, String value) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deployable_id", id(deployableId));
        body.put("key", Objects.requireNonNull(key, "key"));
        body.put("value", Objects.requireNonNull(value, "value"));
        return post(Map.class, "/v2/attributes").entity(JsonBody.of(body)).execute(propagate404());
    }

    @Override
    public ActionResponse deleteAttribute(String attributeId) {
        return deleteWithResponse("/v2/attributes/" + id(attributeId)).execute();
    }

    /** A request body that is a JSON list. */
    static final class ListBody implements ModelEntity {
        private static final long serialVersionUID = 1L;
        private final List<?> items;

        ListBody(List<?> items) {
            this.items = items;
        }

        @JsonValue
        public List<?> items() {
            return items;
        }
    }
}
