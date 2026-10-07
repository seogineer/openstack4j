package org.openstack4j.openstack.baremetal.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.InspectionRuleService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.InspectionRule;
import org.openstack4j.model.baremetal.options.InspectionRuleCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.baremetal.domain.IronicInspectionRule;
import org.openstack4j.openstack.baremetal.domain.IronicInspectionRule.IronicInspectionRuleList;

public class InspectionRuleServiceImpl extends BaseBaremetalServices implements InspectionRuleService {

    @Override
    public List<? extends InspectionRule> list() {
        return list(null);
    }

    @Override
    public List<? extends InspectionRule> list(Map<String, String> filters) {
        Map<String, String> query = filters == null ? new HashMap<>() : new HashMap<>(filters);
        query.putIfAbsent("detail", "true");
        return listOf(IronicInspectionRuleList.class, "/inspection_rules", query);
    }

    @Override
    public InspectionRule get(String ident) {
        return show(IronicInspectionRule.class, "/inspection_rules/" + id(ident));
    }

    @Override
    public InspectionRule create(InspectionRuleCreate create) {
        return create(IronicInspectionRule.class, "/inspection_rules", create);
    }

    @Override
    public InspectionRule update(String ident, List<BaremetalPatch> patches) {
        return patchWith(IronicInspectionRule.class, "/inspection_rules/" + id(ident), patches);
    }

    @Override
    public ActionResponse delete(String ident) {
        return remove("/inspection_rules/" + id(ident));
    }
}
