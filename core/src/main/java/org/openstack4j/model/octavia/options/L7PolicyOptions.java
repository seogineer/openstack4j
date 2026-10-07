package org.openstack4j.model.octavia.options;

import java.util.List;
import java.util.Objects;

/** Body of an L7 policy create or update. */
public class L7PolicyOptions extends OctaviaAttributes<L7PolicyOptions> {

    public static L7PolicyOptions create(String listenerId, String action) {
        return new L7PolicyOptions().put("listener_id", Objects.requireNonNull(listenerId)).put("action", Objects.requireNonNull(action));
    }

    /** An update that sends only the fields set afterwards. */
    public static L7PolicyOptions update() {
        return new L7PolicyOptions();
    }

    @Override
    protected L7PolicyOptions self() {
        return this;
    }

    public L7PolicyOptions name(String value) { return put("name", value); }
    public L7PolicyOptions description(String value) { return put("description", value); }
    public L7PolicyOptions position(Integer value) { return put("position", value); }
    public L7PolicyOptions redirectPoolId(String value) { return put("redirect_pool_id", value); }
    public L7PolicyOptions redirectUrl(String value) { return put("redirect_url", value); }
    public L7PolicyOptions redirectPrefix(String value) { return put("redirect_prefix", value); }
    public L7PolicyOptions redirectHttpCode(Integer value) { return put("redirect_http_code", value); }
    public L7PolicyOptions adminStateUp(Boolean value) { return put("admin_state_up", value); }
    public L7PolicyOptions tags(List<String> value) { return put("tags", value); }
}
