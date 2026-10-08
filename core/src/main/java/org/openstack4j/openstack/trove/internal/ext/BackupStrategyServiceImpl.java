package org.openstack4j.openstack.trove.internal.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.trove.ext.BackupStrategyService;
import org.openstack4j.model.common.ActionResponse;

public class BackupStrategyServiceImpl extends BaseTroveExtService implements BackupStrategyService {

    private static final String PATH = "/backup_strategies";

    @Override
    public List<Map<String, Object>> list(Map<String, String> filters) {
        return listOf(PATH, "backup_strategies", filters);
    }

    @Override
    public Map<String, Object> create(String instanceId, String swiftContainer) {
        Map<String, Object> strategy = new LinkedHashMap<>();
        if (instanceId != null)
            strategy.put("instance_id", instanceId);
        strategy.put("swift_container", Objects.requireNonNull(swiftContainer, "swiftContainer"));
        return postFor(PATH, Map.of("backup_strategy", strategy), "backup_strategy");
    }

    @Override
    public ActionResponse delete(String instanceId) {
        Invocation<ActionResponse> invocation = deleteWithResponse(PATH);
        if (instanceId != null)
            invocation.param("instance_id", instanceId);
        return invocation.execute();
    }
}
