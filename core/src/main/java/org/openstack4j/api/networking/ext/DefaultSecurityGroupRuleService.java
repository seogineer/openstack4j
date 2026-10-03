package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.DefaultSecurityGroupRule;
import org.openstack4j.model.network.options.DefaultSecurityGroupRuleOptions;

/**
 * Default security group rules ({@code /v2.0/default-security-group-rules}, security-groups-default-rules): templates copied into new security groups.
 */
public interface DefaultSecurityGroupRuleService extends RestService {

    /**
     * Lists the default security group rules.
     *
     * @return the result
     */
    List<? extends DefaultSecurityGroupRule> list();

    /**
     * Lists the default security group rules.
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends DefaultSecurityGroupRule> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    DefaultSecurityGroupRule get(String id);

    /**
     * Creates a default security group rule (admin).
     *
     * @param options the options
     * @return the result
     */
    DefaultSecurityGroupRule create(DefaultSecurityGroupRuleOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
