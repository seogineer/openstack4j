package org.openstack4j.openstack.identity.v3.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstack4j.api.identity.v3.RegisteredLimitService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.RegisteredLimit;
import org.openstack4j.model.identity.v3.options.RegisteredLimitCreate;
import org.openstack4j.model.identity.v3.options.RegisteredLimitListOptions;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRegisteredLimit;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRegisteredLimit.RegisteredLimits;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class RegisteredLimitServiceImpl extends BaseIdentityServices implements RegisteredLimitService {

    @Override
    public List<? extends RegisteredLimit> list() {
        return get(RegisteredLimits.class, "/registered_limits").execute().getList();
    }

    @Override
    public List<? extends RegisteredLimit> list(RegisteredLimitListOptions options) {
        return get(RegisteredLimits.class, "/registered_limits").params(Objects.requireNonNull(options).getOptions()).execute().getList();
    }

    @Override
    public RegisteredLimit get(String id) {
        return get(KeystoneRegisteredLimit.class, "/registered_limits/", Objects.requireNonNull(id)).execute();
    }

    @Override
    public List<? extends RegisteredLimit> create(List<RegisteredLimitCreate> limits) {
        List<Map<String, Object>> entries = limits.stream().map(RegisteredLimitCreate::toMap).collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("registered_limits", entries);
        return post(RegisteredLimits.class, "/registered_limits").entity(JsonBody.of(body)).execute().getList();
    }

    @Override
    public RegisteredLimit update(String id, Integer defaultLimit, String description) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (defaultLimit != null) fields.put("default_limit", defaultLimit);
        if (description != null) fields.put("description", description);
        return patch(KeystoneRegisteredLimit.class, "/registered_limits/", Objects.requireNonNull(id))
                .entity(JsonBody.of("registered_limit", fields)).execute();
    }

    @Override
    public ActionResponse delete(String id) {
        return deleteWithResponse("/registered_limits/", Objects.requireNonNull(id)).execute();
    }
}
