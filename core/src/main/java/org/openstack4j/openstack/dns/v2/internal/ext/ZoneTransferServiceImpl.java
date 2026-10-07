package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.dns.v2.ext.ZoneTransferService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.ZoneTransferAccept;
import org.openstack4j.model.dns.v2.ext.ZoneTransferRequest;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateZoneTransferAccept;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateZoneTransferAccept.DesignateZoneTransferAcceptList;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateZoneTransferRequest;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateZoneTransferRequest.DesignateZoneTransferRequestList;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class ZoneTransferServiceImpl extends BaseDesignateExtService implements ZoneTransferService {

    private static final String REQUESTS = "/zones/tasks/transfer_requests";
    private static final String ACCEPTS = "/zones/tasks/transfer_accepts";

    private static Map<String, Object> body(String targetProjectId, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (targetProjectId != null)
            body.put("target_project_id", targetProjectId);
        if (description != null)
            body.put("description", description);
        return body;
    }

    @Override
    public ZoneTransferRequest createRequest(String zoneId, String targetProjectId, String description) {
        return post(DesignateZoneTransferRequest.class, "/zones/" + id(zoneId) + "/tasks/transfer_requests")
                .entity(JsonBody.of(body(targetProjectId, description))).execute(propagate404());
    }

    @Override
    public List<? extends ZoneTransferRequest> listRequests() {
        return listRequests(null);
    }

    @Override
    public List<? extends ZoneTransferRequest> listRequests(Map<String, String> filters) {
        return listOf(DesignateZoneTransferRequestList.class, REQUESTS, filters);
    }

    @Override
    public ZoneTransferRequest getRequest(String requestId) {
        return show(DesignateZoneTransferRequest.class, REQUESTS + "/" + id(requestId));
    }

    @Override
    public ZoneTransferRequest updateRequest(String requestId, String targetProjectId, String description) {
        return patch(DesignateZoneTransferRequest.class, REQUESTS + "/" + id(requestId))
                .entity(JsonBody.of(body(targetProjectId, description))).execute(propagate404());
    }

    @Override
    public ActionResponse deleteRequest(String requestId) {
        return remove(REQUESTS + "/" + id(requestId));
    }

    @Override
    public ZoneTransferAccept accept(String requestId, String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("key", Objects.requireNonNull(key, "key"));
        body.put("zone_transfer_request_id", id(requestId));
        return post(DesignateZoneTransferAccept.class, ACCEPTS).entity(JsonBody.of(body)).execute(propagate404());
    }

    @Override
    public List<? extends ZoneTransferAccept> listAccepts() {
        return listAccepts(null);
    }

    @Override
    public List<? extends ZoneTransferAccept> listAccepts(Map<String, String> filters) {
        return listOf(DesignateZoneTransferAcceptList.class, ACCEPTS, filters);
    }

    @Override
    public ZoneTransferAccept getAccept(String acceptId) {
        return show(DesignateZoneTransferAccept.class, ACCEPTS + "/" + id(acceptId));
    }
}
