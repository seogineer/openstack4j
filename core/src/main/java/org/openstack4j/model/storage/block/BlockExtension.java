package org.openstack4j.model.storage.block;


import org.openstack4j.model.ModelEntity;

/** An API extension ({@code GET /extensions}). */
public interface BlockExtension extends ModelEntity {
    String getName();
    String getAlias();
    String getDescription();
    String getUpdated();
}
