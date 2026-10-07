package org.openstack4j.openstack.baremetal.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.api.baremetal.NodeService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Node;
import org.openstack4j.model.baremetal.NodeStates;
import org.openstack4j.model.baremetal.options.NodeCreate;
import org.openstack4j.model.baremetal.options.NodeProvision;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.baremetal.domain.IronicNode.Nodes;
import org.openstack4j.openstack.baremetal.domain.IronicNode;
import org.openstack4j.openstack.baremetal.domain.IronicNodeStates;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class NodeServiceImpl extends BaseBaremetalServices implements NodeService {

    @Override
    public List<? extends Node> list() {
        return list(null);
    }

    @Override
    public List<? extends Node> list(Map<String, String> filters) {
        return listOf(Nodes.class, "/nodes", filters);
    }

    @Override
    public List<? extends Node> listDetail() {
        return listDetail(null);
    }

    @Override
    public List<? extends Node> listDetail(Map<String, String> filters) {
        return listOf(Nodes.class, "/nodes/detail", filters);
    }

    @Override
    public Node get(String nodeIdent) {
        return show(IronicNode.class, "/nodes/" + id(nodeIdent));
    }

    @Override
    public Node create(NodeCreate node) {
        return create(IronicNode.class, "/nodes", node);
    }

    @Override
    public Node update(String nodeIdent, List<BaremetalPatch> patches) {
        return patchWith(IronicNode.class, "/nodes/" + id(nodeIdent), patches);
    }

    @Override
    public ActionResponse delete(String nodeIdent) {
        return remove("/nodes/" + id(nodeIdent));
    }

    private static String node(String nodeIdent) {
        return "/nodes/" + id(nodeIdent);
    }

    @Override
    public NodeStates getStates(String nodeIdent) {
        return showStrict(IronicNodeStates.class, node(nodeIdent) + "/states");
    }

    @Override
    public ActionResponse setPowerState(String nodeIdent, String target) {
        return action(node(nodeIdent) + "/states/power", Map.of("target", Objects.requireNonNull(target, "target")));
    }

    @Override
    public ActionResponse setPowerState(String nodeIdent, String target, int timeoutSeconds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("target", Objects.requireNonNull(target, "target"));
        body.put("timeout", timeoutSeconds);
        return action(node(nodeIdent) + "/states/power", body);
    }

    @Override
    public ActionResponse setProvisionState(String nodeIdent, NodeProvision provision) {
        return action(node(nodeIdent) + "/states/provision", Objects.requireNonNull(provision, "provision").toMap());
    }

    @Override
    public ActionResponse setRaidConfig(String nodeIdent, Map<String, ?> targetRaidConfig) {
        return action(node(nodeIdent) + "/states/raid", Objects.requireNonNull(targetRaidConfig, "targetRaidConfig"));
    }

    @Override
    public ActionResponse setBootMode(String nodeIdent, String target) {
        return action(node(nodeIdent) + "/states/boot_mode", Map.of("target", Objects.requireNonNull(target, "target")));
    }

    @Override
    public ActionResponse setSecureBoot(String nodeIdent, boolean enabled) {
        return action(node(nodeIdent) + "/states/secure_boot", Map.of("target", enabled));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getConsole(String nodeIdent) {
        return showStrict(Map.class, node(nodeIdent) + "/states/console");
    }

    @Override
    public ActionResponse setConsoleEnabled(String nodeIdent, boolean enabled) {
        return action(node(nodeIdent) + "/states/console", Map.of("enabled", enabled));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getBootDevice(String nodeIdent) {
        return showStrict(Map.class, node(nodeIdent) + "/management/boot_device");
    }

    @Override
    public ActionResponse setBootDevice(String nodeIdent, String bootDevice, boolean persistent) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("boot_device", Objects.requireNonNull(bootDevice, "bootDevice"));
        body.put("persistent", persistent);
        return action(node(nodeIdent) + "/management/boot_device", body);
    }

    @Override
    public List<String> getSupportedBootDevices(String nodeIdent) {
        return showStrict(SupportedBootDevices.class, node(nodeIdent) + "/management/boot_device/supported").devices;
    }

    @Override
    public ActionResponse injectNmi(String nodeIdent) {
        return action(node(nodeIdent) + "/management/inject_nmi", Map.of());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Map<String, Object>> validate(String nodeIdent) {
        return showStrict(Map.class, node(nodeIdent) + "/validate");
    }

    @Override
    public ActionResponse setMaintenance(String nodeIdent, String reason) {
        return action(node(nodeIdent) + "/maintenance", reason == null ? Map.of() : Map.of("reason", reason));
    }

    @Override
    public ActionResponse unsetMaintenance(String nodeIdent) {
        return remove(node(nodeIdent) + "/maintenance");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class SupportedBootDevices {
        @JsonProperty("supported_boot_devices")
        private List<String> devices = Collections.emptyList();
    }

    @Override
    public List<String> listTraits(String nodeIdent) {
        return showStrict(Traits.class, node(nodeIdent) + "/traits").traits;
    }

    @Override
    public ActionResponse setTraits(String nodeIdent, List<String> traits) {
        return action(node(nodeIdent) + "/traits", Map.of("traits", Objects.requireNonNull(traits, "traits")));
    }

    @Override
    public ActionResponse addTrait(String nodeIdent, String trait) {
        return putWithResponse(node(nodeIdent) + "/traits/" + id(trait)).execute();
    }

    @Override
    public ActionResponse removeTrait(String nodeIdent, String trait) {
        return remove(node(nodeIdent) + "/traits/" + id(trait));
    }

    @Override
    public ActionResponse removeAllTraits(String nodeIdent) {
        return remove(node(nodeIdent) + "/traits");
    }

    @Override
    public List<String> listVifs(String nodeIdent) {
        List<Map<String, Object>> vifs = showStrict(Vifs.class, node(nodeIdent) + "/vifs").vifs;
        return vifs.stream().map(v -> (String) v.get("id")).collect(Collectors.toList());
    }

    @Override
    public ActionResponse attachVif(String nodeIdent, String vifId) {
        return attachVif(nodeIdent, vifId, Map.of());
    }

    @Override
    public ActionResponse attachVif(String nodeIdent, String vifId, Map<String, ?> options) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id(vifId));
        if (options != null)
            body.putAll(options);
        return postWithResponse(node(nodeIdent) + "/vifs").entity(JsonBody.of(body)).execute();
    }

    @Override
    public ActionResponse detachVif(String nodeIdent, String vifId) {
        return remove(node(nodeIdent) + "/vifs/" + id(vifId));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Traits {
        @JsonProperty("traits")
        private List<String> traits = Collections.emptyList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Vifs {
        @JsonProperty("vifs")
        private List<Map<String, Object>> vifs = Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> listBiosSettings(String nodeIdent) {
        return showStrict(Lists.class, node(nodeIdent) + "/bios").list("bios");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getBiosSetting(String nodeIdent, String settingName) {
        Map<String, Object> body = show(Map.class, node(nodeIdent) + "/bios/" + id(settingName));
        return body == null ? null : (Map<String, Object>) body.get(settingName);
    }

    @Override
    public List<Map<String, Object>> listFirmwareComponents(String nodeIdent) {
        return showStrict(Lists.class, node(nodeIdent) + "/firmware").list("firmware");
    }

    @Override
    public List<Map<String, Object>> listHistory(String nodeIdent) {
        return showStrict(Lists.class, node(nodeIdent) + "/history").list("history");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getHistoryEvent(String nodeIdent, String eventUuid) {
        return show(Map.class, node(nodeIdent) + "/history/" + id(eventUuid));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getInventory(String nodeIdent) {
        return showStrict(Map.class, node(nodeIdent) + "/inventory");
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> listChildren(String nodeIdent) {
        Map<String, Object> body = showStrict(Map.class, node(nodeIdent) + "/children");
        Object children = body == null ? null : body.get("children");
        return children instanceof List ? (List<String>) children : Collections.emptyList();
    }

    @Override
    public ActionResponse attachVirtualMedia(String nodeIdent, String deviceType, String imageUrl, Map<String, ?> options) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("device_type", Objects.requireNonNull(deviceType, "deviceType"));
        body.put("image_url", Objects.requireNonNull(imageUrl, "imageUrl"));
        if (options != null)
            body.putAll(options);
        return postWithResponse(node(nodeIdent) + "/vmedia").entity(JsonBody.of(body)).execute();
    }

    @Override
    public ActionResponse detachVirtualMedia(String nodeIdent) {
        return remove(node(nodeIdent) + "/vmedia");
    }

    @Override
    public ActionResponse detachVirtualMedia(String nodeIdent, String deviceType) {
        return deleteWithResponse(node(nodeIdent) + "/vmedia").param("device_types", Objects.requireNonNull(deviceType, "deviceType")).execute();
    }

    @Override
    public List<Map<String, Object>> listIndicatorComponents(String nodeIdent) {
        return showStrict(Lists.class, node(nodeIdent) + "/management/indicators").list("components");
    }

    @Override
    public List<Map<String, Object>> listIndicators(String nodeIdent, String component) {
        return showStrict(Lists.class, node(nodeIdent) + "/management/indicators/" + id(component)).list("indicators");
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getIndicatorState(String nodeIdent, String indicator) {
        Map<String, Object> body = showStrict(Map.class, node(nodeIdent) + "/management/indicators/" + id(indicator));
        return body == null ? null : (String) body.get("state");
    }

    @Override
    public ActionResponse setIndicatorState(String nodeIdent, String indicator, String state) {
        return action(node(nodeIdent) + "/management/indicators/" + id(indicator), Map.of("state", Objects.requireNonNull(state, "state")));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> listVendorPassthruMethods(String nodeIdent) {
        return showStrict(Map.class, node(nodeIdent) + "/vendor_passthru/methods");
    }

    @Override
    public ActionResponse vendorPassthru(String nodeIdent, String method, Map<String, ?> args) {
        return postWithResponse(node(nodeIdent) + "/vendor_passthru").param("method", id(method))
                .entity(JsonBody.of(args == null ? Map.of() : args)).execute();
    }

    /** A response with one list of objects under a key. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Lists extends HashMap<String, Object> {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list(String key) {
            Object list = get(key);
            return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
        }
    }
}
