package org.openstack4j.api.dns.v2;

import java.util.Map;
import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.Nameserver;
import org.openstack4j.model.dns.v2.Zone;

/**
 * Designate V2 Zone Service
 */
public interface ZoneService extends RestService {

    /**
     * create a new zone
     *
     * @param zone the zone
     * @return the newly created zone
     */
    Zone create(Zone zone);

    /**
     * creates a new zone
     *
     * @param name the zone name
     * @param email the e-mail for the zone
     * @return the newly created group
     */
    Zone create(String name, String email);

    /**
     * gets detailed information about a specified zone by id
     *
     * @param zoneId the zone identifier
     * @return the zone
     */
    Zone get(String zoneId);

    /**
     * updates an existing zone
     *
     * @param zone the zone set to update
     * @return the updated zone
     */
    Zone update(Zone zone);

    /**
     * delete a zone by id
     *
     * @param zoneId the zone id
     * @return the action response
     */
    ActionResponse delete(String zoneId);

    /**
     * list nameservers for a zone
     *
     * @param zoneId the zone identifier
     * @return list of nameservers for a zone
     */
    List<? extends Nameserver> listNameservers(String zoneId);

    /**
     * lists zones.
     *
     * @return list of zones
     */
    List<? extends Zone> list();

    /**
     * @param filters query parameters such as {@code name}, {@code email}, {@code status}, {@code type}, {@code ttl},
     *                {@code description}, {@code limit}, {@code marker}, {@code sort_key}
     * @return the matching zones
     */
    List<? extends Zone> list(Map<String, String> filters);

    /** Abandons a zone: Designate forgets it but leaves it on the name servers (admin). */
    ActionResponse abandon(String zoneId);

    /** Asks a secondary zone to transfer the zone from its masters now ({@code tasks/xfr}). */
    ActionResponse transferFromMaster(String zoneId);

    /** Moves a zone to another pool (admin); {@code poolId} {@code null} lets the scheduler choose. */
    ActionResponse movePool(String zoneId, String poolId);
}
