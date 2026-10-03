package org.openstack4j.api.identity.v3;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.FederationProtocol;
import org.openstack4j.model.identity.v3.IdentityProvider;

/**
 * OS-FEDERATION identity providers and their protocols ({@code /v3/OS-FEDERATION/identity_providers}).
 */
public interface IdentityProviderService extends RestService {

    /**
     * @return the result
     */
    List<? extends IdentityProvider> list();

    /**
     * @param id the id
     * @return the result
     */
    IdentityProvider get(String id);

    /**
     * Creates an identity provider (PUT); attribute keys: description, enabled, domain_id, remote_ids, authorization_ttl.
     *
     * @param id the id
     * @param Map<String the map< string
     * @param attributes the attributes
     * @return the result
     */
    IdentityProvider create(String id, Map<String, Object> attributes);

    /**
     * Updates an identity provider (PATCH) with the given attributes.
     *
     * @param id the id
     * @param Map<String the map< string
     * @param attributes the attributes
     * @return the result
     */
    IdentityProvider update(String id, Map<String, Object> attributes);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);

    /**
     * Lists the protocols of an identity provider.
     *
     * @param idpId the idp id
     * @return the result
     */
    List<? extends FederationProtocol> protocols(String idpId);

    /**
     * @param idpId the idp id
     * @param protocolId the protocol id
     * @return the result
     */
    FederationProtocol getProtocol(String idpId, String protocolId);

    /**
     * Creates a protocol bound to a mapping (PUT).
     *
     * @param idpId the idp id
     * @param protocolId the protocol id
     * @param mappingId the mapping id
     * @return the result
     */
    FederationProtocol createProtocol(String idpId, String protocolId, String mappingId);

    /**
     * Rebinds a protocol to another mapping (PATCH).
     *
     * @param idpId the idp id
     * @param protocolId the protocol id
     * @param mappingId the mapping id
     * @return the result
     */
    FederationProtocol updateProtocol(String idpId, String protocolId, String mappingId);

    /**
     * @param idpId the idp id
     * @param protocolId the protocol id
     * @return the action response
     */
    ActionResponse deleteProtocol(String idpId, String protocolId);
}
