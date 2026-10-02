package org.openstack4j.model.storage.block;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Log levels of one service ({@code PUT /os-services/get-log}, 3.32+). */
public interface ServiceLogLevel extends ModelEntity {
    String getBinary();
    String getHost();
    /** @return logger prefix to level */
    Map<String, String> getLevels();
}
