package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.TsigKey;
import org.openstack4j.model.dns.v2.options.TsigKeyCreate;
import org.openstack4j.model.dns.v2.options.TsigKeyUpdate;

/** TSIG keys ({@code /v2/tsigkeys}). */
public interface DesignateTsigKeyService extends RestService {

    /** @return the TSIG keys */
    List<? extends TsigKey> list();

    /** @param filters query parameters such as {@code name}, {@code algorithm}, {@code scope}, {@code limit}, {@code marker} */
    List<? extends TsigKey> list(Map<String, String> filters);

    /** @return the TSIG key, or {@code null} when it does not exist */
    TsigKey get(String id);

    TsigKey create(TsigKeyCreate create);

    TsigKey update(String id, TsigKeyUpdate update);

    ActionResponse delete(String id);
}
