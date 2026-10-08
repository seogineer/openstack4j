package org.openstack4j.api.trove.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Backup strategies: where backups of a project or an instance are stored ({@code /backup_strategies}). */
public interface BackupStrategyService extends RestService {

    /** @param filters {@code instance_id}, {@code project_id} (admin) @return the strategies ({@code backend}, {@code swift_container} …) */
    List<Map<String, Object>> list(Map<String, String> filters);

    /**
     * @param instanceId     the instance, or {@code null} for the whole project
     * @param swiftContainer the object storage container for the backups
     * @return the created strategy
     */
    Map<String, Object> create(String instanceId, String swiftContainer);

    /** @param instanceId the instance, or {@code null} for the project's strategy */
    ActionResponse delete(String instanceId);
}
