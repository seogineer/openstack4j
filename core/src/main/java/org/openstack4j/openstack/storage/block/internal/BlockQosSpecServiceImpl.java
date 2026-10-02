package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.*;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;
import org.openstack4j.api.storage.BlockQosSpecService;
import org.openstack4j.model.storage.block.QosAssociation;
import org.openstack4j.model.storage.block.QosSpec;
import org.openstack4j.openstack.storage.block.domain.CinderQosAssociation.QosAssociations;
import org.openstack4j.openstack.storage.block.domain.CinderQosSpec.QosSpecs;

public class BlockQosSpecServiceImpl extends BaseBlockStorageServices implements BlockQosSpecService {

    @Override
    public List<? extends QosSpec> list() {
        return get(QosSpecs.class, uri("/qos-specs")).execute().getList();
    }

    @Override
    public QosSpec get(String qosSpecId) {
        return get(CinderQosSpec.class, uri("/qos-specs/%s", Objects.requireNonNull(qosSpecId))).execute();
    }

    @Override
    public QosSpec create(String name, Map<String, String> specs) {
        Objects.requireNonNull(name);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        if (specs != null)
            body.putAll(specs);
        return post(CinderQosSpec.class, uri("/qos-specs")).entity(JsonBody.of("qos_specs", body)).execute();
    }

    @Override
    public QosSpec update(String qosSpecId, Map<String, String> specs) {
        Objects.requireNonNull(qosSpecId);
        Objects.requireNonNull(specs);
        return put(CinderQosSpec.class, uri("/qos-specs/%s", qosSpecId)).entity(JsonBody.of("qos_specs", specs)).execute();
    }

    @Override
    public ActionResponse delete(String qosSpecId, boolean force) {
        return delete(ActionResponse.class, uri("/qos-specs/%s", Objects.requireNonNull(qosSpecId))).param("force", force).execute();
    }

    @Override
    public ActionResponse deleteKeys(String qosSpecId, List<String> keys) {
        Objects.requireNonNull(qosSpecId);
        Objects.requireNonNull(keys);
        return put(ActionResponse.class, uri("/qos-specs/%s/delete_keys", qosSpecId)).entity(JsonBody.of(Collections.singletonMap("keys", keys))).execute();
    }

    @Override
    public List<? extends QosAssociation> associations(String qosSpecId) {
        return get(QosAssociations.class, uri("/qos-specs/%s/associations", Objects.requireNonNull(qosSpecId))).execute().getList();
    }

    @Override
    public ActionResponse associate(String qosSpecId, String volumeTypeId) {
        return getWithResponse(uri("/qos-specs/%s/associate", Objects.requireNonNull(qosSpecId))).param("vol_type_id", Objects.requireNonNull(volumeTypeId)).execute();
    }

    @Override
    public ActionResponse disassociate(String qosSpecId, String volumeTypeId) {
        return getWithResponse(uri("/qos-specs/%s/disassociate", Objects.requireNonNull(qosSpecId))).param("vol_type_id", Objects.requireNonNull(volumeTypeId)).execute();
    }

    @Override
    public ActionResponse disassociateAll(String qosSpecId) {
        return getWithResponse(uri("/qos-specs/%s/disassociate_all", Objects.requireNonNull(qosSpecId))).execute();
    }
}
