package org.openstack4j.api.telemetry.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Aodh alarm history, state, complex queries and quotas ({@code /v2/...}). */
public interface AodhAlarmExtService extends RestService {

    /** @param filters query parameters such as {@code q.field}/{@code q.op}/{@code q.value}, {@code sort}, {@code limit} @return the alarm's change history */
    List<Map<String, Object>> history(String alarmId, Map<String, String> filters);

    /** @return {@code ok}, {@code alarm} or {@code insufficient data} */
    String getState(String alarmId);

    /** @return the new state */
    String setState(String alarmId, String state);

    /**
     * Complex query of alarms ({@code POST /v2/query/alarms}).
     *
     * @param filter  a JSON filter expression, e.g. {@code {"=": {"state": "alarm"}}}, or {@code null}
     * @param orderby a JSON list, e.g. {@code [{"name": "asc"}]}, or {@code null}
     * @param limit   or {@code null}
     */
    List<Map<String, Object>> queryAlarms(String filter, String orderby, Integer limit);

    /** Complex query of alarm history ({@code POST /v2/query/alarms/history}). */
    List<Map<String, Object>> queryHistory(String filter, String orderby, Integer limit);

    /** @return the project's alarm quotas: resource to limit */
    Map<String, Integer> getQuotas(String projectId);

    /** Sets quotas of a project (admin) and returns them. */
    Map<String, Integer> setQuotas(String projectId, Map<String, Integer> quotas);

    /** Removes the project's quotas, back to the defaults (admin). */
    ActionResponse deleteQuotas(String projectId);
}
