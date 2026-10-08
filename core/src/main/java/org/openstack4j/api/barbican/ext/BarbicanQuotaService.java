package org.openstack4j.api.barbican.ext;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Barbican quotas: {@code secrets}, {@code orders}, {@code containers}, {@code consumers}, {@code cas} ({@code -1} = unlimited). */
public interface BarbicanQuotaService extends RestService {

    /** @return the quotas in effect for the caller's project ({@code GET /v1/quotas}) */
    Map<String, Integer> effective();

    /** @return the projects with configured quotas: project id to its quotas (admin) */
    Map<String, Map<String, Integer>> listProjectQuotas();

    /** @return the configured quotas of a project (admin); a project without them raises */
    Map<String, Integer> getProjectQuotas(String projectId);

    /** Sets quotas of a project (admin). */
    ActionResponse setProjectQuotas(String projectId, Map<String, Integer> quotas);

    /** Removes the configured quotas of a project, back to the defaults (admin). */
    ActionResponse deleteProjectQuotas(String projectId);
}
