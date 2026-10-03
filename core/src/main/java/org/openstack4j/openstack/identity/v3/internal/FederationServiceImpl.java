package org.openstack4j.openstack.identity.v3.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.Apis;
import org.openstack4j.api.identity.v3.FederationService;
import org.openstack4j.api.identity.v3.IdentityProviderService;
import org.openstack4j.api.identity.v3.MappingService;
import org.openstack4j.api.identity.v3.ServiceProviderService;
import org.openstack4j.core.transport.ClientConstants;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.model.identity.v3.Domain;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.openstack.identity.v3.domain.KeystoneDomain.Domains;
import org.openstack4j.openstack.identity.v3.domain.KeystoneProject.Projects;
import org.openstack4j.openstack.identity.v3.domain.KeystoneToken;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class FederationServiceImpl extends BaseIdentityServices implements FederationService {

    @Override public IdentityProviderService identityProviders() { return Apis.get(IdentityProviderService.class); }
    @Override public MappingService mappings() { return Apis.get(MappingService.class); }
    @Override public ServiceProviderService serviceProviders() { return Apis.get(ServiceProviderService.class); }
    @Override public List<? extends Project> projects() { return get(Projects.class, "/OS-FEDERATION/projects").execute().getList(); }
    @Override public List<? extends Domain> domains() { return get(Domains.class, "/OS-FEDERATION/domains").execute().getList(); }

    @Override
    public String saml2Metadata() {
        return IdentityResponses.text(get(Void.class, "/OS-FEDERATION/saml2/metadata").executeWithResponse());
    }

    @Override
    public String saml2Assertion(String tokenId, String serviceProviderId) {
        return IdentityResponses.text(post(Void.class, "/auth/OS-FEDERATION/saml2").entity(assertionRequest(tokenId, serviceProviderId)).executeWithResponse());
    }

    @Override
    public String ecpAssertion(String tokenId, String serviceProviderId) {
        return IdentityResponses.text(post(Void.class, "/auth/OS-FEDERATION/saml2/ecp").entity(assertionRequest(tokenId, serviceProviderId)).executeWithResponse());
    }

    @Override
    public Token federatedToken(String idpId, String protocolId, Map<String, String> headers) {
        HttpResponse response = get(Void.class, "/OS-FEDERATION/identity_providers/", Objects.requireNonNull(idpId), "/protocols/",
                Objects.requireNonNull(protocolId), "/auth").headers(headers == null ? Map.of() : headers).executeWithResponse();
        KeystoneToken token = response.getEntity(KeystoneToken.class);
        if (token != null)
            token.setId(response.header(ClientConstants.HEADER_X_SUBJECT_TOKEN));
        return token;
    }

    /** {@code {"auth": {"identity": {"methods": ["token"], "token": {"id": ...}}, "scope": {"service_provider": {"id": ...}}}}} */
    private static JsonBody assertionRequest(String tokenId, String serviceProviderId) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("methods", List.of("token"));
        identity.put("token", Map.of("id", Objects.requireNonNull(tokenId)));
        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("identity", identity);
        auth.put("scope", Map.of("service_provider", Map.of("id", Objects.requireNonNull(serviceProviderId))));
        return JsonBody.of("auth", auth);
    }
}
