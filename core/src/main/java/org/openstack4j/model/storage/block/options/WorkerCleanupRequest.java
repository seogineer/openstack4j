package org.openstack4j.model.storage.block.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Body of {@code POST /workers/cleanup} (3.24+); only the fields set are sent. */
public class WorkerCleanupRequest {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public static WorkerCleanupRequest create() {
        return new WorkerCleanupRequest();
    }

    public WorkerCleanupRequest clusterName(String clusterName) { fields.put("cluster_name", clusterName); return this; }
    public WorkerCleanupRequest host(String host) { fields.put("host", host); return this; }
    public WorkerCleanupRequest binary(String binary) { fields.put("binary", binary); return this; }
    public WorkerCleanupRequest serviceId(int serviceId) { fields.put("service_id", serviceId); return this; }
    /** Clean services that are up as well; not recommended. */
    public WorkerCleanupRequest isUp(boolean isUp) { fields.put("is_up", isUp); return this; }
    public WorkerCleanupRequest disabled(boolean disabled) { fields.put("disabled", disabled); return this; }
    public WorkerCleanupRequest resourceId(String resourceId) { fields.put("resource_id", resourceId); return this; }
    public WorkerCleanupRequest resourceType(String resourceType) { fields.put("resource_type", resourceType); return this; }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
