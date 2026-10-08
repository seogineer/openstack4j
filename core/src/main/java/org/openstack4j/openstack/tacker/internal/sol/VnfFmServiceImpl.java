package org.openstack4j.openstack.tacker.internal.sol;

import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.tacker.VnfFmService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class VnfFmServiceImpl extends BaseTackerSolService implements VnfFmService {

    public VnfFmServiceImpl() {
        super("/vnffm/v1", "1.3.0");
    }

    @Override
    public Map<String, Object> listAlarms(Map<String, String> params) {
        return page(path("/alarms"), params);
    }

    @Override
    public Map<String, Object> getAlarm(String alarmId) {
        return show(path("/alarms/" + id(alarmId)));
    }

    @Override
    public Map<String, Object> acknowledgeAlarm(String alarmId, String ackState) {
        return strict(patch(Map.class, path("/alarms/" + id(alarmId))).entity(JsonBody.of(Map.of("ackState", Objects.requireNonNull(ackState, "ackState"))))
                .contentType(VnfLcmServiceImpl.MERGE_PATCH));
    }

    @Override
    public Map<String, Object> listSubscriptions(Map<String, String> params) {
        return page(path("/subscriptions"), params);
    }

    @Override
    public Map<String, Object> createSubscription(Map<String, ?> request) {
        return strict(post(Map.class, path("/subscriptions")).entity(JsonBody.of(body(request, "request"))));
    }

    @Override
    public Map<String, Object> getSubscription(String subscriptionId) {
        return show(path("/subscriptions/" + id(subscriptionId)));
    }

    @Override
    public ActionResponse deleteSubscription(String subscriptionId) {
        return act(deleteWithResponse(path("/subscriptions/" + id(subscriptionId))));
    }
}
