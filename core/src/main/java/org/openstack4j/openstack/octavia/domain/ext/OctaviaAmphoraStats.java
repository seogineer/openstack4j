package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.octavia.ext.AmphoraStats;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaAmphoraStats implements AmphoraStats {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("listener_id") private String listenerId;
    @JsonProperty("loadbalancer_id") private String loadbalancerId;
    @JsonProperty("active_connections") private Long activeConnections;
    @JsonProperty("bytes_in") private Long bytesIn;
    @JsonProperty("bytes_out") private Long bytesOut;
    @JsonProperty("request_errors") private Long requestErrors;
    @JsonProperty("total_connections") private Long totalConnections;

    @Override public String getId() { return id; }
    @Override public String getListenerId() { return listenerId; }
    @Override public String getLoadbalancerId() { return loadbalancerId; }
    @Override public Long getActiveConnections() { return activeConnections; }
    @Override public Long getBytesIn() { return bytesIn; }
    @Override public Long getBytesOut() { return bytesOut; }
    @Override public Long getRequestErrors() { return requestErrors; }
    @Override public Long getTotalConnections() { return totalConnections; }

    public static class Stats extends ListResult<OctaviaAmphoraStats> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("amphora_stats")
        private List<OctaviaAmphoraStats> list;

        @Override
        protected List<OctaviaAmphoraStats> value() {
            return list;
        }
    }
}
