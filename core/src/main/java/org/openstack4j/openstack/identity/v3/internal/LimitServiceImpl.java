package org.openstack4j.openstack.identity.v3.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstack4j.api.identity.v3.LimitService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Limit;
import org.openstack4j.model.identity.v3.LimitModel;
import org.openstack4j.openstack.identity.v3.domain.KeystoneLimitModel;
import org.openstack4j.model.identity.v3.options.LimitCreate;
import org.openstack4j.model.identity.v3.options.LimitListOptions;
import org.openstack4j.openstack.identity.v3.domain.KeystoneLimit;
import org.openstack4j.openstack.identity.v3.domain.KeystoneLimit.Limits;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class LimitServiceImpl extends BaseIdentityServices implements LimitService {

    @Override
    public List<? extends Limit> list() {
        return get(Limits.class, "/limits").execute().getList();
    }

    @Override
    public List<? extends Limit> list(LimitListOptions options) {
        return get(Limits.class, "/limits").params(Objects.requireNonNull(options).getOptions()).execute().getList();
    }

    @Override
    public Limit get(String id) {
        return get(KeystoneLimit.class, "/limits/", Objects.requireNonNull(id)).execute();
    }

    @Override
    public List<? extends Limit> create(List<LimitCreate> limits) {
        List<Map<String, Object>> entries = limits.stream().map(LimitCreate::toMap).collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("limits", entries);
        return post(Limits.class, "/limits").entity(JsonBody.of(body)).execute().getList();
    }

    @Override
    public Limit update(String id, Integer resourceLimit, String description) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (resourceLimit != null) fields.put("resource_limit", resourceLimit);
        if (description != null) fields.put("description", description);
        return patch(KeystoneLimit.class, "/limits/", Objects.requireNonNull(id))
                .entity(JsonBody.of("limit", fields)).execute();
    }

    @Override
    public ActionResponse delete(String id) {
        return deleteWithResponse("/limits/", Objects.requireNonNull(id)).execute();
    }

    @Override
    public LimitModel model() {
        return get(KeystoneLimitModel.class, "/limits/model").execute();
    }
}
