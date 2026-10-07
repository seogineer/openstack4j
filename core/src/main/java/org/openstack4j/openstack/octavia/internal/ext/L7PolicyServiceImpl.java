package org.openstack4j.openstack.octavia.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.octavia.ext.L7PolicyService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.L7Policy;
import org.openstack4j.model.octavia.ext.L7Rule;
import org.openstack4j.model.octavia.options.L7PolicyOptions;
import org.openstack4j.model.octavia.options.L7RuleOptions;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaL7Rule;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaL7Rule.L7Rules;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaL7Policy;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaL7Policy.L7Policies;

public class L7PolicyServiceImpl extends BaseOctaviaExtService implements L7PolicyService {

    private static final String PATH = "/lbaas/l7policies";
    private static final String ROOT = "l7policy";

    @Override public List<? extends L7Policy> list() { return listOf(L7Policies.class, PATH, null); }
    @Override public List<? extends L7Policy> list(Map<String, String> filters) { return listOf(L7Policies.class, PATH, filters); }
    @Override public L7Policy get(String id) { return show(OctaviaL7Policy.class, PATH + "/" + id(id)); }
    @Override public L7Policy create(L7PolicyOptions options) { return create(OctaviaL7Policy.class, PATH, ROOT, options); }
    @Override public L7Policy update(String id, L7PolicyOptions options) { return update(OctaviaL7Policy.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }

    private static String rules(String policyId) {
        return PATH + "/" + id(policyId) + "/rules";
    }

    @Override public List<? extends L7Rule> listRules(String policyId) { return listOf(L7Rules.class, rules(policyId), null); }
    @Override public List<? extends L7Rule> listRules(String policyId, Map<String, String> filters) { return listOf(L7Rules.class, rules(policyId), filters); }
    @Override public L7Rule getRule(String policyId, String ruleId) { return show(OctaviaL7Rule.class, rules(policyId) + "/" + id(ruleId)); }
    @Override public L7Rule createRule(String policyId, L7RuleOptions options) { return create(OctaviaL7Rule.class, rules(policyId), "rule", options); }
    @Override public L7Rule updateRule(String policyId, String ruleId, L7RuleOptions options) { return update(OctaviaL7Rule.class, rules(policyId) + "/" + id(ruleId), "rule", options); }
    @Override public ActionResponse deleteRule(String policyId, String ruleId) { return remove(rules(policyId) + "/" + id(ruleId)); }
}
