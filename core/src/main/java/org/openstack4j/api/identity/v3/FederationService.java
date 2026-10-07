package org.openstack4j.api.identity.v3;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.identity.v3.Domain;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.identity.v3.Token;

/**
 * OS-FEDERATION: identity providers, mappings, service providers, SAML2 and federated tokens.
 */
public interface FederationService extends RestService {

    /**
     * Identity providers and protocols.
     *
     * @return the result
     */
    IdentityProviderService identityProviders();

    /**
     * Mappings.
     *
     * @return the result
     */
    MappingService mappings();

    /**
     * Service providers.
     *
     * @return the result
     */
    ServiceProviderService serviceProviders();

    /**
     * Projects the federated user can scope to.
     *
     * @return the result
     */
    List<? extends Project> projects();

    /**
     * Domains the federated user can scope to.
     *
     * @return the result
     */
    List<? extends Domain> domains();

    /**
     * Returns the SAML2 IdP metadata XML of this Keystone.
     *
     * @return the result
     */
    String saml2Metadata();

    /**
     * Returns a SAML2 assertion XML for the service provider, issued for the token.
     *
     * @param tokenId the token id
     * @param serviceProviderId the service provider id
     * @return the result
     */
    String saml2Assertion(String tokenId, String serviceProviderId);

    /**
     * Returns a SAML2 ECP (SOAP) envelope for the service provider, issued for the token.
     *
     * @param tokenId the token id
     * @param serviceProviderId the service provider id
     * @return the result
     */
    String ecpAssertion(String tokenId, String serviceProviderId);

    /**
     * Requests an unscoped federated token; the headers carry the remote attributes (normally set by the web server).
     *
     * @param idpId the idp id
     * @param protocolId the protocol id
     * @param headers the headers
     * @return the result
     */
    Token federatedToken(String idpId, String protocolId, Map<String, String> headers);
}
