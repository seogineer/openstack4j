package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.TapMirror;
import org.openstack4j.model.network.options.TapMirrorOptions;

/** Tap mirrors ({@code /v2.0/taas/tap_mirrors}). */
public interface TapMirrorService extends RestService {

    /** @return the tap mirrors */
    List<? extends TapMirror> list();

    /** @param filters query parameters such as {@code name}, {@code project_id} */
    List<? extends TapMirror> list(Map<String, String> filters);

    /** @return the tap mirror, or {@code null} when it does not exist */
    TapMirror get(String id);

    TapMirror create(TapMirrorOptions options);

    /** Changes only the fields set in {@code options}. */
    TapMirror update(String id, TapMirrorOptions options);

    ActionResponse delete(String id);
}
