package org.openstack4j.model.octavia.options;

import java.util.Objects;

/** Body of an Octavia quota update; only the quotas set are sent (-1 = unlimited). */
public class OctaviaQuotaOptions extends OctaviaAttributes<OctaviaQuotaOptions> {

    public static OctaviaQuotaOptions create() {
        return new OctaviaQuotaOptions();
    }

    @Override
    protected OctaviaQuotaOptions self() {
        return this;
    }

    public OctaviaQuotaOptions loadbalancer(Integer value) { return put("loadbalancer", value); }
    public OctaviaQuotaOptions listener(Integer value) { return put("listener", value); }
    public OctaviaQuotaOptions member(Integer value) { return put("member", value); }
    public OctaviaQuotaOptions pool(Integer value) { return put("pool", value); }
    public OctaviaQuotaOptions healthmonitor(Integer value) { return put("healthmonitor", value); }
    public OctaviaQuotaOptions l7policy(Integer value) { return put("l7policy", value); }
    public OctaviaQuotaOptions l7rule(Integer value) { return put("l7rule", value); }
}
