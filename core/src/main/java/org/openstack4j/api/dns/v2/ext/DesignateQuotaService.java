package org.openstack4j.api.dns.v2.ext;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** DNS quotas of a project ({@code /v2/quotas/{project_id}}): {@code zones}, {@code zone_recordsets}, {@code zone_records}, {@code recordset_records}, {@code api_export_size}. */
public interface DesignateQuotaService extends RestService {

    /**
     * @return the project's quotas (an unknown project id gets the defaults). Another project's quotas need the
     *         {@code X-Auth-All-Projects: true} header, e.g. {@code os.headers(Map.of("X-Auth-All-Projects", "true"))};
     *         without it Designate answers 403
     */
    Map<String, Integer> get(String projectId);

    /** Changes the given quotas (admin) and returns all of them. */
    Map<String, Integer> update(String projectId, Map<String, Integer> quotas);

    /** Resets the project's quotas to the defaults (admin). */
    ActionResponse reset(String projectId);
}
