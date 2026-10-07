package org.openstack4j.model.heat.ext;

import org.openstack4j.model.ModelEntity;

/** A heat-engine service record (admin). */
public interface HeatEngineService extends ModelEntity {
    String getId();
    String getBinary();
    String getHost();
    String getHostname();
    String getEngineId();
    String getTopic();
    String getStatus();
    Integer getReportInterval();
    String getCreatedAt();
    String getUpdatedAt();
    String getDeletedAt();
}
