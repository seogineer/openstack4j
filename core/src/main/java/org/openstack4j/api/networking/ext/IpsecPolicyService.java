package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.IpsecPolicy;
import org.openstack4j.model.network.options.IpsecPolicyOptions;

/** IPsec policies ({@code /v2.0/vpn/ipsecpolicies}). */
public interface IpsecPolicyService extends RestService {

    /** @return the IPsec policies */
    List<? extends IpsecPolicy> list();

    /** @param filters query parameters such as {@code name}, {@code transform_protocol}, {@code encapsulation_mode} */
    List<? extends IpsecPolicy> list(Map<String, String> filters);

    /** @return the IPsec policy, or {@code null} when it does not exist */
    IpsecPolicy get(String id);

    IpsecPolicy create(IpsecPolicyOptions options);

    /** Changes only the fields set in {@code options}. */
    IpsecPolicy update(String id, IpsecPolicyOptions options);

    ActionResponse delete(String id);
}
