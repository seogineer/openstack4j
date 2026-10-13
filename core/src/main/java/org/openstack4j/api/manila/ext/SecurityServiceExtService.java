package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.manila.SecurityService;
import org.openstack4j.model.manila.ext.options.SecurityServiceOptions;

/**
 * Security services at the session's microversion, at least 2.44 (the first version whose responses carry {@code ou};
 * the server stores {@code ou} at any version). The legacy {@code securityServices()} stays at 2.6.
 * {@code default_ad_site} needs a session microversion of 2.76 or later and can be sent with
 * {@link SecurityServiceOptions#attribute(String, Object)}.
 */
public interface SecurityServiceExtService extends RestService {

    SecurityService create(SecurityServiceOptions options);

    /** Only name and description can change while the security service is used by a share server (403 otherwise). */
    SecurityService update(String securityServiceId, SecurityServiceOptions options);

    /** @return the security service, or {@code null} when it does not exist */
    SecurityService get(String securityServiceId);

    /**
     * @param filters query parameters such as name, id, type, user, server, dns_ip, domain, share_network_id, all_tenants (may be null)
     * @return details of the security services
     */
    List<? extends SecurityService> listDetails(Map<String, String> filters);
}
