package org.openstack4j.api.identity.v3;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.ApplicationCredential;
import org.openstack4j.model.identity.v3.options.ApplicationCredentialCreate;

/**
 * Application credentials of a user ({@code /v3/users/{user_id}/application_credentials}).
 */
public interface ApplicationCredentialService extends RestService {

    /**
     * @param userId the owning user
     * @return the user's application credentials
     */
    List<? extends ApplicationCredential> list(String userId);

    /**
     * @param userId the owning user
     * @param name   the credential name to filter on
     * @return the user's application credentials with that name
     */
    List<? extends ApplicationCredential> list(String userId, String name);

    /**
     * @param userId the owning user
     * @param id     the credential id
     * @return the credential (without its secret)
     */
    ApplicationCredential get(String userId, String id);

    /**
     * Creates an application credential. The secret is returned only here.
     *
     * @param userId the owning user
     * @param create the credential to create
     * @return the created credential including {@link ApplicationCredential#getSecret()}
     */
    ApplicationCredential create(String userId, ApplicationCredentialCreate create);

    /**
     * @param userId the owning user
     * @param id     the credential id
     * @return the action response
     */
    ActionResponse delete(String userId, String id);
}
