package org.openstack4j.openstack.trove.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.trove.*;

/**
 * Trove API Implementation
 *
 * @author sumit gandhi
 */
public class TroveServiceImpl extends BaseTroveServices implements TroveService {

    /**
     * {@inheritDoc}
     */
    @Override
    public DatastoreService datastoreService() {
        return Apis.get(DatastoreService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseService databaseService() {
        return Apis.get(DatabaseService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserService databaseUsersService() {
        return Apis.get(UserService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InstanceFlavorService flavorService() {
        return Apis.get(InstanceFlavorService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InstanceService instanceService() {
        return Apis.get(InstanceService.class);
    }

    @Override
    public org.openstack4j.api.trove.ext.TroveInstanceExtService instancesExt() {
        return Apis.get(org.openstack4j.api.trove.ext.TroveInstanceExtService.class);
    }

    @Override
    public org.openstack4j.api.trove.ext.TroveBackupService backups() {
        return Apis.get(org.openstack4j.api.trove.ext.TroveBackupService.class);
    }

    @Override
    public org.openstack4j.api.trove.ext.BackupStrategyService backupStrategies() {
        return Apis.get(org.openstack4j.api.trove.ext.BackupStrategyService.class);
    }

    @Override
    public org.openstack4j.api.trove.ext.ConfigurationGroupService configurations() {
        return Apis.get(org.openstack4j.api.trove.ext.ConfigurationGroupService.class);
    }

    @Override
    public org.openstack4j.api.trove.ext.TroveDatastoreExtService datastoresExt() {
        return Apis.get(org.openstack4j.api.trove.ext.TroveDatastoreExtService.class);
    }

    @Override
    public org.openstack4j.api.trove.ext.TroveAdminService troveAdmin() {
        return Apis.get(org.openstack4j.api.trove.ext.TroveAdminService.class);
    }
}
