package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.Blacklist;
import org.openstack4j.model.dns.v2.options.BlacklistCreate;
import org.openstack4j.model.dns.v2.options.BlacklistUpdate;

/** Blacklists ({@code /v2/blacklists}). */
public interface DesignateBlacklistService extends RestService {

    /** @return the blacklists */
    List<? extends Blacklist> list();

    /** @param filters query parameters such as {@code pattern}, {@code description}, {@code limit}, {@code marker} */
    List<? extends Blacklist> list(Map<String, String> filters);

    /** @return the blacklist, or {@code null} when it does not exist */
    Blacklist get(String id);

    Blacklist create(BlacklistCreate create);

    Blacklist update(String id, BlacklistUpdate update);

    ActionResponse delete(String id);
}
