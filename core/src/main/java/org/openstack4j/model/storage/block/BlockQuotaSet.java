package org.openstack4j.model.storage.block;

import java.util.Map;

import org.openstack4j.common.Buildable;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.storage.block.builder.BlockQuotaSetBuilder;

/**
 * An OpenStack Quota-Set
 *
 * @author Jeremy Unruh
 */
public interface BlockQuotaSet extends ModelEntity, Buildable<BlockQuotaSetBuilder> {

    /**
     * @return the identifier
     */
    String getId();

    /**
     * @return the Snapshots.
     */
    int getSnapshots();

    /**
     * @return the Volumes
     */
    int getVolumes();

    /**
     * @return the gigabytes
     */
    int getGigabytes();

    Map<String, Integer> getVolumeTypesQuotas();

    /** @return backups */
    default Integer getBackups() { return null; }
    /** @return backup_gigabytes */
    default Integer getBackupGigabytes() { return null; }
    /** @return per_volume_gigabytes */
    default Integer getPerVolumeGigabytes() { return null; }
    /** @return groups */
    default Integer getGroups() { return null; }
}
