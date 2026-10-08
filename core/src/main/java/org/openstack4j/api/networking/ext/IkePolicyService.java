package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.IkePolicy;
import org.openstack4j.model.network.options.IkePolicyOptions;

/** IKE policies ({@code /v2.0/vpn/ikepolicies}). */
public interface IkePolicyService extends RestService {

    /** @return the IKE policies */
    List<? extends IkePolicy> list();

    /** @param filters query parameters such as {@code name}, {@code auth_algorithm}, {@code encryption_algorithm}, {@code ike_version} */
    List<? extends IkePolicy> list(Map<String, String> filters);

    /** @return the IKE policy, or {@code null} when it does not exist */
    IkePolicy get(String id);

    IkePolicy create(IkePolicyOptions options);

    /** Changes only the fields set in {@code options}. */
    IkePolicy update(String id, IkePolicyOptions options);

    ActionResponse delete(String id);
}
