package org.openstack4j.api.compute;

import org.openstack4j.common.RestService;
import org.openstack4j.model.compute.InstanceUsageAuditLog;

/** Instance usage audit logs ({@code /os-instance_usage_audit_log}); admin only. */
public interface InstanceUsageAuditLogService extends RestService {

    /** Status of the current audit period. */
    InstanceUsageAuditLog list();

    /**
     * @param before a date time; the audit period that ends before it is returned
     */
    InstanceUsageAuditLog get(String before);
}
