package org.openstack4j.model.compute;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Status of the instance usage audit task for one period (admin only). */
public interface InstanceUsageAuditLog extends ModelEntity {
    List<String> getHostsNotRun();
    Map<String, Object> getLog();
    Integer getNumHosts();
    Integer getNumHostsDone();
    Integer getNumHostsNotRun();
    Integer getNumHostsRunning();
    String getOverallStatus();
    String getPeriodBeginning();
    String getPeriodEnding();
    Integer getTotalErrors();
    Integer getTotalInstances();
}
