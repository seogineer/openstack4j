package org.openstack4j.api.identity.v3;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Mapping;

/**
 * OS-FEDERATION mappings ({@code /v3/OS-FEDERATION/mappings}).
 */
public interface MappingService extends RestService {

    /**
     * @return the result
     */
    List<? extends Mapping> list();

    /**
     * @param id the id
     * @return the result
     */
    Mapping get(String id);

    /**
     * Creates a mapping with its rules (PUT).
     *
     * @param id the id
     * @param List<Map<String the list< map< string
     * @param rules the rules
     * @return the result
     */
    Mapping create(String id, List<Map<String, Object>> rules);

    /**
     * Replaces the rules of a mapping (PATCH).
     *
     * @param id the id
     * @param List<Map<String the list< map< string
     * @param rules the rules
     * @return the result
     */
    Mapping update(String id, List<Map<String, Object>> rules);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
