package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.InspectionRule;
import org.openstack4j.model.baremetal.options.InspectionRuleCreate;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal inspection rules ({@code /v1/inspection_rules}) (microversion 1.96). */
public interface InspectionRuleService extends RestService {

    /** @return the inspection rules with all fields */
    List<? extends InspectionRule> list();

    /** @param filters query parameters such as {@code phase}, {@code limit}, {@code marker} ({@code detail=true} is sent unless {@code detail} or {@code fields} is given) */
    List<? extends InspectionRule> list(Map<String, String> filters);

    /** @return the inspection rule, or {@code null} when it does not exist */
    InspectionRule get(String ident);

    InspectionRule create(InspectionRuleCreate create);

    /** Updates with JSON Patch operations. */
    InspectionRule update(String ident, List<BaremetalPatch> patches);

    ActionResponse delete(String ident);
}
