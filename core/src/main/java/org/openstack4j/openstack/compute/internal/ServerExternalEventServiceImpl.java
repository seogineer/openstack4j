package org.openstack4j.openstack.compute.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.compute.ServerExternalEventService;
import org.openstack4j.model.compute.ExternalEvent;
import org.openstack4j.model.compute.ExternalEventCreate;
import org.openstack4j.openstack.compute.domain.JsonBody;
import org.openstack4j.openstack.compute.domain.NovaExternalEvent;
import org.openstack4j.openstack.internal.MicroVersion;

/** {@code POST /os-server-external-events} */
public class ServerExternalEventServiceImpl extends BaseComputeServices implements ServerExternalEventService {

    private static final Map<String, MicroVersion> EVENT_FLOORS = Map.of(
            "volume-extended", ComputeMicroVersions.V(51),
            "power-update", ComputeMicroVersions.V(76),
            "accelerator-request-bound", ComputeMicroVersions.V(82));

    @Override
    public List<? extends ExternalEvent> create(List<ExternalEventCreate> events) {
        Objects.requireNonNull(events);
        List<Map<String, Object>> body = new ArrayList<>();
        for (ExternalEventCreate e : events) {
            MicroVersion floor = EVENT_FLOORS.get(e.getName());
            if (floor != null)
                requireMicroVersion("External event " + e.getName(), floor);
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("name", e.getName());
            event.put("server_uuid", e.getServerUuid());
            if (e.getStatus() != null) event.put("status", e.getStatus());
            if (e.getTag() != null) event.put("tag", e.getTag());
            body.add(event);
        }
        return post(NovaExternalEvent.NovaExternalEvents.class, uri("/os-server-external-events"))
                .entity(JsonBody.of(Collections.singletonMap("events", body)))
                .execute().getList();
    }
}
