package org.openstack4j.openstack.identity.v3.internal;

import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.identity.v3.domain.KeystoneDomainConfig;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.DomainService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Domain;
import org.openstack4j.openstack.identity.v3.domain.KeystoneDomain;
import org.openstack4j.openstack.identity.v3.domain.KeystoneDomain.Domains;

import static org.openstack4j.core.transport.ClientConstants.PATH_DOMAINS;

public class DomainServiceImpl extends BaseIdentityServices implements DomainService {

    @Override
    public Domain create(Domain domain) {
        Objects.requireNonNull(domain);
        return post(KeystoneDomain.class, PATH_DOMAINS).entity(domain).execute();
    }

    @Override
    public Domain create(String name, String description, boolean enabled) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        Objects.requireNonNull(enabled);
        return create(KeystoneDomain.builder().name(name).description(description).enabled(enabled).build());
    }

    @Override
    public Domain update(Domain domain) {
        Objects.requireNonNull(domain);
        return patch(KeystoneDomain.class, PATH_DOMAINS, "/", domain.getId()).entity(domain).execute();
    }

    @Override
    public Domain get(String domainId) {
        Objects.requireNonNull(domainId);
        return get(KeystoneDomain.class, PATH_DOMAINS, "/", domainId).execute();
    }

    @Override
    public List<? extends Domain> getByName(String domainName) {
        Objects.requireNonNull(domainName);
        return get(Domains.class, uri(PATH_DOMAINS)).param("name", domainName).execute().getList();
    }

    @Override
    public ActionResponse delete(String domainId) {
        Objects.requireNonNull(domainId);
        return deleteWithResponse(PATH_DOMAINS, "/", domainId).execute();
    }

    @Override
    public List<? extends Domain> list() {
        return get(Domains.class, uri(PATH_DOMAINS)).execute().getList();
    }


    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> groups(KeystoneDomainConfig result) {
        if (result == null || result.getConfig() == null)
            return Collections.emptyMap();
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        result.getConfig().forEach((k, v) -> out.put(k, v instanceof Map ? (Map<String, Object>) v : Collections.emptyMap()));
        return out;
    }

    /** A group comes back nested, {@code {"config": {"<group>": {...}}}}; a flat {@code {"config": {...}}} is accepted too. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> options(KeystoneDomainConfig result, String group) {
        if (result == null || result.getConfig() == null)
            return Collections.emptyMap();
        Map<String, Object> config = result.getConfig();
        Object nested = config.get(group);
        return config.size() == 1 && nested instanceof Map ? (Map<String, Object>) nested : config;
    }

    /** A single option comes back as {@code {"config": {"<option>": value}}}; a bare {@code {"<option>": value}} is accepted too. */
    @SuppressWarnings("unchecked")
    private static Object single(Map<String, Object> response, String option) {
        if (response == null)
            return null;
        Object config = response.get("config");
        return config instanceof Map && !option.equals("config") ? ((Map<String, Object>) config).get(option) : response.get(option);
    }

    @Override public Map<String, Map<String, Object>> config(String domainId) { return groups(get(KeystoneDomainConfig.class, uri("/domains/%s/config", domainId)).execute()); }
    @Override public Map<String, Object> configGroup(String domainId, String group) { return options(get(KeystoneDomainConfig.class, uri("/domains/%s/config/%s", domainId, group)).execute(), group); }

    @Override
    @SuppressWarnings("unchecked")
    public Object configOption(String domainId, String group, String option) {
        return single(get(HashMap.class, uri("/domains/%s/config/%s/%s", domainId, group, option)).execute(), option);
    }

    @Override
    public Map<String, Map<String, Object>> createConfig(String domainId, Map<String, Map<String, Object>> config) {
        return groups(put(KeystoneDomainConfig.class, uri("/domains/%s/config", domainId)).entity(JsonBody.of("config", config)).execute());
    }

    @Override
    public Map<String, Map<String, Object>> updateConfig(String domainId, Map<String, Map<String, Object>> config) {
        return groups(patch(KeystoneDomainConfig.class, uri("/domains/%s/config", domainId)).entity(JsonBody.of("config", config)).execute());
    }

    @Override
    public Map<String, Object> updateConfigGroup(String domainId, String group, Map<String, Object> options) {
        return options(patch(KeystoneDomainConfig.class, uri("/domains/%s/config/%s", domainId, group)).entity(JsonBody.of("config", options)).execute(), group);
    }

    @Override
    public Object updateConfigOption(String domainId, String group, String option, Object value) {
        Map<String, Object> body = new HashMap<>();
        body.put(option, value);
        return options(patch(KeystoneDomainConfig.class, uri("/domains/%s/config/%s/%s", domainId, group, option)).entity(JsonBody.of("config", body)).execute(), group).get(option);
    }

    @Override public ActionResponse deleteConfig(String domainId) { return deleteWithResponse(uri("/domains/%s/config", domainId)).execute(); }
    @Override public ActionResponse deleteConfigGroup(String domainId, String group) { return deleteWithResponse(uri("/domains/%s/config/%s", domainId, group)).execute(); }
    @Override public ActionResponse deleteConfigOption(String domainId, String group, String option) { return deleteWithResponse(uri("/domains/%s/config/%s/%s", domainId, group, option)).execute(); }
    @Override public Map<String, Map<String, Object>> defaultConfig() { return groups(get(KeystoneDomainConfig.class, uri("/domains/config/default")).execute()); }
    @Override public Map<String, Object> defaultConfigGroup(String group) { return options(get(KeystoneDomainConfig.class, uri("/domains/config/%s/default", group)).execute(), group); }

    @Override
    @SuppressWarnings("unchecked")
    public Object defaultConfigOption(String group, String option) {
        return single(get(HashMap.class, uri("/domains/config/%s/%s/default", group, option)).execute(), option);
    }
}
