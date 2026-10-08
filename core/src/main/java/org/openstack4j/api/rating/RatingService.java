package org.openstack4j.api.rating;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Rating (CloudKitty v2): rated dataframes, rating modules, scope states, summaries and reprocessing tasks. Results are {@code Map}s. */
public interface RatingService extends RestService {

    /**
     * @param filters {@code begin}, {@code end}, {@code limit}, {@code offset}, {@code filters} (e.g. {@code project_id:p1})
     * @return {@code total} and {@code dataframes}
     */
    Map<String, Object> listDataframes(Map<String, String> filters);

    /** Adds rated dataframes (admin). */
    ActionResponse addDataframes(List<Map<String, Object>> dataframes);

    /** @return the rating modules ({@code module_id}, {@code enabled}, {@code priority}, {@code hot_config}, {@code description}) */
    List<Map<String, Object>> listModules();

    /** @return the module, or {@code null} when it does not exist */
    Map<String, Object> getModule(String moduleId);

    /** @param enabled or {@code null} @param priority or {@code null} */
    ActionResponse updateModule(String moduleId, Boolean enabled, Integer priority);

    /** @param filters {@code scope_id}, {@code scope_key}, {@code fetcher}, {@code collector}, {@code active}, {@code limit}, {@code offset} @return the scope states */
    List<Map<String, Object>> listScopes(Map<String, String> filters);

    /**
     * Resets the processing state of scopes (admin).
     *
     * @param body {@code all_scopes: true} or {@code scope_id}, plus optional {@code scope_key}, {@code fetcher},
     *             {@code collector}, {@code last_processed_timestamp}
     */
    ActionResponse resetScopes(Map<String, ?> body);

    /** Changes a scope (admin), e.g. {@code {"scope_id": …, "active": false}}; @return the scope */
    Map<String, Object> patchScope(Map<String, ?> body);

    /** Creates a scope (admin), e.g. {@code {"scope_id": …, "scope_key": "project_id", "active": true}}; @return the scope */
    Map<String, Object> createScope(Map<String, ?> body);

    /** @param filters {@code begin}, {@code end}, {@code groupby}, {@code filters}, {@code limit}, {@code offset}, {@code response_format} @return {@code total}, {@code columns}/{@code results} */
    Map<String, Object> summary(Map<String, String> filters);

    /**
     * Schedules reprocessing of scopes (admin).
     *
     * @param scopeIds the scopes, or {@code ["ALL"]}
     * @param start    e.g. {@code 2021-06-01 00:00:00+00:00}
     * @param end      e.g. {@code 2021-06-01 23:00:00+00:00}
     */
    ActionResponse reprocess(List<String> scopeIds, String start, String end, String reason);

    /** @param filters {@code scope_ids}, {@code order}, {@code limit}, {@code offset} @return the reprocessing tasks */
    List<Map<String, Object>> listReprocesses(Map<String, String> filters);

    /** @return the reprocessing tasks of one scope */
    List<Map<String, Object>> getReprocesses(String scopeId);
}
