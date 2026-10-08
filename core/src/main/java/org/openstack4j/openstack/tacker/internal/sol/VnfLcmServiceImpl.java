package org.openstack4j.openstack.tacker.internal.sol;

import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.tacker.VnfLcmService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;

/** VNF LCM v2 ({@code Version: 2.0.0}); {@link V1} is the v1 API ({@code Version: 1.3.0}). */
public class VnfLcmServiceImpl extends BaseTackerSolService implements VnfLcmService {

    static final String MERGE_PATCH = "application/merge-patch+json";

    public VnfLcmServiceImpl() {
        this("/vnflcm/v2", "2.0.0");
    }

    protected VnfLcmServiceImpl(String root, String version) {
        super(root, version);
    }

    /** VNF LCM v1. */
    public static class V1 extends VnfLcmServiceImpl {
        public V1() {
            super("/vnflcm/v1", "1.3.0");
        }
    }

    private String instance(String vnfInstanceId) {
        return path("/vnf_instances/" + id(vnfInstanceId));
    }

    private String opOcc(String lcmOpOccId) {
        return path("/vnf_lcm_op_occs/" + id(lcmOpOccId));
    }

    @Override
    public Map<String, Object> apiVersions() {
        return strict(get(Map.class, path("/api_versions")));
    }

    @Override
    public Map<String, Object> listVnfInstances(Map<String, String> params) {
        return page(path("/vnf_instances"), params);
    }

    @Override
    public Map<String, Object> createVnfInstance(Map<String, ?> request) {
        return strict(post(Map.class, path("/vnf_instances")).entity(JsonBody.of(body(request, "request"))));
    }

    @Override
    public Map<String, Object> getVnfInstance(String vnfInstanceId) {
        return show(instance(vnfInstanceId));
    }

    @Override
    public String updateVnfInstance(String vnfInstanceId, Map<String, ?> changes) {
        return accepted(patch(Void.class, instance(vnfInstanceId)).entity(JsonBody.of(body(changes, "changes"))).contentType(MERGE_PATCH));
    }

    @Override
    public ActionResponse deleteVnfInstance(String vnfInstanceId) {
        return act(deleteWithResponse(instance(vnfInstanceId)));
    }

    @Override
    public String instantiate(String vnfInstanceId, Map<String, ?> request) {
        return accepted(instance(vnfInstanceId) + "/instantiate", body(request, "request"));
    }

    @Override
    public String terminate(String vnfInstanceId, Map<String, ?> request) {
        return accepted(instance(vnfInstanceId) + "/terminate", body(request, "request"));
    }

    @Override
    public String heal(String vnfInstanceId, Map<String, ?> request) {
        return accepted(instance(vnfInstanceId) + "/heal", request);
    }

    @Override
    public String scale(String vnfInstanceId, Map<String, ?> request) {
        return accepted(instance(vnfInstanceId) + "/scale", body(request, "request"));
    }

    @Override
    public String changeExtConn(String vnfInstanceId, Map<String, ?> request) {
        return accepted(instance(vnfInstanceId) + "/change_ext_conn", body(request, "request"));
    }

    @Override
    public String changeVnfpkg(String vnfInstanceId, Map<String, ?> request) {
        return accepted(instance(vnfInstanceId) + "/change_vnfpkg", body(request, "request"));
    }

    @Override
    public Map<String, Object> listLcmOpOccs(Map<String, String> params) {
        return page(path("/vnf_lcm_op_occs"), params);
    }

    @Override
    public Map<String, Object> getLcmOpOcc(String lcmOpOccId) {
        return show(opOcc(lcmOpOccId));
    }

    @Override
    public ActionResponse retry(String lcmOpOccId) {
        return act(postWithResponse(opOcc(lcmOpOccId) + "/retry"));
    }

    @Override
    public ActionResponse rollback(String lcmOpOccId) {
        return act(postWithResponse(opOcc(lcmOpOccId) + "/rollback"));
    }

    @Override
    public Map<String, Object> fail(String lcmOpOccId) {
        return strict(post(Map.class, opOcc(lcmOpOccId) + "/fail"));
    }

    @Override
    public ActionResponse cancel(String lcmOpOccId, String cancelMode) {
        return act(postWithResponse(opOcc(lcmOpOccId) + "/cancel").entity(JsonBody.of(Map.of("cancelMode", Objects.requireNonNull(cancelMode, "cancelMode")))));
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
