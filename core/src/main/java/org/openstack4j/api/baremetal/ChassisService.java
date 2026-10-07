package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Chassis;
import org.openstack4j.model.baremetal.Node;
import org.openstack4j.model.baremetal.options.ChassisCreate;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal chassis ({@code /v1/chassis}). */
public interface ChassisService extends RestService {

    /** @return the chassis (summary fields; use {@code listDetail()} for all fields) */
    List<? extends Chassis> list();

    /** @param filters query parameters such as {@code limit}, {@code marker}, {@code fields} */
    List<? extends Chassis> list(Map<String, String> filters);

    /** @return the chassis with all fields */
    List<? extends Chassis> listDetail();

    /** @return the nodes in a chassis with all fields; a missing chassis raises */
    List<? extends Node> listNodes(String chassisId);

    /** @return the chassis, or {@code null} when it does not exist */
    Chassis get(String chassisId);

    Chassis create(ChassisCreate create);

    /** Updates with JSON Patch operations, e.g. {@code BaremetalPatch.replace("/description", "rack 3")}. */
    Chassis update(String chassisId, List<BaremetalPatch> patches);

    ActionResponse delete(String chassisId);
}
