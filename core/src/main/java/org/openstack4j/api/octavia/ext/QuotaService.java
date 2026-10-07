package org.openstack4j.api.octavia.ext;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.OctaviaQuota;
import org.openstack4j.model.octavia.options.OctaviaQuotaOptions;

/**
 * Octavia quotas ({@code /v2/lbaas/quotas}); null means the default applies, -1 means unlimited.
 */
public interface QuotaService extends RestService {

    /**
     * Lists the projects with non-default quotas (admin).
     *
     * @return the result
     */
    List<? extends OctaviaQuota> list();

    /**
     * Returns the default quotas.
     *
     * @return the result
     */
    OctaviaQuota defaults();

    /**
     * Returns the quotas of a project; null fields use the defaults.
     *
     * @param projectId the project id
     * @return the result
     */
    OctaviaQuota get(String projectId);

    /**
     * Updates quotas of a project; only the fields set are sent.
     *
     * @param projectId the project id
     * @param options the options
     * @return the result
     */
    OctaviaQuota update(String projectId, OctaviaQuotaOptions options);

    /**
     * Resets the quotas of a project to the defaults.
     *
     * @param projectId the project id
     * @return the action response
     */
    ActionResponse reset(String projectId);
}
