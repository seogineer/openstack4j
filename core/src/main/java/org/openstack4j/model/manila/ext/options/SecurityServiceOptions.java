package org.openstack4j.model.manila.ext.options;

import org.openstack4j.model.manila.SecurityService;

/** The body of {@code POST /v2/security-services} and {@code PUT /v2/security-services/{id}} (microversion 2.44 or later). */
public final class SecurityServiceOptions extends ManilaAttributes<SecurityServiceOptions> {

    private SecurityServiceOptions() {
    }

    public static SecurityServiceOptions create() {
        return new SecurityServiceOptions();
    }

    @Override
    protected SecurityServiceOptions self() {
        return this;
    }

    public SecurityServiceOptions type(SecurityService.Type type) {
        return put("type", type == null ? null : type.value());
    }

    public SecurityServiceOptions name(String name) {
        return put("name", name);
    }

    public SecurityServiceOptions description(String description) {
        return put("description", description);
    }

    public SecurityServiceOptions dnsIp(String dnsIp) {
        return put("dns_ip", dnsIp);
    }

    public SecurityServiceOptions user(String user) {
        return put("user", user);
    }

    public SecurityServiceOptions password(String password) {
        return put("password", password);
    }

    public SecurityServiceOptions domain(String domain) {
        return put("domain", domain);
    }

    public SecurityServiceOptions server(String server) {
        return put("server", server);
    }

    /** The organizational unit of an Active Directory security service. */
    public SecurityServiceOptions ou(String ou) {
        return put("ou", ou);
    }
}
