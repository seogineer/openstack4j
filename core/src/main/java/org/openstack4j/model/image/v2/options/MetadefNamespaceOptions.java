package org.openstack4j.model.image.v2.options;

import java.util.Objects;

/** Body of a metadef namespace create or (full) update. */
public class MetadefNamespaceOptions extends ImageAttributes<MetadefNamespaceOptions> {

    public static MetadefNamespaceOptions create(String namespace) {
        return new MetadefNamespaceOptions().put("namespace", Objects.requireNonNull(namespace));
    }

    /** PUT replaces the namespace, so the (possibly new) namespace name is required. */
    public static MetadefNamespaceOptions update(String namespace) {
        return create(namespace);
    }

    @Override
    protected MetadefNamespaceOptions self() {
        return this;
    }

    public MetadefNamespaceOptions displayName(String displayName) { return put("display_name", displayName); }
    public MetadefNamespaceOptions description(String description) { return put("description", description); }
    /** public or private. */
    public MetadefNamespaceOptions visibility(String visibility) { return put("visibility", visibility); }
    /** A protected namespace cannot be deleted. */
    public MetadefNamespaceOptions protectedNamespace(Boolean isProtected) { return put("protected", isProtected); }
}
