package org.openstack4j.openstack.identity.v3.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.identity.v3.IdentityProviderService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.FederationProtocol;
import org.openstack4j.model.identity.v3.IdentityProvider;
import org.openstack4j.openstack.identity.v3.domain.KeystoneFederationProtocol;
import org.openstack4j.openstack.identity.v3.domain.KeystoneFederationProtocol.Protocols;
import org.openstack4j.openstack.identity.v3.domain.KeystoneIdentityProvider;
import org.openstack4j.openstack.identity.v3.domain.KeystoneIdentityProvider.IdentityProviders;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class IdentityProviderServiceImpl extends BaseIdentityServices implements IdentityProviderService {

    private static final String IDPS = "/OS-FEDERATION/identity_providers";

    private static String idp(String id) {
        return IDPS + "/" + Objects.requireNonNull(id);
    }

    private static String protocol(String idpId, String protocolId) {
        return idp(idpId) + "/protocols/" + Objects.requireNonNull(protocolId);
    }

    @Override public List<? extends IdentityProvider> list() { return get(IdentityProviders.class, IDPS).execute().getList(); }
    @Override public IdentityProvider get(String id) { return get(KeystoneIdentityProvider.class, idp(id)).execute(); }

    @Override
    public IdentityProvider create(String id, Map<String, Object> attributes) {
        return put(KeystoneIdentityProvider.class, idp(id)).entity(JsonBody.of("identity_provider", attributes == null ? Collections.emptyMap() : attributes)).execute();
    }

    @Override
    public IdentityProvider update(String id, Map<String, Object> attributes) {
        return patch(KeystoneIdentityProvider.class, idp(id)).entity(JsonBody.of("identity_provider", Objects.requireNonNull(attributes))).execute();
    }

    @Override public ActionResponse delete(String id) { return deleteWithResponse(idp(id)).execute(); }
    @Override public List<? extends FederationProtocol> protocols(String idpId) { return get(Protocols.class, idp(idpId) + "/protocols").execute().getList(); }
    @Override public FederationProtocol getProtocol(String idpId, String protocolId) { return get(KeystoneFederationProtocol.class, protocol(idpId, protocolId)).execute(); }

    @Override
    public FederationProtocol createProtocol(String idpId, String protocolId, String mappingId) {
        return put(KeystoneFederationProtocol.class, protocol(idpId, protocolId))
                .entity(JsonBody.of("protocol", Collections.singletonMap("mapping_id", Objects.requireNonNull(mappingId)))).execute();
    }

    @Override
    public FederationProtocol updateProtocol(String idpId, String protocolId, String mappingId) {
        return patch(KeystoneFederationProtocol.class, protocol(idpId, protocolId))
                .entity(JsonBody.of("protocol", Collections.singletonMap("mapping_id", Objects.requireNonNull(mappingId)))).execute();
    }

    @Override public ActionResponse deleteProtocol(String idpId, String protocolId) { return deleteWithResponse(protocol(idpId, protocolId)).execute(); }
}
