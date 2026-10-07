package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.dns.v2.ext.Pool;

/** Pools ({@code /v2/pools}). */
public interface DesignatePoolService extends RestService {

    /** @return the pools */
    List<? extends Pool> list();

    /** @param filters query parameters such as {@code limit}, {@code marker} */
    List<? extends Pool> list(Map<String, String> filters);

    /** @return the pool, or {@code null} when it does not exist */
    Pool get(String id);
}
