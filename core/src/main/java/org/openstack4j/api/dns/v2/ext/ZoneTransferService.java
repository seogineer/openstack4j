package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.ZoneTransferAccept;
import org.openstack4j.model.dns.v2.ext.ZoneTransferRequest;

/**
 * Zone ownership transfers: the owner creates a request ({@code /v2/zones/{zone_id}/tasks/transfer_requests}), the
 * receiving project accepts it with the request's id and key ({@code /v2/zones/tasks/transfer_accepts}).
 */
public interface ZoneTransferService extends RestService {

    /**
     * @param targetProjectId the only project that may accept, or {@code null} for any project that has the key
     * @param description     may be {@code null}
     * @return the request with its {@code key}
     */
    ZoneTransferRequest createRequest(String zoneId, String targetProjectId, String description);

    List<? extends ZoneTransferRequest> listRequests();

    /** @param filters query parameters such as {@code status} */
    List<? extends ZoneTransferRequest> listRequests(Map<String, String> filters);

    /** @return the request, or {@code null} when it does not exist */
    ZoneTransferRequest getRequest(String requestId);

    /** Changes the description and/or the target project; a {@code null} argument is left unchanged. */
    ZoneTransferRequest updateRequest(String requestId, String targetProjectId, String description);

    ActionResponse deleteRequest(String requestId);

    /** Accepts a transfer request; the zone moves to the caller's project. */
    ZoneTransferAccept accept(String requestId, String key);

    List<? extends ZoneTransferAccept> listAccepts();

    /** @param filters query parameters such as {@code status} */
    List<? extends ZoneTransferAccept> listAccepts(Map<String, String> filters);

    /** @return the acceptance, or {@code null} when it does not exist */
    ZoneTransferAccept getAccept(String acceptId);
}
