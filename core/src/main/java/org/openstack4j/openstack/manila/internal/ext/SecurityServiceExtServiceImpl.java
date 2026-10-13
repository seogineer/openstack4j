package org.openstack4j.openstack.manila.internal.ext;

import static org.openstack4j.openstack.manila.internal.ManilaMicroVersions.V;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.SecurityServiceExtService;
import org.openstack4j.model.manila.SecurityService;
import org.openstack4j.model.manila.ext.options.SecurityServiceOptions;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ManilaSecurityService;

public class SecurityServiceExtServiceImpl extends BaseManilaExtService implements SecurityServiceExtService {

    /** 2.44 is the first microversion whose security service responses include {@code ou}. */
    private static final MicroVersion OU = V(44);

    @Override
    public SecurityService create(SecurityServiceOptions options) {
        return create(OU, ManilaSecurityService.class, "/security-services", "security_service", options);
    }

    @Override
    public SecurityService update(String securityServiceId, SecurityServiceOptions options) {
        return update(OU, ManilaSecurityService.class, "/security-services/" + id(securityServiceId), "security_service", options);
    }

    @Override
    public SecurityService get(String securityServiceId) {
        return show(OU, ManilaSecurityService.class, "/security-services/" + id(securityServiceId));
    }

    @Override
    public List<? extends SecurityService> listDetails(Map<String, String> filters) {
        return listOf(OU, ManilaSecurityService.SecurityServices.class, "/security-services/detail", filters);
    }
}
