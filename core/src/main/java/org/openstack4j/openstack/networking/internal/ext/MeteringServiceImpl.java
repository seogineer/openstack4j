package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.MeteringService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.MeteringLabel;
import org.openstack4j.model.network.ext.MeteringLabelRule;
import org.openstack4j.model.network.options.MeteringLabelOptions;
import org.openstack4j.model.network.options.MeteringLabelRuleOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronMeteringLabel;
import org.openstack4j.openstack.networking.domain.ext.NeutronMeteringLabel.MeteringLabels;
import org.openstack4j.openstack.networking.domain.ext.NeutronMeteringLabelRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronMeteringLabelRule.MeteringLabelRules;

public class MeteringServiceImpl extends BaseNeutronExtService implements MeteringService {

    private static final String LABELS = "/metering/metering-labels";
    private static final String RULES = "/metering/metering-label-rules";

    @Override public List<? extends MeteringLabel> listLabels() { return listOf(MeteringLabels.class, LABELS, null); }
    @Override public List<? extends MeteringLabel> listLabels(Map<String, String> filters) { return listOf(MeteringLabels.class, LABELS, filters); }
    @Override public MeteringLabel getLabel(String id) { return show(NeutronMeteringLabel.class, LABELS + "/" + id(id)); }
    @Override public MeteringLabel createLabel(MeteringLabelOptions options) { return create(NeutronMeteringLabel.class, LABELS, "metering_label", options); }
    @Override public ActionResponse deleteLabel(String id) { return remove(LABELS + "/" + id(id)); }
    @Override public List<? extends MeteringLabelRule> listRules() { return listOf(MeteringLabelRules.class, RULES, null); }
    @Override public List<? extends MeteringLabelRule> listRules(Map<String, String> filters) { return listOf(MeteringLabelRules.class, RULES, filters); }
    @Override public MeteringLabelRule getRule(String id) { return show(NeutronMeteringLabelRule.class, RULES + "/" + id(id)); }
    @Override public MeteringLabelRule createRule(MeteringLabelRuleOptions options) { return create(NeutronMeteringLabelRule.class, RULES, "metering_label_rule", options); }
    @Override public ActionResponse deleteRule(String id) { return remove(RULES + "/" + id(id)); }
}
