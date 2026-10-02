package org.openstack4j.model.placement.v1;

import java.util.Objects;

import org.openstack4j.model.ModelEntity;

/** Request body of {@code PUT /resource_providers/{uuid}}. The name is always required by Placement. */
public final class ResourceProviderUpdate implements ModelEntity {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String parentProviderUuid;
    private final boolean unparent;

    private ResourceProviderUpdate(String name, String parentProviderUuid, boolean unparent) {
        this.name = Objects.requireNonNull(name, "name");
        this.parentProviderUuid = parentProviderUuid;
        this.unparent = unparent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    /** @return the new parent, or {@code null} when the parent is left alone (or removed, see {@link #isUnparent()}) */
    public String getParentProviderUuid() {
        return parentProviderUuid;
    }

    /** @return {@code true} to make the provider a root provider again (placement 1.37) */
    public boolean isUnparent() {
        return unparent;
    }

    public static final class Builder {
        private String name;
        private String parentProviderUuid;
        private boolean unparent;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** Re-parents the provider (placement 1.37 when it already has a different parent). */
        public Builder parentProviderUuid(String parentProviderUuid) {
            this.parentProviderUuid = parentProviderUuid;
            this.unparent = false;
            return this;
        }

        /** Removes the parent (placement 1.37). */
        public Builder unparent() {
            this.unparent = true;
            this.parentProviderUuid = null;
            return this;
        }

        public ResourceProviderUpdate build() {
            return new ResourceProviderUpdate(name, parentProviderUuid, unparent);
        }
    }
}
