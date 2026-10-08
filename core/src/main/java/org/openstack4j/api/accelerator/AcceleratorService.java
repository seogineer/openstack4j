package org.openstack4j.api.accelerator;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/**
 * Accelerators (Cyborg v2): accelerator requests (ARQs), device profiles, devices, deployables and attributes.
 * Results are {@code Map}s.
 */
public interface AcceleratorService extends RestService {

    /** @param filters e.g. {@code instance}, {@code bind_state} @return the ARQs */
    List<Map<String, Object>> listArqs(Map<String, String> filters);

    /** @return the ARQ, or {@code null} when it does not exist */
    Map<String, Object> getArq(String arqId);

    /** Creates the ARQs of a device profile; @return the created ARQs */
    List<Map<String, Object>> createArqs(String deviceProfileName);

    /**
     * Binds or unbinds ARQs with JSON Patch per ARQ, e.g. {@code {"<arq uuid>": [{"op": "add", "path": "/instance_uuid", "value": …}, …]}}
     * ({@code /project_id} needs accelerator 2.1, sent for you).
     */
    ActionResponse patchArqs(Map<String, List<Map<String, Object>>> patches);

    /** Deletes ARQs by uuid. */
    ActionResponse deleteArqs(List<String> arqIds);

    /** Deletes the ARQs of an instance. */
    ActionResponse deleteArqsOfInstance(String instanceId);

    /** @return the device profiles */
    List<Map<String, Object>> listDeviceProfiles(Map<String, String> filters);

    /** @param nameOrId a name needs accelerator 2.2 (sent for you) @return the profile, or {@code null} */
    Map<String, Object> getDeviceProfile(String nameOrId);

    /** @param profile {@code name}, {@code groups} (e.g. {@code [{"resources:CUSTOM_ACCELERATOR_FPGA": "1"}]}), optional {@code description} */
    Map<String, Object> createDeviceProfile(Map<String, ?> profile);

    ActionResponse deleteDeviceProfile(String deviceProfileId);

    /** Deletes device profiles by name. */
    ActionResponse deleteDeviceProfilesByName(List<String> names);

    /** @param filters e.g. {@code type}, {@code vendor}, {@code hostname} @return the devices (status needs 2.3, sent for you) */
    List<Map<String, Object>> listDevices(Map<String, String> filters);

    /** @return the device, or {@code null} when it does not exist */
    Map<String, Object> getDevice(String deviceId);

    ActionResponse enableDevice(String deviceId);

    ActionResponse disableDevice(String deviceId);

    List<Map<String, Object>> listDeployables(Map<String, String> filters);

    /** @return the deployable, or {@code null} when it does not exist */
    Map<String, Object> getDeployable(String deployableId);

    /** Programs an FPGA deployable with a bitstream image; @return the deployable */
    Map<String, Object> programDeployable(String deployableId, String imageId);

    List<Map<String, Object>> listAttributes(Map<String, String> filters);

    /** @return the attribute, or {@code null} when it does not exist */
    Map<String, Object> getAttribute(String attributeId);

    /** @return the created attributes */
    List<Map<String, Object>> createAttribute(String deployableId, String key, String value);

    ActionResponse deleteAttribute(String attributeId);
}
