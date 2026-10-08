package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.SfcServiceGraph;
import org.openstack4j.model.network.options.SfcServiceGraphOptions;

/** SFC service graphs ({@code /v2.0/sfc/service_graphs}). */
public interface SfcServiceGraphService extends RestService {

    /** @return the SFC service graphs */
    List<? extends SfcServiceGraph> list();

    /** @param filters query parameters such as {@code name}, {@code project_id} */
    List<? extends SfcServiceGraph> list(Map<String, String> filters);

    /** @return the service graph, or {@code null} when it does not exist */
    SfcServiceGraph get(String id);

    SfcServiceGraph create(SfcServiceGraphOptions options);

    /** Changes only the fields set in {@code options}. */
    SfcServiceGraph update(String id, SfcServiceGraphOptions options);

    ActionResponse delete(String id);
}
