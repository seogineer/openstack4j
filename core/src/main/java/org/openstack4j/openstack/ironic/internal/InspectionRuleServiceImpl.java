package org.openstack4j.openstack.ironic.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.ironic.InspectionRuleService;
import org.openstack4j.model.ironic.BaremetalPatch;
import org.openstack4j.model.ironic.InspectionRule;
import org.openstack4j.model.ironic.options.InspectionRuleCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.ironic.domain.IronicInspectionRule;
import org.openstack4j.openstack.ironic.domain.IronicInspectionRule.IronicInspectionRuleList;

public class InspectionRuleServiceImpl extends BaseBaremetalServices implements InspectionRuleService {

    @Override
    public List<? extends InspectionRule> list() {
        return list(null);
    }

    @Override
    public List<? extends InspectionRule> list(Map<String, String> filters) {
        return listOf(IronicInspectionRuleList.class, "/inspection_rules", withDetail(filters));
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
