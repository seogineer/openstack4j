package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.AvailabilityZone;
import org.openstack4j.model.manila.Service;

/**
 * The 2.7+ paths of availability zones, services, quotas and share type access (the older {@code os-*} methods of
 * {@code os.share()} keep working at 2.6).
 */
public interface ShareAdministrationService extends RestService {

    /** @return the availability zones ({@code GET /availability-zones}, 2.7) */
    List<? extends AvailabilityZone> availabilityZones();

    /** @param filters query parameters such as {@code host}, {@code binary}, {@code zone}, {@code state}, {@code status}, {@code ensuring} (2.93) */
    List<? extends Service> services(Map<String, String> filters);

    /** @return the service's {@code host}, {@code binary} and {@code disabled} (2.7) */
    Map<String, Object> enableService(String host, String binary);

    /** @param reason may be {@code null}; a reason needs microversion 2.83 */
    Map<String, Object> disableService(String host, String binary, String reason);

    /** Runs the ensure-shares operation of a share service host (2.86, admin). */
    ActionResponse ensureShares(String host);

    /** @return the project's quotas ({@code quota_set}, 2.7); a missing project raises */
    Map<String, Object> quotaSet(String projectId);

    /** @return per quota its {@code in_use}, {@code limit} and {@code reserved} (2.25) */
    Map<String, Object> quotaSetDetail(String projectId);

    /** @return the default quotas (2.7) */
    Map<String, Object> quotaSetDefaults(String projectId);

    /** Changes the given quotas (admin, 2.7) and returns all of them. */
    Map<String, Object> updateQuotaSet(String projectId, Map<String, ?> quotas);

    /** Resets the project's quotas to the defaults (admin, 2.7). */
    ActionResponse deleteQuotaSet(String projectId);

    /** @return the quotas of a quota class, e.g. {@code default} (2.7) */
    Map<String, Object> quotaClassSet(String quotaClassName);

    /** Changes the quotas of a quota class (admin, 2.7) and returns them. */
    Map<String, Object> updateQuotaClassSet(String quotaClassName, Map<String, ?> quotas);

    /** @return the projects that can use a private share type ({@code share_type_id}, {@code project_id}; 2.7) */
    List<Map<String, Object>> shareTypeAccess(String shareTypeId);
}
