package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.octavia.ext.OctaviaQuota;
import org.openstack4j.openstack.common.ListResult;

/** Octavia answers single quotas with {@code load_balancer}/{@code health_monitor}; the list carries both spellings. */
@JsonRootName("quota")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaQuotaEntity implements OctaviaQuota {

    private static final long serialVersionUID = 1L;

    @JsonProperty("project_id") private String projectId;
    @JsonProperty("load_balancer") @JsonAlias("loadbalancer") private Integer loadbalancer;
    @JsonProperty("listener") private Integer listener;
    @JsonProperty("member") private Integer member;
    @JsonProperty("pool") private Integer pool;
    @JsonProperty("health_monitor") @JsonAlias("healthmonitor") private Integer healthmonitor;
    @JsonProperty("l7policy") private Integer l7policy;
    @JsonProperty("l7rule") private Integer l7rule;

    @Override public String getProjectId() { return projectId; }
    @Override public Integer getLoadbalancer() { return loadbalancer; }
    @Override public Integer getListener() { return listener; }
    @Override public Integer getMember() { return member; }
    @Override public Integer getPool() { return pool; }
    @Override public Integer getHealthmonitor() { return healthmonitor; }
    @Override public Integer getL7policy() { return l7policy; }
    @Override public Integer getL7rule() { return l7rule; }

    public static class Quotas extends ListResult<OctaviaQuotaEntity> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("quotas")
        private List<OctaviaQuotaEntity> list;

        @Override
        protected List<OctaviaQuotaEntity> value() {
            return list;
        }
    }
}
