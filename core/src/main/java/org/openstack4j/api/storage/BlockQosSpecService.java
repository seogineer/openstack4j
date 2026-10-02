package org.openstack4j.api.storage;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.QosAssociation;
import org.openstack4j.model.storage.block.QosSpec;

/** QoS specifications ({@code /qos-specs}). */
public interface BlockQosSpecService extends RestService {

    List<? extends QosSpec> list();

    QosSpec get(String qosSpecId);

    /** @param specs key/values; {@code consumer} selects front-end, back-end or both */
    QosSpec create(String name, Map<String, String> specs);

    /** Adds or updates keys. */
    QosSpec update(String qosSpecId, Map<String, String> specs);

    /** @param force delete even when associated with volume types */
    ActionResponse delete(String qosSpecId, boolean force);

    ActionResponse deleteKeys(String qosSpecId, List<String> keys);

    List<? extends QosAssociation> associations(String qosSpecId);

    ActionResponse associate(String qosSpecId, String volumeTypeId);

    ActionResponse disassociate(String qosSpecId, String volumeTypeId);

    ActionResponse disassociateAll(String qosSpecId);
}
