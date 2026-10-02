package org.openstack4j.openstack.compute.internal;

import java.util.Objects;

import org.openstack4j.api.compute.InstanceUsageAuditLogService;
import org.openstack4j.model.compute.InstanceUsageAuditLog;
import org.openstack4j.openstack.compute.domain.NovaInstanceUsageAuditLog;

/** {@code GET /os-instance_usage_audit_log[/{before}]} */
public class InstanceUsageAuditLogServiceImpl extends BaseComputeServices implements InstanceUsageAuditLogService {

    @Override
    public InstanceUsageAuditLog list() {
        NovaInstanceUsageAuditLog.Current current = get(NovaInstanceUsageAuditLog.Current.class, uri("/os-instance_usage_audit_log")).execute();
        return current == null ? null : current.log;
    }

    @Override
    public InstanceUsageAuditLog get(String before) {
        Objects.requireNonNull(before);
        NovaInstanceUsageAuditLog.Before log = get(NovaInstanceUsageAuditLog.Before.class, uri("/os-instance_usage_audit_log/%s", before)).execute();
        return log == null ? null : log.log;
    }
}
