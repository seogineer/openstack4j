package org.openstack4j.openstack.barbican.internal.ext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.barbican.ext.OrderService;
import org.openstack4j.model.barbican.ext.Order;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.barbican.domain.ext.BarbicanOrder;
import org.openstack4j.openstack.barbican.domain.ext.BarbicanOrder.BarbicanOrderList;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class OrderServiceImpl extends BaseBarbicanExtService implements OrderService {

    @Override
    public List<? extends Order> list() {
        return list(null);
    }

    @Override
    public List<? extends Order> list(Map<String, String> filters) {
        BarbicanOrderList orders = get(BarbicanOrderList.class, "/orders").params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        return orders == null ? Collections.emptyList() : orders.getList();
    }

    @Override
    public Order get(String orderId) {
        return get(BarbicanOrder.class, "/orders/" + id(orderId)).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public String create(String type, Map<String, ?> meta) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", Objects.requireNonNull(type, "type"));
        body.put("meta", Objects.requireNonNull(meta, "meta"));
        Map<String, Object> created = post(Map.class, "/orders").entity(JsonBody.of(body)).execute(propagate404());
        return created == null ? null : (String) created.get("order_ref");
    }

    @Override
    public ActionResponse delete(String orderId) {
        return deleteWithResponse("/orders/" + id(orderId)).execute();
    }
}
