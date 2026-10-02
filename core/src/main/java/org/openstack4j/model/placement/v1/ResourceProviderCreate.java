package org.openstack4j.model.placement.v1;

import java.util.Objects;

import org.openstack4j.model.ModelEntity;

/** Request body of {@code POST /resource_providers}. */
public final class ResourceProviderCreate implements ModelEntity {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String uuid;
    private final String parentProviderUuid;

    private ResourceProviderCreate(String name, String uuid, String parentProviderUuid) {
        this.name = Objects.requireNonNull(name, "name");
        this.uuid = uuid;
        this.parentProviderUuid = parentProviderUuid;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    /** @return the UUID to create the provider with, or {@code null} to let Placement choose one */
    public String getUuid() {
        return uuid;
    }

    public String getParentProviderUuid() {
        return parentProviderUuid;
    }

    public static final class Builder {
        private String name;
        private String uuid;
        private String parentProviderUuid;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder parentProviderUuid(String parentProviderUuid) {
            this.parentProviderUuid = parentProviderUuid;
            return this;
        }

        public ResourceProviderCreate build() {
            return new ResourceProviderCreate(name, uuid, parentProviderUuid);
        }
    }
}
