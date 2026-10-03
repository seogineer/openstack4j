package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.DefaultSecurityGroupRuleService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.DefaultSecurityGroupRule;
import org.openstack4j.model.network.options.DefaultSecurityGroupRuleOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronDefaultSecurityGroupRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronDefaultSecurityGroupRule.Rules;

public class DefaultSecurityGroupRuleServiceImpl extends BaseNeutronExtService implements DefaultSecurityGroupRuleService {

    private static final String PATH = "/default-security-group-rules";
    private static final String ROOT = "default_security_group_rule";

    @Override public List<? extends DefaultSecurityGroupRule> list() { return listOf(Rules.class, PATH, null); }
    @Override public List<? extends DefaultSecurityGroupRule> list(Map<String, String> filters) { return listOf(Rules.class, PATH, filters); }
    @Override public DefaultSecurityGroupRule get(String id) { return show(NeutronDefaultSecurityGroupRule.class, PATH + "/" + id(id)); }
    @Override public DefaultSecurityGroupRule create(DefaultSecurityGroupRuleOptions options) { return create(NeutronDefaultSecurityGroupRule.class, PATH, ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
