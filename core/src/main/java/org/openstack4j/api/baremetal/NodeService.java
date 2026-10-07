package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Node;
import org.openstack4j.model.baremetal.NodeStates;
import org.openstack4j.model.baremetal.options.NodeCreate;
import org.openstack4j.model.baremetal.options.NodeProvision;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal nodes ({@code /v1/nodes}). A node is addressed by its UUID or (microversion 1.5+) its name. */
public interface NodeService extends RestService {

    /** @return the nodes (summary fields; use {@code listDetail()} for all fields) */
    List<? extends Node> list();

    /**
     * @param filters query parameters such as {@code provision_state}, {@code maintenance}, {@code driver},
     *                {@code resource_class}, {@code limit}, {@code marker}, {@code fields}
     * @return the matching nodes (summary fields)
     */
    List<? extends Node> list(Map<String, String> filters);

    /** @return the nodes with all fields */
    List<? extends Node> listDetail();

    /** @return the matching nodes with all fields */
    List<? extends Node> listDetail(Map<String, String> filters);

    /** @return the node, or {@code null} when it does not exist */
    Node get(String nodeIdent);

    /** @return the created node (provision state {@code enroll} from microversion 1.11) */
    Node create(NodeCreate node);

    /**
     * Updates a node with JSON Patch operations, e.g. {@code BaremetalPatch.replace("/description", "rack 3")}.
     *
     * @return the updated node
     */
    Node update(String nodeIdent, List<BaremetalPatch> patches);

    ActionResponse delete(String nodeIdent);

    /** @return the node's states; a missing node raises a {@code ResponseException} */
    NodeStates getStates(String nodeIdent);

    /** @param target {@code power on}, {@code power off}, {@code rebooting}, {@code soft power off} or {@code soft rebooting} */
    ActionResponse setPowerState(String nodeIdent, String target);

    /** @param timeoutSeconds how long to wait for the power change (microversion 1.27) */
    ActionResponse setPowerState(String nodeIdent, String target, int timeoutSeconds);

    ActionResponse setProvisionState(String nodeIdent, NodeProvision provision);

    /** @param targetRaidConfig the target RAID configuration, e.g. {@code {"logical_disks": [...]}} (microversion 1.12) */
    ActionResponse setRaidConfig(String nodeIdent, Map<String, ?> targetRaidConfig);

    /** @param target {@code bios} or {@code uefi} (microversion 1.76) */
    ActionResponse setBootMode(String nodeIdent, String target);

    /** Turns UEFI secure boot on or off (microversion 1.76). */
    ActionResponse setSecureBoot(String nodeIdent, boolean enabled);

    /** @return {@code console_enabled} and {@code console_info}; a missing node raises */
    Map<String, Object> getConsole(String nodeIdent);

    ActionResponse setConsoleEnabled(String nodeIdent, boolean enabled);

    /** @return {@code boot_device} and {@code persistent}; a missing node raises */
    Map<String, Object> getBootDevice(String nodeIdent);

    /** @param bootDevice e.g. {@code pxe}, {@code disk}, {@code cdrom}, {@code bios}, {@code safe} */
    ActionResponse setBootDevice(String nodeIdent, String bootDevice, boolean persistent);

    /** @return the boot devices the node supports; a missing node raises */
    List<String> getSupportedBootDevices(String nodeIdent);

    /** Sends a non-maskable interrupt (microversion 1.29). */
    ActionResponse injectNmi(String nodeIdent);

    /** @return per interface ({@code boot}, {@code deploy}, {@code power} ...) its {@code result} and {@code reason}; a missing node raises */
    Map<String, Map<String, Object>> validate(String nodeIdent);

    /** @param reason why the node is in maintenance; {@code null} sends no reason */
    ActionResponse setMaintenance(String nodeIdent, String reason);

    ActionResponse unsetMaintenance(String nodeIdent);

    /** @return the node's traits (microversion 1.37); a missing node raises */
    List<String> listTraits(String nodeIdent);

    /** Replaces all traits of the node (microversion 1.37). */
    ActionResponse setTraits(String nodeIdent, List<String> traits);

    ActionResponse addTrait(String nodeIdent, String trait);

    ActionResponse removeTrait(String nodeIdent, String trait);

    ActionResponse removeAllTraits(String nodeIdent);

    /** @return the IDs of the VIFs attached to the node (microversion 1.28); a missing node raises */
    List<String> listVifs(String nodeIdent);

    /** Attaches a VIF (a Neutron port ID) to the node (microversion 1.28). */
    ActionResponse attachVif(String nodeIdent, String vifId);

    /** @param options more fields of the attach body, e.g. {@code port_uuid} or {@code portgroup_uuid} (microversion 1.67) */
    ActionResponse attachVif(String nodeIdent, String vifId, Map<String, ?> options);

    ActionResponse detachVif(String nodeIdent, String vifId);

    /** @return the node's BIOS settings ({@code name}, {@code value}); a missing node raises */
    List<Map<String, Object>> listBiosSettings(String nodeIdent);

    /**
     * @param filters query parameters, e.g. {@code detail=true} for {@code attribute_type}, {@code allowable_values} ...
     *                (microversion 1.74) or {@code fields}
     * @return the node's BIOS settings; a missing node raises
     */
    List<Map<String, Object>> listBiosSettings(String nodeIdent, Map<String, String> filters);

    /** @return the BIOS setting, or {@code null} when the node or the setting does not exist */
    Map<String, Object> getBiosSetting(String nodeIdent, String settingName);

    /** @return the node's firmware components (microversion 1.86); a missing node raises */
    List<Map<String, Object>> listFirmwareComponents(String nodeIdent);

    /** @return the node's history events (microversion 1.78); a missing node raises */
    List<Map<String, Object>> listHistory(String nodeIdent);

    /** @return the history event, or {@code null} when it does not exist (microversion 1.78) */
    Map<String, Object> getHistoryEvent(String nodeIdent, String eventUuid);

    /** @return {@code inventory} and {@code plugin_data} of the last inspection (microversion 1.81); a missing node or inventory raises */
    Map<String, Object> getInventory(String nodeIdent);

    /** @return the node's child node UUIDs (microversion 1.83); a missing node raises */
    List<String> listChildren(String nodeIdent);

    /**
     * Attaches virtual media (microversion 1.89).
     *
     * @param deviceType e.g. {@code CDROM}
     * @param options    more fields, e.g. {@code image_download_source}; may be {@code null}
     */
    ActionResponse attachVirtualMedia(String nodeIdent, String deviceType, String imageUrl, Map<String, ?> options);

    /** Detaches all virtual media (microversion 1.89). */
    ActionResponse detachVirtualMedia(String nodeIdent);

    /** Detaches the virtual media of one device type (microversion 1.89). */
    ActionResponse detachVirtualMedia(String nodeIdent, String deviceType);

    /** @return the components that have indicators (microversion 1.63); a missing node raises */
    List<Map<String, Object>> listIndicatorComponents(String nodeIdent);

    /** @return the indicators of a component, e.g. {@code system} (microversion 1.63); a missing node raises */
    List<Map<String, Object>> listIndicators(String nodeIdent, String component);

    /** @param indicator {@code <indicator>@<component>}, e.g. {@code led@system}; @return its state, e.g. {@code ON} */
    String getIndicatorState(String nodeIdent, String indicator);

    /** @param state {@code ON}, {@code OFF} or {@code BLINKING} */
    ActionResponse setIndicatorState(String nodeIdent, String indicator, String state);

    /** @return the vendor passthru methods of the node's driver; a missing node raises */
    Map<String, Object> listVendorPassthruMethods(String nodeIdent);

    /** Calls a vendor passthru method with {@code POST}. */
    ActionResponse vendorPassthru(String nodeIdent, String method, Map<String, ?> args);
}
