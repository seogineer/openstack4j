package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.TapFlow;
import org.openstack4j.model.network.options.TapFlowOptions;

/** Tap flows ({@code /v2.0/taas/tap_flows}). */
public interface TapFlowService extends RestService {

    /** @return the tap flows */
    List<? extends TapFlow> list();

    /** @param filters query parameters such as {@code name}, {@code project_id} */
    List<? extends TapFlow> list(Map<String, String> filters);

    /** @return the tap flow, or {@code null} when it does not exist */
    TapFlow get(String id);

    TapFlow create(TapFlowOptions options);

    /** Changes only the fields set in {@code options}. */
    TapFlow update(String id, TapFlowOptions options);

    ActionResponse delete(String id);
}
