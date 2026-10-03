package org.openstack4j.openstack.identity.v3.internal;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.TrustService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.Trust;
import org.openstack4j.model.identity.v3.options.TrustCreate;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole.Roles;
import org.openstack4j.openstack.identity.v3.domain.KeystoneTrust;
import org.openstack4j.openstack.identity.v3.domain.KeystoneTrust.Trusts;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class TrustServiceImpl extends BaseIdentityServices implements TrustService {

    private static final String TRUSTS = "/OS-TRUST/trusts";

    @Override
    public List<? extends Trust> list() {
        return get(Trusts.class, TRUSTS).execute().getList();
    }

    @Override
    public List<? extends Trust> list(String trustorUserId, String trusteeUserId) {
        return get(Trusts.class, TRUSTS).param(trustorUserId != null, "trustor_user_id", trustorUserId)
                .param(trusteeUserId != null, "trustee_user_id", trusteeUserId).execute().getList();
    }

    @Override
    public Trust get(String trustId) {
        return get(KeystoneTrust.class, TRUSTS, "/", Objects.requireNonNull(trustId)).execute();
    }

    @Override
    public Trust create(TrustCreate create) {
        return post(KeystoneTrust.class, TRUSTS).entity(JsonBody.of("trust", Objects.requireNonNull(create).toMap())).execute();
    }

    @Override
    public ActionResponse delete(String trustId) {
        return deleteWithResponse(TRUSTS, "/", Objects.requireNonNull(trustId)).execute();
    }

    @Override
    public List<? extends Role> roles(String trustId) {
        return get(Roles.class, TRUSTS, "/", Objects.requireNonNull(trustId), "/roles").execute().getList();
    }

    @Override
    public Role getRole(String trustId, String roleId) {
        return get(KeystoneRole.class, TRUSTS, "/", Objects.requireNonNull(trustId), "/roles/", Objects.requireNonNull(roleId)).execute();
    }

    @Override
    public ActionResponse checkRole(String trustId, String roleId) {
        return head(ActionResponse.class, TRUSTS, "/", Objects.requireNonNull(trustId), "/roles/", Objects.requireNonNull(roleId)).execute();
    }
}
