package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.FirewallGroup;
import org.openstack4j.model.network.options.FirewallGroupOptions;

/** Firewall groups ({@code /v2.0/fwaas/firewall_groups}). */
public interface FirewallGroupService extends RestService {

    /** @return the firewall groups */
    List<? extends FirewallGroup> list();

    /** @param filters query parameters such as {@code name}, {@code status}, {@code project_id} */
    List<? extends FirewallGroup> list(Map<String, String> filters);

    /** @return the firewall group, or {@code null} when it does not exist */
    FirewallGroup get(String id);

    FirewallGroup create(FirewallGroupOptions options);

    /** Changes only the fields set in {@code options}. */
    FirewallGroup update(String id, FirewallGroupOptions options);

    ActionResponse delete(String id);
}
