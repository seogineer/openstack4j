package org.openstack4j.openstack.heat.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.heat.ext.HeatInfoService;
import org.openstack4j.model.heat.ext.HeatEngineService;
import org.openstack4j.openstack.heat.domain.ext.HeatEngineServiceEntity.Services;

public class HeatInfoServiceImpl extends BaseHeatExtService implements HeatInfoService {

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> buildInfo() {
        Map<String, Object> info = showStrict(Map.class, "/build_info");
        return info == null ? Collections.emptyMap() : info;
    }

    @Override public List<? extends HeatEngineService> services() { return listOf(Services.class, "/services", null); }
}
