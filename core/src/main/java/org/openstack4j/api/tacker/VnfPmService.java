package org.openstack4j.api.tacker;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** VNF performance management (ETSI NFV-SOL 002/003, Tacker {@code /vnfpm/v2}). Lists return {@code items} and {@code next}. */
public interface VnfPmService extends RestService {

    Map<String, Object> listPmJobs(Map<String, String> params);

    /** @param request {@code objectType}, {@code objectInstanceIds}, {@code criteria}, {@code callbackUri}, optional {@code subObjectInstanceIds}, {@code authentication} */
    Map<String, Object> createPmJob(Map<String, ?> request);

    /** @return the PM job, or {@code null} when it does not exist */
    Map<String, Object> getPmJob(String pmJobId);

    /** @param changes {@code callbackUri} and/or {@code authentication} @return the changes made */
    Map<String, Object> updatePmJob(String pmJobId, Map<String, ?> changes);

    ActionResponse deletePmJob(String pmJobId);

    /** @return the report, or {@code null} when it does not exist */
    Map<String, Object> getReport(String pmJobId, String reportId);

    Map<String, Object> listThresholds(Map<String, String> params);

    /** @param request {@code objectType}, {@code objectInstanceId}, {@code criteria}, {@code callbackUri}, optional {@code subObjectInstanceIds}, {@code authentication} */
    Map<String, Object> createThreshold(Map<String, ?> request);

    /** @return the threshold, or {@code null} when it does not exist */
    Map<String, Object> getThreshold(String thresholdId);

    /** @param changes {@code callbackUri} and/or {@code authentication} @return the changes made */
    Map<String, Object> updateThreshold(String thresholdId, Map<String, ?> changes);

    ActionResponse deleteThreshold(String thresholdId);
}
