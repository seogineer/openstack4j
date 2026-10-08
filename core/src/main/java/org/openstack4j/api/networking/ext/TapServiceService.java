package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.TapService;
import org.openstack4j.model.network.options.TapServiceOptions;

/** Tap services ({@code /v2.0/taas/tap_services}). */
public interface TapServiceService extends RestService {

    /** @return the tap services */
    List<? extends TapService> list();

    /** @param filters query parameters such as {@code name}, {@code project_id} */
    List<? extends TapService> list(Map<String, String> filters);

    /** @return the tap service, or {@code null} when it does not exist */
    TapService get(String id);

    TapService create(TapServiceOptions options);

    /** Changes only the fields set in {@code options}. */
    TapService update(String id, TapServiceOptions options);

    ActionResponse delete(String id);
}
