package org.openstack4j.model.trove.ext.options;

import java.util.Map;
import java.util.Objects;

/** Body of a backup create or update; only the fields set are sent. */
public class BackupOptions extends TroveAttributes<BackupOptions> {

    public static BackupOptions create(String name) {
        return new BackupOptions().put("name", Objects.requireNonNull(name, "name"));
    }

    @Override
    protected BackupOptions self() {
        return this;
    }

    public BackupOptions name(String value) {
        return put("name", value);
    }

    /** The instance to back up (or use {@code restore_from} for a remote backup). */
    public BackupOptions instance(String value) {
        return put("instance", value);
    }

    public BackupOptions description(String value) {
        return put("description", value);
    }

    /** Makes an incremental backup on top of this backup. */
    public BackupOptions parentId(String value) {
        return put("parent_id", value);
    }

    /** 1 for an incremental backup of the last backup. */
    public BackupOptions incremental(Integer value) {
        return put("incremental", value);
    }

    public BackupOptions swiftContainer(String value) {
        return put("swift_container", value);
    }

    /** e.g. {@code swift} or {@code cinder}. */
    public BackupOptions storageDriver(String value) {
        return put("storage_driver", value);
    }

    /** Registers a backup copied from another region. */
    public BackupOptions restoreFrom(Map<String, Object> value) {
        return put("restore_from", value);
    }
}
