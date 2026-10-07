package org.openstack4j.model.octavia.ext;

import org.openstack4j.model.ModelEntity;

/** Octavia quotas of a project; null means the default applies, -1 means unlimited. */
public interface OctaviaQuota extends ModelEntity {
    String getProjectId();
    Integer getLoadbalancer();
    Integer getListener();
    Integer getMember();
    Integer getPool();
    Integer getHealthmonitor();
    Integer getL7policy();
    Integer getL7rule();
}
