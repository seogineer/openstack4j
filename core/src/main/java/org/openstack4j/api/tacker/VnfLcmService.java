package org.openstack4j.api.tacker;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/**
 * VNF lifecycle management (ETSI NFV-SOL 002/003, Tacker {@code /vnflcm/v2} or {@code /vnflcm/v1}). Results are
 * {@code Map}s; a list returns {@code items} and {@code next}, the {@code nextpage_opaque_marker} of the following page
 * (or {@code null}). Lifecycle operations are asynchronous and return the id of their operation occurrence (from the
 * {@code Location} header, {@code null} if the server sends none); a failed request raises.
 */
public interface VnfLcmService extends RestService {

    /** @return {@code uriPrefix} and {@code apiVersions} of this API */
    Map<String, Object> apiVersions();

    /** @param params e.g. {@code filter}, {@code all_fields}, {@code fields}, {@code exclude_fields}, {@code nextpage_opaque_marker}, or {@code null} */
    Map<String, Object> listVnfInstances(Map<String, String> params);

    /** @param request {@code vnfdId} and optionally {@code vnfInstanceName}, {@code vnfInstanceDescription}, {@code metadata} */
    Map<String, Object> createVnfInstance(Map<String, ?> request);

    /** @return the VNF instance, or {@code null} when it does not exist */
    Map<String, Object> getVnfInstance(String vnfInstanceId);

    /** Modifies the instance's information (a JSON merge patch). @return the operation occurrence id */
    String updateVnfInstance(String vnfInstanceId, Map<String, ?> changes);

    ActionResponse deleteVnfInstance(String vnfInstanceId);

    /** @param request {@code flavourId} and e.g. {@code instantiationLevelId}, {@code extVirtualLinks}, {@code vimConnectionInfo}, {@code additionalParams} */
    String instantiate(String vnfInstanceId, Map<String, ?> request);

    /** @param request {@code terminationType} ({@code FORCEFUL} or {@code GRACEFUL}) and optionally {@code gracefulTerminationTimeout} */
    String terminate(String vnfInstanceId, Map<String, ?> request);

    /** @param request e.g. {@code cause}, {@code vnfcInstanceId}, {@code additionalParams}, or {@code null} */
    String heal(String vnfInstanceId, Map<String, ?> request);

    /** @param request {@code type} ({@code SCALE_OUT}/{@code SCALE_IN}), {@code aspectId}, optional {@code numberOfSteps} */
    String scale(String vnfInstanceId, Map<String, ?> request);

    /** @param request {@code extVirtualLinks} and optionally {@code vimConnectionInfo}, {@code additionalParams} */
    String changeExtConn(String vnfInstanceId, Map<String, ?> request);

    /** Changes the current VNF package (v2 only). @param request {@code vnfdId} and {@code additionalParams} */
    String changeVnfpkg(String vnfInstanceId, Map<String, ?> request);

    Map<String, Object> listLcmOpOccs(Map<String, String> params);

    /** @return the operation occurrence, or {@code null} when it does not exist */
    Map<String, Object> getLcmOpOcc(String lcmOpOccId);

    ActionResponse retry(String lcmOpOccId);

    ActionResponse rollback(String lcmOpOccId);

    /** Marks a failed operation as finally failed. @return the operation occurrence */
    Map<String, Object> fail(String lcmOpOccId);

    /** Cancels an ongoing operation (v1 only). @param cancelMode {@code GRACEFUL} or {@code FORCEFUL} */
    ActionResponse cancel(String lcmOpOccId, String cancelMode);

    Map<String, Object> listSubscriptions(Map<String, String> params);

    /** @param request {@code callbackUri} and optionally {@code filter}, {@code authentication} @return the subscription */
    Map<String, Object> createSubscription(Map<String, ?> request);

    /** @return the subscription, or {@code null} when it does not exist */
    Map<String, Object> getSubscription(String subscriptionId);

    ActionResponse deleteSubscription(String subscriptionId);
}
