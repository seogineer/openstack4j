package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.Tld;
import org.openstack4j.model.dns.v2.options.TldCreate;
import org.openstack4j.model.dns.v2.options.TldUpdate;

/** TLDs ({@code /v2/tlds}). */
public interface DesignateTldService extends RestService {

    /** @return the TLDs */
    List<? extends Tld> list();

    /** @param filters query parameters such as {@code name}, {@code description}, {@code limit}, {@code marker} */
    List<? extends Tld> list(Map<String, String> filters);

    /** @return the TLD, or {@code null} when it does not exist */
    Tld get(String id);

    Tld create(TldCreate create);

    Tld update(String id, TldUpdate update);

    ActionResponse delete(String id);
}
