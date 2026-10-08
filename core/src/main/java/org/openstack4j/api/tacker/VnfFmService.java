package org.openstack4j.api.tacker;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** VNF fault management (ETSI NFV-SOL 002/003, Tacker {@code /vnffm/v1}). Lists return {@code items} and {@code next}. */
public interface VnfFmService extends RestService {

    /** @param params e.g. {@code filter} ({@code (eq,perceivedSeverity,CRITICAL)}), {@code nextpage_opaque_marker}, or {@code null} */
    Map<String, Object> listAlarms(Map<String, String> params);

    /** @return the alarm, or {@code null} when it does not exist */
    Map<String, Object> getAlarm(String alarmId);

    /** @param ackState {@code ACKNOWLEDGED} or {@code UNACKNOWLEDGED} @return the changes made */
    Map<String, Object> acknowledgeAlarm(String alarmId, String ackState);

    Map<String, Object> listSubscriptions(Map<String, String> params);

    /** @param request {@code callbackUri} and optionally {@code filter}, {@code authentication} */
    Map<String, Object> createSubscription(Map<String, ?> request);

    /** @return the subscription, or {@code null} when it does not exist */
    Map<String, Object> getSubscription(String subscriptionId);

    ActionResponse deleteSubscription(String subscriptionId);
}
