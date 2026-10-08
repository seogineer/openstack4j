package org.openstack4j.openstack.tacker.internal.sol;

import java.util.Map;

import org.openstack4j.api.tacker.VnfPmService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class VnfPmServiceImpl extends BaseTackerSolService implements VnfPmService {

    public VnfPmServiceImpl() {
        super("/vnfpm/v2", "2.1.0");
    }

    private String job(String pmJobId) {
        return path("/pm_jobs/" + id(pmJobId));
    }

    private String threshold(String thresholdId) {
        return path("/thresholds/" + id(thresholdId));
    }

    @Override
    public Map<String, Object> listPmJobs(Map<String, String> params) {
        return page(path("/pm_jobs"), params);
    }

    @Override
    public Map<String, Object> createPmJob(Map<String, ?> request) {
        return strict(post(Map.class, path("/pm_jobs")).entity(JsonBody.of(body(request, "request"))));
    }

    @Override
    public Map<String, Object> getPmJob(String pmJobId) {
        return show(job(pmJobId));
    }

    @Override
    public Map<String, Object> updatePmJob(String pmJobId, Map<String, ?> changes) {
        return strict(patch(Map.class, job(pmJobId)).entity(JsonBody.of(body(changes, "changes"))).contentType(VnfLcmServiceImpl.MERGE_PATCH));
    }

    @Override
    public ActionResponse deletePmJob(String pmJobId) {
        return act(deleteWithResponse(job(pmJobId)));
    }

    @Override
    public Map<String, Object> getReport(String pmJobId, String reportId) {
        return show(job(pmJobId) + "/reports/" + id(reportId));
    }

    @Override
    public Map<String, Object> listThresholds(Map<String, String> params) {
        return page(path("/thresholds"), params);
    }

    @Override
    public Map<String, Object> createThreshold(Map<String, ?> request) {
        return strict(post(Map.class, path("/thresholds")).entity(JsonBody.of(body(request, "request"))));
    }

    @Override
    public Map<String, Object> getThreshold(String thresholdId) {
        return show(threshold(thresholdId));
    }

    @Override
    public Map<String, Object> updateThreshold(String thresholdId, Map<String, ?> changes) {
        return strict(patch(Map.class, threshold(thresholdId)).entity(JsonBody.of(body(changes, "changes"))).contentType(VnfLcmServiceImpl.MERGE_PATCH));
    }

    @Override
    public ActionResponse deleteThreshold(String thresholdId) {
        return act(deleteWithResponse(threshold(thresholdId)));
    }
}
