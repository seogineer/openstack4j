package org.openstack4j.openstack.identity.v3.internal;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.ApplicationCredentialService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.ApplicationCredential;
import org.openstack4j.model.identity.v3.options.ApplicationCredentialCreate;
import org.openstack4j.openstack.identity.v3.domain.KeystoneApplicationCredential;
import org.openstack4j.openstack.identity.v3.domain.KeystoneApplicationCredential.ApplicationCredentials;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class ApplicationCredentialServiceImpl extends BaseIdentityServices implements ApplicationCredentialService {

    @Override
    public List<? extends ApplicationCredential> list(String userId) {
        return get(ApplicationCredentials.class, uri("/users/%s/application_credentials", Objects.requireNonNull(userId))).execute().getList();
    }

    @Override
    public List<? extends ApplicationCredential> list(String userId, String name) {
        return get(ApplicationCredentials.class, uri("/users/%s/application_credentials", Objects.requireNonNull(userId)))
                .param("name", Objects.requireNonNull(name)).execute().getList();
    }

    @Override
    public ApplicationCredential get(String userId, String id) {
        return get(KeystoneApplicationCredential.class, uri("/users/%s/application_credentials/%s", Objects.requireNonNull(userId), Objects.requireNonNull(id))).execute();
    }

    @Override
    public ApplicationCredential create(String userId, ApplicationCredentialCreate create) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(create);
        return post(KeystoneApplicationCredential.class, uri("/users/%s/application_credentials", userId))
                .entity(JsonBody.of("application_credential", create.toMap())).execute();
    }

    @Override
    public ActionResponse delete(String userId, String id) {
        return deleteWithResponse(uri("/users/%s/application_credentials/%s", Objects.requireNonNull(userId), Objects.requireNonNull(id))).execute();
    }
}
