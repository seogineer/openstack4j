package org.openstack4j.openstack.identity.v3.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.identity.v3.MappingService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Mapping;
import org.openstack4j.openstack.identity.v3.domain.KeystoneMapping;
import org.openstack4j.openstack.identity.v3.domain.KeystoneMapping.Mappings;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class MappingServiceImpl extends BaseIdentityServices implements MappingService {

    private static final String MAPPINGS = "/OS-FEDERATION/mappings";

    private static String mapping(String id) {
        return MAPPINGS + "/" + Objects.requireNonNull(id);
    }

    private static JsonBody rules(List<Map<String, Object>> rules) {
        return JsonBody.of("mapping", Collections.singletonMap("rules", Objects.requireNonNull(rules)));
    }

    @Override public List<? extends Mapping> list() { return get(Mappings.class, MAPPINGS).execute().getList(); }
    @Override public Mapping get(String id) { return get(KeystoneMapping.class, mapping(id)).execute(); }
    @Override public Mapping create(String id, List<Map<String, Object>> rules) { return put(KeystoneMapping.class, mapping(id)).entity(rules(rules)).execute(); }
    @Override public Mapping update(String id, List<Map<String, Object>> rules) { return patch(KeystoneMapping.class, mapping(id)).entity(rules(rules)).execute(); }
    @Override public ActionResponse delete(String id) { return deleteWithResponse(mapping(id)).execute(); }
}
