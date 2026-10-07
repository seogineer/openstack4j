package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.ZoneShare;

/** Shares of a zone with other projects ({@code /v2/zones/{zone_id}/shares}). */
public interface ZoneShareService extends RestService {

    /** @return the shares of a zone; a missing zone raises */
    List<? extends ZoneShare> list(String zoneId);

    /** @param filters query parameters such as {@code target_project_id}, {@code limit}, {@code marker} */
    List<? extends ZoneShare> list(String zoneId, Map<String, String> filters);

    /** @return the share, or {@code null} when it does not exist */
    ZoneShare get(String zoneId, String shareId);

    /** Shares the zone with {@code targetProjectId}, which can then create recordsets in it. */
    ZoneShare create(String zoneId, String targetProjectId);

    ActionResponse delete(String zoneId, String shareId);
}
