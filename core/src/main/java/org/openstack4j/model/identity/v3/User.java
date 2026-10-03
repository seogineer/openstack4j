package org.openstack4j.model.identity.v3;

import java.util.List;
import java.util.Date;
import java.util.Map;

import org.openstack4j.common.Buildable;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.identity.v3.builder.UserBuilder;

/**
 * identity v3 user model class
 *
 * @see <a href="http://developer.openstack.org/api-ref-identity-v3.html#users-v3">API reference</a>
 */
public interface User extends ModelEntity, Buildable<UserBuilder> {

    /**
     * Globally unique within the owning domain.
     *
     * @return the Id of the user
     */
    String getId();

    /**
     * @return the name of the user
     */
    String getName();

    /**
     * @return the email of the user
     */
    String getEmail();

    /**
     * @return the description of the user
     */
    String getDescription();

    /** @return the user options (for example {@code ignore_password_expiry}, {@code multi_factor_auth_enabled}); null if absent */
    default Map<String, Object> getOptions() {
        return null;
    }

    /** @return when the password expires; null if it never expires */
    default Date getPasswordExpiresAt() {
        return null;
    }

    /** @return the federated identities ({@code idp_id} and {@code protocols}); null if absent */
    default List<Map<String, Object>> getFederated() {
        return null;
    }

    /**
     * @return the password of the user
     */
    String getPassword();

    /**
     * @return the defaultProjectId of the user
     */
    String getDefaultProjectId();

    /**
     * @return the domainId of the user
     */
    String getDomainId();

    /**
     * @return the domain of the user
     */
    Domain getDomain();

    /**
     * @return the links of the user
     */
    Map<String, String> getLinks();

    /**
     * @return the enabled status of the user
     */
    boolean isEnabled();

    /**
     * sets the enabled status of the user
     *
     * @param enabled the enabled
     */
    void setEnabled(Boolean enabled);

}
