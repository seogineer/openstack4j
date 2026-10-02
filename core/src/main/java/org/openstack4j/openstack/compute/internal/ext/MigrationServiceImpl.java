package org.openstack4j.openstack.compute.internal.ext;

import java.util.Objects;
import java.util.Set;

import org.openstack4j.model.compute.ext.MigrationListOptions;
import org.openstack4j.openstack.internal.microversion.MicroVersions;

import java.util.List;

import org.openstack4j.api.compute.ext.MigrationService;
import org.openstack4j.model.compute.ext.Migration;
import org.openstack4j.model.compute.ext.MigrationsFilter;
import org.openstack4j.openstack.compute.domain.ext.ExtMigration.Migrations;
import org.openstack4j.openstack.compute.internal.BaseComputeServices;
import org.openstack4j.openstack.compute.internal.ComputeMicroVersions;

/**
 * API which supports the "os-migrations" extension.
 *
 * @author Jeremy Unruh
 */
public class MigrationServiceImpl extends BaseComputeServices implements MigrationService {

    private static final Set<String> LEGACY_FILTER_KEYS = Set.of("hidden", "host", "instance_uuid", "source_compute", "status", "migration_type");

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Migration> list() {
        return list((MigrationsFilter) null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Migration> list(MigrationsFilter filter) {
        Invocation<Migrations> inv = get(Migrations.class, uri("/os-migrations"));
        if (filter != null) {
            inv.params(filter.getConstraints());
            // 2.59 rejects query parameters outside its schema, such as cell_name
            if (!LEGACY_FILTER_KEYS.containsAll(filter.getConstraints().keySet()))
                capped(inv, ComputeMicroVersions.V(58));
        }
        return inv.execute().getList();
    }

    @Override
    public List<? extends Migration> list(MigrationListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Migration list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(Migrations.class, uri("/os-migrations")).params(options.toQueryParams()).execute().getList();
    }
}
