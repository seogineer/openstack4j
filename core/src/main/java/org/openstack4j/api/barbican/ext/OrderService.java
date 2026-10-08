package org.openstack4j.api.barbican.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.barbican.ext.Order;
import org.openstack4j.model.common.ActionResponse;

/** Barbican orders ({@code /v1/orders}). */
public interface OrderService extends RestService {

    /** @return the orders */
    List<? extends Order> list();

    /** @param filters query parameters such as {@code limit}, {@code offset} */
    List<? extends Order> list(Map<String, String> filters);

    /** @return the order, or {@code null} when it does not exist */
    Order get(String orderId);

    /**
     * Starts generating a secret.
     *
     * @param type {@code key}, {@code asymmetric} or {@code certificate}
     * @param meta e.g. {@code name}, {@code algorithm}, {@code bit_length}, {@code mode}, {@code payload_content_type}
     * @return the reference (URL) of the new order; poll {@link #get(String)} until it is {@code ACTIVE}
     */
    String create(String type, Map<String, ?> meta);

    ActionResponse delete(String orderId);
}
