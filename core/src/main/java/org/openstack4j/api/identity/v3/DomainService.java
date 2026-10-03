package org.openstack4j.api.identity.v3;

import java.util.Map;
import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Domain;

/**
 * Identity V3 Domain Service
 */
public interface DomainService extends RestService {

    /**
     * Creates a new domain
     *
     * @param domain the Domain to create
     * @return the new domain
     */
    Domain create(Domain domain);

    /**
     * Creates a new domain
     *
     * @param name the name of the new domain
     * @param description the description of the new domain
     * @param enabled the enabled status of the new domain
     * @return the new domain
     */
    Domain create(String name, String description, boolean enabled);

    /**
     * Updates an existing domain
     *
     * @param domain the domain set to update
     * @return the updated domain
     */
    Domain update(Domain domain);

    /**
     * Get detailed information on a domain by id
     *
     * @param domainId the domain identifier
     * @return the domain
     */
    Domain get(String domainId);

    /**
     * Get detailed information on a domain by name
     *
     * @param domainName the domain name
     * @return the domain
     */
    List<? extends Domain> getByName(String domainName);

    /**
     * Deletes a domain by id
     *
     * @param domainId the domain id
     * @return the ActionResponse
     */
    ActionResponse delete(String domainId);

    /**
     * lists all domains the current token has access to
     *
     * @return list of domains
     */
    List<? extends Domain> list();


    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): the domain-specific configuration, group → option → value ({@code GET /domains/{id}/config}).
     *
     * @param domainId the domainId
     * @return the result
     */
    Map<String, Map<String, Object>> config(String domainId);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): one group of the domain configuration, option → value.
     *
     * @param domainId the domainId
     * @param group the group
     * @return the result
     */
    Map<String, Object> configGroup(String domainId, String group);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): one option of the domain configuration.
     *
     * @param domainId the domainId
     * @param group the group
     * @param option the option
     * @return the result
     */
    Object configOption(String domainId, String group, String option);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): creates the domain configuration ({@code PUT}) and returns it.
     *
     * @param domainId the domainId
     * @param String the String
     * @param String the String
     * @param config the config
     * @return the result
     */
    Map<String, Map<String, Object>> createConfig(String domainId, Map<String, Map<String, Object>> config);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): merges into the domain configuration ({@code PATCH}) and returns the result.
     *
     * @param domainId the domainId
     * @param String the String
     * @param String the String
     * @param config the config
     * @return the result
     */
    Map<String, Map<String, Object>> updateConfig(String domainId, Map<String, Map<String, Object>> config);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): merges into one group and returns the group.
     *
     * @param domainId the domainId
     * @param group the group
     * @param String the String
     * @param options the options
     * @return the result
     */
    Map<String, Object> updateConfigGroup(String domainId, String group, Map<String, Object> options);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): sets one option and returns its new value.
     *
     * @param domainId the domainId
     * @param group the group
     * @param option the option
     * @param value the value
     * @return the result
     */
    Object updateConfigOption(String domainId, String group, String option, Object value);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): deletes the whole domain configuration.
     *
     * @param domainId the domainId
     * @return the action response
     */
    ActionResponse deleteConfig(String domainId);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): deletes one group of the domain configuration.
     *
     * @param domainId the domainId
     * @param group the group
     * @return the action response
     */
    ActionResponse deleteConfigGroup(String domainId, String group);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): deletes one option of the domain configuration.
     *
     * @param domainId the domainId
     * @param group the group
     * @param option the option
     * @return the action response
     */
    ActionResponse deleteConfigOption(String domainId, String group, String option);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): the default configuration a domain configuration overrides ({@code GET /domains/config/default}).
     *
     * @return the result
     */
    Map<String, Map<String, Object>> defaultConfig();

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): one group of the default configuration.
     *
     * @param group the group
     * @return the result
     */
    Map<String, Object> defaultConfigGroup(String group);

    /**
     * Domain-specific configuration (admin only; requires domain-specific drivers): one option of the default configuration.
     *
     * @param group the group
     * @param option the option
     * @return the result
     */
    Object defaultConfigOption(String group, String option);
}
