package org.openstack4j.openstack.heat.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.heat.domain.HeatSoftwareConfig;

/** {@code {"software_configs": [...]}}. */
public class HeatSoftwareConfigs extends ListResult<HeatSoftwareConfig> {

    private static final long serialVersionUID = 1L;

    @JsonProperty("software_configs")
    private List<HeatSoftwareConfig> list;

    @Override
    protected List<HeatSoftwareConfig> value() {
        return list;
    }
}
